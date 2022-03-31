# Asynchronous-logging
Tried to create a light version of log4j (without LMAX Disruptor of course)

It's been several months since I started, and I got to say this was not logging at all from the start. It's more like me trying to fix some issues on a lock-free ring buffer after abandoning it for 3 months.

```test``` package only contains an attempt to optimize memory barriers for volatile integer. Sadly, in the end, it pretty much looks like a rip-off of LMAX Disruptor's ```Sequence```.

See ```BlockingTorus``` if you seek performances. It uses a simple spinlock to lock critical sections.
See ```AsyncTorus``` if you seek every more performances. With the usual fetch&Add logic, it allows lock-free interactions between producers and the data structure. The only drawback with the fetch&Add is that some race condition may occur when N threads attempt to write at the same time, N being more than the ring buffer's capacity. An older write may void a younger one, though in this case, we can barely say which one is older. But anyway, the buffer capacity should be set according to the number of threads that will log.
