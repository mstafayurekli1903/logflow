package logflow.pipeline;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class FileSink implements AutoCloseable {
    private final String outputPath;
    private BufferedWriter writer;

    public FileSink(String outputPath) {
        this.outputPath = outputPath;
    }

    public void open() {
        try {
            this.writer = new BufferedWriter(new FileWriter(outputPath));
        } catch (IOException e) {
            throw new RuntimeException("Cikti dosyasi acilamadi: " + outputPath, e);
        }
    }

    public void consume(Object item) {
        if (item == null || writer == null) return;
        try {
            writer.write(item.toString());
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException("Yazma hatasi", e);
        }
    }

    @Override
    public void close() {
        if (writer != null) {
            try {
                writer.flush();
                writer.close();
            } catch (IOException ignored) {}
        }
    }
}
