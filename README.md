# Asynchronous-logging
Tried to create a light version of log4j (without LMAX Disruptor of course)

It's been several months since I started, and I got to say this was not logging at all from the start. It's more like me trying to fix some issues on a lock-free ring buffer after abandoning it for 3 months.

```test``` package only contains an attempt to optimize memory barriers for volatile integer. Sadly, in the end, it pretty much looks like a rip-off of LMAX Disruptor's ```Sequence```.

See ```BlockingTorus``` if you seek performances. It uses a simple spinlock to lock critical sections.
See ```AsyncTorus``` if you seek every more performances. With the usual fetch&Add logic, it allows lock-free interactions between producers and the data structure. The only drawback with the fetch&Add is that some race condition may occur when N threads attempt to write at the same time, N being more than the ring buffer's capacity. An older write may void a younger one, though in this case, we can barely say which one is older. But anyway, the buffer capacity should be set according to the number of threads that will log.

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
The concurrent variant is way more complicated:
```
void add(T o)
    uint h = head.fetchAndAdd() % max;  // atomic version of LOAD head and then after LOAD head STORE head+1
    T old = data[h];
    data[h] = o;  // here, we completely ignore when more than N add requests are executed "at the same time", N >= max. If data overwrite or race condition between threads     // that called add "at the same time" are not desired, then another concurrent queue needs to be used specifically for overflow case.
    
    if (old != null)
        tail.fetchAndAdd(); // move tail along.
        
T poll()
    uint t = tail.fetchAndAdd() % max;  // u got the gist of it now
    T result = data[t];
    data[t] = null;
    return result;
```
As you can see, the concurrent version is a bit different, but thanks to the fetch&Add atomic operation, things are quite clean. However, we completely skip the full buffer situation. While it is indeed much easier to handle that way, our data structure is in a way not usable at all. But eh, bit by bit, would you? Making a circular buffer wait-free is enough hard as it is right now. Though, I'm not that sure fetchAndAdd is actually wait-free as it's usually just a CompareAndSwap loop with n and n+1.

Anyway, in this concurrent version, we still have one major problem beside the overflow situation. It is that producers write to the array data to add their object. That alone is fine as we distribute a unique index to each producer (ignore overflow please). However, while consumers should ideally only be reading, they nonetheless try to write into the array the null pointer to signal they're done consuming an object. We absolutely need that mechanism, primordial for knowing whether the buffer has room, and the tail position. But then, that means we ought to make the entire array atomic, which will surely make our performances crumble, if it wasn't done before. For the sake of the algorithm correctness, I still wrote something like this algorithm in ```...```, nowhere actually. With these remarks in mind, note that ```AsyncTorus``` and ```AsyncDonut``` are not correct. Left them here, as they show different approaches to the problem but they are both far from correct.
