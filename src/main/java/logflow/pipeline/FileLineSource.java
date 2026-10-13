package logflow.pipeline;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class FileLineSource implements AutoCloseable {
    private final String filePath;
    private BufferedReader reader;

    public FileLineSource(String filePath) {
        this.filePath = filePath;
    }

    public void open() {
        try {
            this.reader = new BufferedReader(new FileReader(filePath));
        } catch (IOException e) {
            throw new RuntimeException("Dosya okunamadi: " + filePath, e);
        }
    }

    public String nextLine() throws IOException {
        return (reader != null) ? reader.readLine() : null;
    }

    @Override
    public void close() {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException ignored) {}
        }
    }
}
