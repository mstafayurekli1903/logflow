package logflow.pipeline;

import logflow.model.LogRecord;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ProjectionStage implements PipelineStage<LogRecord, String> {
    private final List<String> fields;

    public ProjectionStage(String... fields) {
        this.fields = Arrays.asList(fields);
    }

    @Override
    public void process(LogRecord record, Emitter<String> emitter) {
        if (record == null) return;

        String line = fields.stream().map(f -> switch (f.trim().toLowerCase()) {
            case "timestamp" -> String.valueOf(record.timestamp());
            case "clientip"  -> record.clientIp();
            case "method"    -> record.method();
            case "path"      -> record.path();
            case "status"    -> String.valueOf(record.status());
            case "bytes"     -> String.valueOf(record.bytes());
            case "useragent" -> record.userAgent();
            default          -> "";
        }).collect(Collectors.joining(" "));

        emitter.emit(line);
    }
}
