package logflow.error;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class DeadLetterSink implements AutoCloseable {
    private final String outputPath;
    private BufferedWriter writer;

    public DeadLetterSink(String outputPath) {
        this.outputPath = outputPath;
    }

    public void open() throws IOException {
        if (outputPath != null && !outputPath.isBlank()) {
            java.io.File file = new java.io.File(outputPath);
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            this.writer = new BufferedWriter(new FileWriter(file));
        }
    }

    public void write(FailedRecord failedRecord) {
        if (writer != null) {
            try {
                writer.write(failedRecord.toDeadLetterFormat());
                writer.newLine();
            } catch (IOException e) {
                System.err.println("Dead-letter hedefine yazılamadı: " + e.getMessage());
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (writer != null) {
            writer.flush();
            writer.close();
        }
    }
}
