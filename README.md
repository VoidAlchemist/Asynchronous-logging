# Asynchronous-logging
Tried to create a light version of log4j (without LMAX Disruptor of course)

It's been several months since I started, and I got to say this was not logging at all from the start. It's more like me trying to fix some issues on a lock-free ring buffer after abandoning it for 3 months.

```test``` package only contains an attempt to optimize memory barriers for volatile integer. Sadly, in the end, it pretty much looks like a rip-off of LMAX Disruptor's ```Sequence```.

See ```BlockingTorus``` if you seek performances. It uses a simple spinlock to lock critical sections.

The following are a few notes about my fun little journey in the wait-free world, though it's practically become a blog.

## Lock-free and wait-free data structure

The first version of this logger works thanks to the ```SpinLock``` class. While "asynchronous" doesn't mean "lock-free", I surely don't want to use locks anymore for logging. I tried once, but I must have missed something back then because one log instruction was taking in average 80 ms with 10 threads alive. At that point, I almost thought "why not just do a synchronous logger? like just play with nio in the caller thread".
First of all, we ought to understand what are lock-free and wait-free algorithms.

Definitions:
- lock-free = at least one thread is making progress (or without mutexes, definition can vary). To simplify, we'll say it's without mutexes for avoiding deadlocks. Typically, a SpinLock is considered lock-free.
- wait-free = all threads are making progress. Now, that implies we can't just sit in a compare and swap loop doing nothing. In that sense, I always ask myself if fetch and add is really wait-free.

Abbreviations:
- FAA = fetch and add
- CAS = compare and swap
- CAE = compare and exchange

Logic behind ring buffer is as follow:

```
void add(T o)
    head %= max;  // broken with several producers
    T old = data[head];
    data[head] = o;
    
    if (old != null)  // overwrite case
        tail++; // broken with different producer-consumer
    
    head++; // broken with several producers

T poll()
    tail %= max;  // broken with several consumers
    T result = data[tail];
    data[tail] = null;  // super broken with different consumer-producer
    tail++; // broken with several consumers
    return result;
```
The concurrent variant is more complicated:
```
void add(T o)
    int h = head.FAA();  // atomic version of LOAD head and then later LAOD head STORE head+1
    int h1 = h % max;
    
    if (h != h1)
        head.CAS(h, h1); // multiple producers may help each other on this
        // low priority. allows to not limit the number of transactions with the buffer to Integer.MAX_VALUE
    
    T old = data[h1];
    data[h1] = o;  // here, we completely ignore when more than N threads add requests are executed 
    //"at the same time", N >= max. If data overwrite or race condition between threads     
    // that called add "at the same time" are not desired,
    // then another concurrent queue needs to be used specifically for overflow case.
    
    if (old != null)
        tail.FAA(); // move tail along.
        
T poll()
    int t = tail.FAA();    // you got the gist of it now
    int t1 = t % max;
    
    if (t != t1)
        tail.CAS(t, t1); // multiple consumers may help each other on this
        // it's low priority and allows to not limit the number of poll() calls to Integer.MAX_VALUE
    
    T result = data[t];
    data[t] = null;
    return result;
```
As you can see, the concurrent version is a bit different, but thanks to the FAA atomic operation, things are quite clean. However, we completely skip the full buffer situation. While it is indeed much easier to handle that way, our data structure is in a way not usable at all. But eh, bit by bit, would you? Making a circular buffer wait-free is though enough as it is right now. Though, I'm not that sure FAA is actually wait-free as it's usually just a CAS loop with n and n+1.

Anyway, in this concurrent version, we still have one major problem beside the overflow situation. Indeed, producers write to the array data to add their object. That alone is fine as we distribute a unique index to each producer (ignore overflow please). However, while consumers should ideally only be reading, they nonetheless try to write into the array the null pointer to signal they're done consuming an object. We absolutely need that mechanism, primordial for knowing whether the buffer has room, and the tail position. But then, that means we ought to make the entire array atomic, which will surely make our performances crumble, if it wasn't done before. For the sake of the algorithm correctness, I still wrote something like this algorithm in ```...```, nowhere actually. With these remarks in mind, note that ```AsyncTorus``` and ```AsyncDonut``` are not correct. Left them here, as they show different approaches to the problem but their algorithms are far from correct.

As a result, our current concurrent version should be linearizable, but has a lot of flaws, which is bad as we want correctness over speed (but speed still matters).
But hold on! While this ring buffer thing is good for simple objects, if we want to pass, say 2-3 primitives or string, are we going to create a wrapper object, like a record every time we add something to the queue and then destroy it?? Doesn't it sound like a lot of unnecessary object creation? To me, re-creating wrappers and destroying them right after always calls for a pool. I mean, immutability is perfect for concurrency, but I want a correct **and** realistic java-ish algorithm.
To be honest, until someone reminded me that objects shouldn't stay in the buffer after they got consumed, I instinctively kept objects in the buffer, to prevent producers **and** consumers both writing to the array, out of laziness. So, going back to our logging stuff, we want all the time formatting and string concatenation stuff to be done by the logging daemon (the consumer) if possible. But then we're no longer passing only one string, but several strings plus a long plus a throwable (see ```con.xenon.logging.LogEvent```). So, from that point on, it looks like we should more go for a pool than an actual circular buffer. With that said, getting a pool lock-free is quite difficult as we need to secure object distribution (pretty much by indexing with fetch&Add like we did before, ignoring overflow), but also some of the object's fields. Wow that looks painful, for something half-baked that even completely ignores overflow.

