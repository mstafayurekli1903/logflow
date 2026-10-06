package logflow.pipeline;

import logflow.model.LogRecord;

public class ConsoleSink {
    public void print(LogRecord record) {
        System.out.printf("[%s] %s %s %s %d - %d bytes (%s)%n",
            record.timestamp(), record.clientIp(), record.method(),
            record.path(), record.status(), record.bytes(), record.userAgent()
        );
    }
}
