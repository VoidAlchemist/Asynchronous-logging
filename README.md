# Asynchronous-logging
Tried to create a light version of log4j (without LMAX Disruptor of course)

It's been several months since I started, and I got to say this was not logging at all from the start. It's more like me trying to fix some issues on a lock-free ring buffer after abandoning it for 3 months.

```test``` package only contains an attempt to optimize memory barriers for volatile integer. Sadly, in the end, it pretty much looks like a rip-off of LMAX Disruptor's ```Sequence```.

See ```BlockingTorus``` if you seek performances. It uses a simple spinlock to lock critical sections. A non-blocking version is planned by mixing it with the usual fetch&Add logic for head and tail indexing, though we'll lose some of the safety we currently have especially consumer-producer race conditions. 
