package log;

import config.Config;

import java.util.logging.*;

public class CustomLogger {
    private static Logger log;

    /**
     * Creates the custom logger instance
     */
    public static void createLogger(Config config) {
        ConsoleHandler handler = new ConsoleHandler() {
            @Override
            protected void setOutputStream(java.io.OutputStream out) throws SecurityException {
                super.setOutputStream(System.out); // force stdout → white
            }
        };
        handler.setFormatter(new MinimalFormatter());
        handler.setLevel(config.logLevel());

        log = Logger.getLogger(CustomLogger.class.getName());

        log.setUseParentHandlers(false);
        log.addHandler(handler);
        log.setLevel(config.logLevel());
    }

    /**
     * Gets the custom logger instance. Should only be called after {@code createLogger} was called
     */
    public static Logger getLogger() {
        return log;
    }
}

class MinimalFormatter extends Formatter {
    @Override
    public String format(LogRecord record) {
        String message = record.getLevel() + ": " + record.getMessage() + "\n";
        if (record.getThrown() != null) {
            java.io.StringWriter trace = new java.io.StringWriter();
            record.getThrown().printStackTrace(new java.io.PrintWriter(trace));
            message += trace;
        }

        return message;
    }
}