*Side note: Keep in mind that when I say "ignoring overflow", I don't mean "working with arrays of infinite length". That's what you will find in the littérature; but that's not realistic. Moreover, I do move tail along when head starts to overwrite, so what's of concern really is the "overflow", a.k.a. "what to do when producers produce too fast and the buffer is full." (as you should've guessed, we won't just block producers until there's room).*

For an asynchronous pool, one must have a head cursor to distribute objects to producers (which won't actually produce objects this time, but rather set some fields) as well as a tail cursor to know from where to start consuming (which here will only read some fields). For our pool to work properly, we need one more thing, that is to know whether an object is dead or alive (whether it's ready to be edited by a producer or it needs to be freed by a consumer).
To sum it up, we need 2 atomic cursors head and tail, as well as a volatile boolean -let's call it "dead"- for each object in the pool. Volatile keyword should be enough here as we make the assumption that different producers work with different objects, same for consumers (ignore overflow).

The final lock-free pool< T extends wrap_something> implementation should look like this:

```
void update(var... new_field_values)
    int h = head.FAA() % max;
    
    
    T current = data[h];
    
    if (!current.dead)  // overwrite case
        tail.FAA();
    
    current.setFields(new_fields_values);
    current.dead = false;   // volatile set. btw, also makes setFields(var...) a volatile operation. google "happens before volatile"
    

void peek(Consumer<wrap_something> consumer)
    int h = head.get();
    int h1 = h % max;
    if (h1 == tail.get() % max)
        return;
        
    int t = tail.FAA();
    int t1 = t % max;
    
    if (h != h1)
        head.CAS(h, h1);
    
    if (t != t1)
        tail.CAS(t, t1);
        
    if (t1 == h1)
        return;
    
    T current = data[t1];
    current.dead = true;
    consumer.accept(current);   // do something with fields.
    // sadly, we cannot convert the current.dead = true; and this line in one atomic operation,
    // so this is still not correct...
    
```

Wait, there is a huge problem with this implementation: it goes abnormally well! Indeed, unless you are researcher that works with wait-free stuff all day long, when you think your wait-free algorithm is correct, then it's not yet.

Here, the deal is easy: our `update()` and `peek()` methods are not linearizable. Though I don't quite know myself what it means, here it's because we need at least 2 instructions to peek/update an object, `obj.dead = boolean` and `obj.setFields(newFields)`/`doStuffWithFields(obj)`.
A simple timestamp example:

**Assumptions and definitions:**

Thread P is a producer.
Thread C is a consumer.
The ring buffer is full and P just starts overwritting. C tries to help.

1. C claims the index of the oldest object, say at index N.
2. P claims that very N index as it wants to write more. As the N object isn't dead, tail is incremented atomically, but too bad, C already got its ticket for N.
3. C starts consuming. First calls `current.dead = true`.
2. P starts producing. First calls `current.setFields(new_fields_values)`.
3. C calls `consumer.accept(current)`.
4. P calls `current.dead = false`.

Now you ask, what's the effect extacly? Our algorithm isn't that stupid either so problems "only" arise when these 4 lines get interleaved.
In this timestamp, C arrived a inch too late and the old data got overwrote. C did consume the freshly-set fields, yet `current.dead` is `false`.


*Performance note: Padding could be added to ```con.xenon.logging.LogEvent``` around its fields to minimize false sharing occurrences.*

*Final note: In general, keep in mind that lock-free, or even wait-free doesn't mean fast nor efficient as CAS and FAA have a heavy cost, like every volatile-related stuff. This should be quite visible with the pool structure producer-side-wise because a spin-lock implementation only requires a CAE loop and a volatile set to lock critical sections, whereas a lock-free implementation must do 1 FAA for head, a CAS on head for modulo max (though it can be done by some consumer), a volatile set on "dead", then a volatile read on "dead" of the next object to know if it should move tail along, and if so performs FAA on tail.*


## Logging

Here goes logging, finally. Not that there's much to say, though.
One big thing that took me quite the time was making the logging thread a daemon. Indeed, in my early attempts to write an asynchronous logger, I quickly bumped into the need to make my thread automatically shuts down. Back in the day, I had to manually shut down IntelliJ each time since the logging thread was still there alive. I didn't immediatly think of daemons because for me it was restricted to HTTP requests and packets stuff, not such things as writing into file. So what's the solution then? The API's user's got to type "LogManager.shut()" at the end of his program?? That's way too dangerous.
The only way to make a thread automatically shuts down when other "user threads" are dead is to make it a daemon. In Java, when all user threads are stopped, the JVM stops along with all the daemons. The problem with daemons is that they are interrupted by the JVM itself, ignoring finally blocks or any security net. So, on forums, you will see tons of people crying over not using daemons for IO and to use user threads instead. But then what? it's not like adding ```Runtime.getRuntime().addShutDownHook()``` will help since it's being called once all user threads stop.
In the end, I made my thread a daemon. With great power comes great responsibility, I had to make the IO close quietly.
Two major problems come when a daemon is shut down abruptly: 
- no IO resource will get correctly closed.
- any held lock won't get released.
These two facts seem natural because the JVM is terminating, so no trade-off.
But still, I don't want my thread to keep alive the JVM but I want it to close nicely.
The solution is actually quite easy. I just had a shutdown hook to the runtime, thing that is actually useful when working with daemons (with user threads we go nowhere).
```
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    try{
        Thread.sleep(100);
        bufferedWriter.close();
    } catch(Exception ignored) {}
})};
```
In ```LogManager```, I am actually keeping open a BufferedWriter open all the time, instead of try-with-resources it each time I log, and I close safely after the daemons are shutdown. However, ```java.io.BufferedWriter``` methods are synchronized. That means that if my daemon is abruptly killed, I won't every be able to enter a critical section and close the buffer. So I rewrote ```BufferedWriter``` without locks, which is actually perfect for performances as well, since only the logging thread will be accessing it. See ```com.xenon.utils.UnsafeBufferedWriter```.
