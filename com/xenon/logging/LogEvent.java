package com.xenon.logging;

import java.time.LocalTime;

public class LogEvent {

    public long time;
    public String threadName, msg;
    public Throwable throwable;
    public LogManager.Level lvl;

    /**
     * Constructs a LogEvent object.
     * Producer side.
     * @param time the time the log was emitted
     * @param threadName the name of the producer thread
     * @param msg the log message
     * @param throwable the throwable associated with the event
     */
    public void construct(long time, String threadName, String msg, Throwable throwable, LogManager.Level lvl){
        this.time = time;
        this.threadName = threadName;
        this.msg = msg;
        this.throwable = throwable;
        this.lvl = lvl;
    }

    /**
     *
     * @return the formatted message of the log
     */
    public String getText(){
        var b = new StringBuilder("<")
                .append("> [")
                .append(threadName)
                .append('/')
                .append(lvl)
                .append("] : ")
                .append(msg);
        if (throwable != null)
            b.append(throwable.getMessage());

        return b.toString();
    }

}
