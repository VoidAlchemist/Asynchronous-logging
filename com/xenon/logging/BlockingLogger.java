package com.xenon.logging;

/**
 * Utility class to pass log messages to {@link LogManager}.
 * @author Zenon
 */
@SuppressWarnings("unused")
public class BlockingLogger {


    /**
     * Writes the message to DEBUG level into logs file
     * @param text the desired message to be logged
     */
    public void debug(String text) {
        this.log(text, LogManager.Level.DEBUG);
    }
    /**
     * Writes the message to DEBUG level into logs file
     * @param text the desired message to be logged
     * @param t the throwable associated with the log event
     */
    public void debug(String text, Throwable t) {
        this.log(text + t.getMessage(), LogManager.Level.DEBUG);
    }
    /**
     * Writes the message to INFO level into logs file
     * @param text the desired message to be logged
     */
    public void info(String text) {
        this.log(text, LogManager.Level.INFO);
    }
    /**
     * Writes the message to INFO level into logs file
     * @param text the desired message to be logged
     * @param t the throwable associated with the log event
     */
    public void info(String text, Throwable t) {
        this.log(text + t.getMessage(), LogManager.Level.INFO);
    }
    /**
     * Writes the message to WARN level into logs file
     * @param text the desired message to be logged
     */
    public void warn(String text) {
        this.log(text, LogManager.Level.WARN);
    }
    /**
     * Writes the message to WARN level into logs file
     * @param text the desired message to be logged
     * @param t the throwable associated with the log event
     */
    public void warn(String text, Throwable t) {
        this.log(text + t.getMessage(), LogManager.Level.WARN);
    }
    /**
     * Writes the message to ERROR level into logs file
     * @param text the desired message to be logged
     */
    public void error(String text) {
        this.log(text, LogManager.Level.ERROR);
    }
    /**
     * Writes the message to ERROR level into logs file
     * @param text the desired message to be logged
     * @param t the throwable associated with the log event
     */
    public void error(String text, Throwable t) {
        this.log(text+t.getMessage(), LogManager.Level.ERROR);
    }
    /**
     * Writes the message to FATAL level into logs file
     * @param text the desired message to be logged
     */
    public void fatal(String text) {
        this.log(text, LogManager.Level.FATAL);
    }
    /**
     * Writes the message to FATAL level into logs file
     * @param text the desired message to be logged
     * @param t the throwable associated with the log event
     */
    public void fatal(String text, Throwable t) {
        this.log(text+t.getMessage(), LogManager.Level.FATAL);
    }
    /**
     * Logs the given message to file with the corresponding level of severity.
     * @param text the desired message to be logged
     * @param level the severity level
     */
    public void log(String text, LogManager.Level level) {
        LogManager.queueLog(text, level);
    }
}
