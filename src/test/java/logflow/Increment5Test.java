package logflow;

import logflow.config.PipelineLoader;
import logflow.config.StageRegistry;
import logflow.error.*;
import logflow.pipeline.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class Increment5Test {

    private StageRegistry registry;
    private PipelineLoader loader;

    @BeforeEach
    public void setUp() {
        registry = new StageRegistry();
        loader = new PipelineLoader(registry);
    }

    @Test
    public void testChaosDirtyLogProcessing(@TempDir Path tempDir) throws IOException {
        Path dirtyLog = tempDir.resolve("dirty.log");
        Files.writeString(dirtyLog, """
        192.168.1.10 - - [20/Oct/2026:10:55:36 +0000] "GET /index.html HTTP/1.1" 200 1024
        KESILMIS_SATIR
        10.0.0.2 - - [20/Oct/2026:11:05:00 +0000] "GET /api HTTP/1.1" 200 2048
        192.168.1.70 - - [20/Oct/2026:12:05:00 +0000] "GET /neg HTTP/1.1" 200 -500
        """);

        Path cleanOut = tempDir.resolve("clean.txt");
        Path dlOut = tempDir.resolve("dl.txt");
        Path cfg = tempDir.resolve("cfg.yaml");

        String yaml = String.format("""
        pipeline:
          name: chaos-test
          source: { params: { path: "%s" } }
          dead_letter: { params: { path: "%s" } }
          stages:
            - type: parse
              params: { format: clf }
              on_error: DEAD_LETTER
            - type: validate
              params: { min_bytes: 0 }
              on_error: DEAD_LETTER
          sink: { params: { path: "%s" } }
        """, dirtyLog.toString().replace("\\", "/"), dlOut.toString().replace("\\", "/"), cleanOut.toString().replace("\\", "/"));

        Files.writeString(cfg, yaml);

        Pipeline pipeline = loader.load(cfg.toString());
        RunSummary summary = pipeline.execute();

        assertEquals(4, summary.getTotalIn());
        assertEquals(2, summary.getTotalOut());
        assertEquals(2, summary.getTotalDeadLetter());
        assertTrue(Files.exists(dlOut));
        assertTrue(Files.exists(cleanOut));
    }

    @Test
    public void testFailFastPolicy(@TempDir Path tempDir) throws IOException {
        Path dirtyLog = tempDir.resolve("failfast.log");
        Files.writeString(dirtyLog, "HATALI_SATIR\n");

        Path cleanOut = tempDir.resolve("clean.txt");
        Path cfg = tempDir.resolve("cfg.yaml");

        String yaml = String.format("""
        pipeline:
          name: failfast-test
          source: { params: { path: "%s" } }
          stages:
            - type: parse
              params: { format: clf }
              on_error: FAIL_FAST
          sink: { params: { path: "%s" } }
        """, dirtyLog.toString().replace("\\", "/"), cleanOut.toString().replace("\\", "/"));

        Files.writeString(cfg, yaml);

        Pipeline pipeline = loader.load(cfg.toString());
        assertThrows(PipelineException.class, pipeline::execute);
    }

    @Test
    public void testSkipPolicy(@TempDir Path tempDir) throws IOException {
        Path dirtyLog = tempDir.resolve("skip.log");
        Files.writeString(dirtyLog, """
        192.168.1.10 - - [20/Oct/2026:10:55:36 +0000] "GET /index.html HTTP/1.1" 200 1024
        BOZUK_SATIR
        """);

        Path cleanOut = tempDir.resolve("clean.txt");
        Path cfg = tempDir.resolve("cfg.yaml");

        String yaml = String.format("""
        pipeline:
          name: skip-test
          source: { params: { path: "%s" } }
          stages:
            - type: parse
              params: { format: clf }
              on_error: SKIP
          sink: { params: { path: "%s" } }
        """, dirtyLog.toString().replace("\\", "/"), cleanOut.toString().replace("\\", "/"));

        Files.writeString(cfg, yaml);

        Pipeline pipeline = loader.load(cfg.toString());
        RunSummary summary = pipeline.execute();

        assertEquals(2, summary.getTotalIn());
        assertEquals(1, summary.getTotalOut());
        assertEquals(0, summary.getTotalDeadLetter());
        assertEquals(1, summary.getSkippedPerStage().get("parse"));
    }
}
