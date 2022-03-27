# Asynchonous-logging
Tried to create a light version of log4j (without LMAX Disruptor of course)

It's been several months since I started, and I got to say this is not logging at all. It's more me trying to fix some issues on a lock-free ring buffer after abandoning it for 3 months.

```test``` package only contains an attempt to optimize memory barriers for volatile integer. Sadly, in the end, it pretty much looks like a rip-off of LMAX Disruptor's ```Sequence```.
