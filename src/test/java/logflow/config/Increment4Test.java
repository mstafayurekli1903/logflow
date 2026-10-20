package logflow.config;

import logflow.pipeline.Pipeline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class Increment4Test {

    private StageRegistry registry;
    private PipelineLoader loader;

    @BeforeEach
    public void setUp() {
        registry = new StageRegistry();
        loader = new PipelineLoader(registry);
    }

    private String createYaml(Path tempDir, String content) throws IOException {
        Path file = tempDir.resolve("config.yaml");
        Files.writeString(file, content);
        return file.toString();
    }

    @Test
    public void testValidConfiguration(@TempDir Path tempDir) throws IOException {
        String yaml = """
        pipeline:
          source: { params: { path: "data/access-small.log" } }
          stages:
            - type: parse
              params: { format: clf }
            - type: filter
              params: { field: status, op: ">=", value: 200 }
            - type: project
              params: { fields: [status, path] }
          sink: { params: { path: "out/test.log" } }
        """;
        Pipeline pipeline = loader.load(createYaml(tempDir, yaml));
        assertNotNull(pipeline);
    }

    @Test
    public void testUnknownStageType(@TempDir Path tempDir) throws IOException {
        String yaml = """
        pipeline:
          source: { params: { path: "data/access-small.log" } }
          stages:
            - type: bilinmeyen_asama
              params: {}
          sink: { params: { path: "out/test.log" } }
        """;
        ConfigurationException ex = assertThrows(ConfigurationException.class, () -> loader.load(createYaml(tempDir, yaml)));
        assertTrue(ex.getMessage().contains("Bilinmeyen asama"));
    }

    @Test
    public void testMissingParameter(@TempDir Path tempDir) throws IOException {
        String yaml = """
        pipeline:
          source: { params: { path: "data/access-small.log" } }
          stages:
            - type: parse
              params: {}
          sink: { params: { path: "out/test.log" } }
        """;
        ConfigurationException ex = assertThrows(ConfigurationException.class, () -> loader.load(createYaml(tempDir, yaml)));
        assertTrue(ex.getMessage().contains("format"));
    }

    @Test
    public void testWrongTypeParameter(@TempDir Path tempDir) throws IOException {
        String yaml = """
        pipeline:
          source: { params: { path: "data/access-small.log" } }
          stages:
            - type: filter
              params: { field: status, op: ">=", value: "sayi_degil" }
          sink: { params: { path: "out/test.log" } }
        """;
        ConfigurationException ex = assertThrows(ConfigurationException.class, () -> loader.load(createYaml(tempDir, yaml)));
        assertTrue(ex.getMessage().contains("sayisal deger bekleniyor"));
    }

    @Test
    public void testEmptyPipeline(@TempDir Path tempDir) throws IOException {
        String yaml = """
        pipeline:
          source: { params: { path: "data/access-small.log" } }
          stages: []
          sink: { params: { path: "out/test.log" } }
        """;
        ConfigurationException ex = assertThrows(ConfigurationException.class, () -> loader.load(createYaml(tempDir, yaml)));
        assertTrue(ex.getMessage().contains("bos olamaz"));
    }
}
