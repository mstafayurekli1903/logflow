package logflow.pipeline;

import logflow.model.LogRecord;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParserStage implements PipelineStage<String, LogRecord> {
    private static final Pattern CLF_PATTERN = Pattern.compile(
        "^(\\S+) \\S+ \\S+ \\[(.*?)\\] \"(\\S+) (.*?) \\S+\" (\\d{3}) (\\d+|-)(?: \".*?\" \"(.*?)\")?.*$"
    );
    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH);

    private int malformedCount = 0;

    @Override
    public void process(String line, Emitter<LogRecord> emitter) {
        if (line == null || line.trim().isEmpty()) {
            malformedCount++;
            return;
        }

        Matcher matcher = CLF_PATTERN.matcher(line);
        if (!matcher.matches()) {
            malformedCount++;
            return;
        }

        try {
            String clientIp = matcher.group(1);
            String timestampStr = matcher.group(2);
            String method = matcher.group(3);
            String path = matcher.group(4);
            int status = Integer.parseInt(matcher.group(5));
            String bytesStr = matcher.group(6);
            long bytes = bytesStr.equals("-") ? 0L : Long.parseLong(bytesStr);
            String userAgent = matcher.group(7) != null ? matcher.group(7) : "";

            Instant timestamp = ZonedDateTime.parse(timestampStr, DATE_FORMATTER).toInstant();

            LogRecord record = new LogRecord(
                timestamp, clientIp, method, path, status, bytes, userAgent, Collections.emptyMap(), line
            );
            emitter.emit(record);
        } catch (Exception e) {
            malformedCount++;
        }
    }

    public int getMalformedCount() {
        return malformedCount;
    }
}
