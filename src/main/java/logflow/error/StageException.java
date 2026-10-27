package logflow.error;

public class StageException extends RuntimeException {
    public StageException(String message) {
        super(message);
    }

    public StageException(String message, Throwable cause) {
        super(message, cause);
    }
}
