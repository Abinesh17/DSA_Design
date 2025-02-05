
package logger.logmanager;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class LoggingFramework {

    public enum LogLevel {
        DEBUG, INFO, WARNING, ERROR, FATAL
    }

    public static class LogMessage
    {
        LogLevel level;
        String message;
        LocalDateTime timestamp;
        public LogMessage(LogLevel level, String message, LocalDateTime timestamp)
        {
            this.level = level;
            this.message = message;
            this.timestamp = timestamp;
        }

        public String toString()
        {
            return "[ "+level+" ] [ "+timestamp.toString()+" ] "+message+ "\n";
        }
    }
    public interface LogHandler
    {
        public void log(LogMessage message);
    }

    public static class ConsoleLogHandler implements LogHandler
    {
        @Override
        public void log(LogMessage message) {
            System.out.println(message.toString());
        }
    }

    public static class FileLogHandler implements LogHandler
    {
        private String filePath;
        public FileLogHandler(String filePath)
        {
            this.filePath = filePath;
        }

        @Override
        public void log(LogMessage message) {
            try(FileWriter writer = new FileWriter(filePath))
            {
                writer.write(message.toString());
            }
            catch (IOException ex)
            {
                System.err.println("Failed to write to file: "+ ex.getMessage());
            }
        }
    }

    public static class LogManager
    {
        private Set<LogLevel> enabledLevels;
        private Set<LogHandler> logHandlers;
        private Thread workerThread;
        private BlockingQueue<LogMessage> blockingQueue;
        private volatile boolean running = true;
        public LogManager(Set<LogLevel> enabledLevels, Set<LogHandler> logHandlers)
        {
            this.enabledLevels = enabledLevels;
            this.logHandlers = logHandlers;
            this.blockingQueue = new LinkedBlockingQueue<>();
            workerThread = new Thread(this::processLogs);
            workerThread.start();

            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        }

        private void processLogs()
        {
            while(!blockingQueue.isEmpty() || running)
            {
                try {
                    LogMessage message = blockingQueue.take(); // No CPU wastage: It only wakes up when a log is available.
                    for (LogHandler logHandler : logHandlers) {
                        logHandler.log(message);
                    }
                }
                catch (Exception ex)
                {
                    System.err.println("Exception ocurred : "+ ex.getMessage());
                }
            }
        }
        public void shutdown()
        {
            running = false;
            workerThread.interrupt();
            processLogs();
        }

        public void log(LocalDateTime dateTime, LogLevel level, String message)
        {
            if(!this.enabledLevels.contains(level)) return;

            this.blockingQueue.offer(new LogMessage(level, message, dateTime));
        }
    }

    public static void main(String[] args) {
        // we need to support multiple logging levels (INFO, WARNING, ERROR, FATAL, DEBUG)
        // should be configurable to include/exclude logging levels
        // should have multiple output sources
        // should be thread safe
        LoggingFramework.LogManager logManager = new LoggingFramework.LogManager(Set.of(LogLevel.values()), Set.of(new ConsoleLogHandler(), new FileLogHandler("")));
        logManager.log(LocalDateTime.now(),LogLevel.INFO,"First log");
    }
}
