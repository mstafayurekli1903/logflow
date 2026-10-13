package logflow.pipeline;

import logflow.model.LogRecord;
import java.util.function.Predicate;

public class PredicateFilterStage implements PipelineStage<LogRecord, LogRecord> {
    private final Predicate<LogRecord> predicate;

    public PredicateFilterStage(Predicate<LogRecord> predicate) {
        this.predicate = predicate;
    }

    @Override
    public void process(LogRecord input, Emitter<LogRecord> emitter) {
        if (input != null && predicate.test(input)) {
            emitter.emit(input);
        }
    }
}
