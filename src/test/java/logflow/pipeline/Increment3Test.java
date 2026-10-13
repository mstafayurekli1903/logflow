package logflow.pipeline;

import logflow.model.LogRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class Increment3Test {

    private LogRecord createDummyRecord(int status, String path) {
        return new LogRecord(
                Instant.now(), "127.0.0.1", "GET", path, status, 1024L, "Mozilla/5.0", Collections.emptyMap(), "raw"
        );
    }

    @Test
    public void testPredicateFilterStage() {
        PredicateFilterStage stage = new PredicateFilterStage(r -> r.status() >= 400);
        List<LogRecord> emitted = new ArrayList<>();

        stage.process(createDummyRecord(200, "/home"), emitted::add);
        stage.process(createDummyRecord(404, "/notfound"), emitted::add);
        stage.process(createDummyRecord(500, "/api/error"), emitted::add);

        assertEquals(2, emitted.size());
        assertEquals(404, emitted.get(0).status());
        assertEquals(500, emitted.get(1).status());
    }

    @Test
    public void testProjectionStage() {
        ProjectionStage stage = new ProjectionStage("status", "path");
        List<String> emitted = new ArrayList<>();

        stage.process(createDummyRecord(200, "/api/data"), emitted::add);

        assertEquals(1, emitted.size());
        assertEquals("200 /api/data", emitted.get(0));
    }

    @Test
    public void testFileSinkLifecycle(@TempDir Path tempDir) throws IOException {
        Path outFile = tempDir.resolve("test_sink.log");
        FileSink sink = new FileSink(outFile.toString());

        sink.open();
        sink.consume("line1");
        sink.consume("line2");
        sink.close();

        List<String> lines = Files.readAllLines(outFile);
        assertEquals(2, lines.size());
        assertEquals("line1", lines.get(0));
        assertEquals("line2", lines.get(1));
    }

    @Test
    public void testFullPipelineIntegration(@TempDir Path tempDir) throws IOException {
        Path inputFile = tempDir.resolve("fixture.log");
        Path outputFile = tempDir.resolve("output.log");

        List<String> rawLogs = List.of(
                "192.168.1.1 - - [10/Oct/2026:10:00:00 +0300] \"GET /api/users HTTP/1.1\" 404 128 \"-\" \"curl\"",
                "192.168.1.2 - - [10/Oct/2026:10:01:00 +0300] \"GET /index.html HTTP/1.1\" 200 1024 \"-\" \"curl\"",
                "192.168.1.3 - - [10/Oct/2026:10:02:00 +0300] \"POST /api/orders HTTP/1.1\" 500 256 \"-\" \"curl\""
        );
        Files.write(inputFile, rawLogs);

        Pipeline pipeline = PipelineBuilder.from(new FileLineSource(inputFile.toString()))
                .via(new ParserStage())
                .via(new PredicateFilterStage(r -> r.status() >= 400))
                .via(new ProjectionStage("status", "path"))
                .to(new FileSink(outputFile.toString()));

        pipeline.execute();

        List<String> resultLines = Files.readAllLines(outputFile);
        assertEquals(2, resultLines.size());
        assertEquals("404 /api/users", resultLines.get(0));
        assertEquals("500 /api/orders", resultLines.get(1));
    }
}
