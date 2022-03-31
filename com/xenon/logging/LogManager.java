package com.xenon.logging;

import com.xenon.collections.BlockingTorus;
import com.xenon.utils.FilesUtils;
import com.xenon.utils.UnsafeBufferedWriter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Main logging class, though the user will interact with {@link BlockingLogger}.
 * @author Zenon
 */
@SuppressWarnings("unused")
public class LogManager {

    static{
        Path root = Paths.get("./logs");    // default configuration
        build(root, "latest.txt");
    }

    /**
     * Sets the log directory as well as the log file. Can be set anytime.
     * Default is "./logs" for directory and "./logs/latest.txt" for file.
     * @param logDirectory the new log directory
     * @param logFileName the new log file name inside the log directory. The path leading to the log file should
     *                    be <code>logDirectory.resolve(logFileName)</code>.
     * @throws IllegalStateException if a <code>IOException</code> occurs
     */
    public static void build(Path logDirectory, String logFileName){
        Path logFile = logDirectory.resolve(logFileName);

        try{
            if (!Files.exists(logDirectory))
                Files.createDirectory(logDirectory);

            if (!Files.exists(logFile))
                Files.createFile(logFile);

            bufferedWriter = FilesUtils.newUnsafeBufferedWriter(
                    logFile,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND);

        }catch(IOException exception){
            throw new IllegalStateException(exception);
        }
    }

    /**
     * The logging background thread.
     */
    private static final class LogBackground extends Thread{


        LogBackground(String name){
            super(name);
            this.setDaemon(true);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    bufferedWriter.realClose(); // avoid shutting down daemon whilst doing IO, leaving the writer open.
                } catch (IOException ignored) {}    // IOException means we failed to flush the buffer.
                // doesn't matter as long as the writer can get closed
            }));
            this.start();
        }

        @SuppressWarnings("InfiniteLoopStatement")
        @Override
        public void run() {
            while (true) {
                ringBuffer.consumeAll(LogManager::append);

                while(ringBuffer.isEmpty())
                    Thread.yield();
            }
        }
    }

    private static UnsafeBufferedWriter bufferedWriter;

    /**
     * Separate thread only used for logging.
     */
    private static final LogBackground loggingThread = new LogBackground("Logging Thread");

    /**
     * Logger singleton
     */
    private static final BlockingLogger instance = new BlockingLogger();

    /**
     * @return the logger instance to log stuff.
     */
    public static BlockingLogger getLogger() {
        return instance;
    }

    /**
     * Max characters per line in the log file
     */
    private static final byte MAX_CHARACTERS_PER_LINE = Byte.MAX_VALUE;

    /**
     * DateFormatter to get the time formatted in the logs
     */
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * The underlying concurrent String queue that allows passing logs to the logging daemon.
     */
    public static final BlockingTorus<String> ringBuffer = BlockingTorus.build(16);

    /**
     * Formats the text correctly for logging and queue it to the log queue.
     * @param text the core message of the log
     * @param level the level of severity of the message
     * @see BlockingLogger
     */
    static void queueLog(String text, Level level){
        String msg = "<"+LocalTime.now().format(formatter)+"> ["+Thread.currentThread().getName()+'/'
                +level.toString()+"] : "+text;

        ringBuffer.offer(msg);
    }


    /**
     * Core method to append a line at the end of the logs file.
     * @param line the line to log
     */
    private static void append(String line) {
        int stopIndex = line.length() - 1;

        int lineCount = 1 + stopIndex / MAX_CHARACTERS_PER_LINE;

        StringBuilder builder = new StringBuilder();

        try(UnsafeBufferedWriter bw = bufferedWriter){
            for (int i=0; i < lineCount; i++) {
                int startLine = i * MAX_CHARACTERS_PER_LINE;
                if (i > 0)
                    builder.append("\t");

                builder.append(line,
                        startLine,
                        Math.min(stopIndex + 1, startLine + MAX_CHARACTERS_PER_LINE)).append("\n");
                bw.write(builder.toString());
                builder.setLength(0);   // reset builder to not create a new instance every loop turn
            }
        }catch(IOException e) {
            e.printStackTrace();
            System.out.println("failed to log into file");
        }
    }

    /**
     * Different levels of severity concerning logging.
     * @author Zenon
     */
    public enum Level{
        DEBUG, INFO, WARN, ERROR, FATAL
    }
}