package logflow.model;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

public record LogRecord(
    Instant timestamp,
    String clientIp,
    String method,
    String path,
    int status,
    long bytes,
    String userAgent,
    Map<String, String> attributes,
    String raw
) {
    public LogRecord {
        attributes = (attributes == null) ? Collections.emptyMap() : Collections.unmodifiableMap(attributes);
    }
}
