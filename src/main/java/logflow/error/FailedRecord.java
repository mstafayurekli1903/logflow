package logflow.error;

import java.time.Instant;

public record FailedRecord(
    String rawInput,
    String stageName,
    String errorMessage,
    Instant timestamp
) {
    public String toDeadLetterFormat() {
        return String.format("[%s] [STAGE: %s] [ERROR: %s] RAW: %s",
                timestamp, stageName, errorMessage.replace("\n", " "), rawInput);
    }
}
