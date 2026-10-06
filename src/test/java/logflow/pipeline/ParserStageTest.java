package logflow.pipeline;

import logflow.model.LogRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ParserStageTest {
    private ParserStage stage;
    private CollectingEmitter<LogRecord> emitter;

    @BeforeEach
    void setUp() {
        stage = new ParserStage();
        emitter = new CollectingEmitter<>();
    }

    @Test
    void testValidLine() {
        String line = "192.168.1.10 - - [10/Oct/2026:13:55:36 +0300] \"GET /index.html HTTP/1.1\" 200 1024 \"-\" \"curl/7.68.0\"";
        stage.process(line, emitter);
        assertEquals(1, emitter.getEmitted().size());
        assertEquals(0, stage.getMalformedCount());
    }

    @Test
    void testMissingFields() {
        String line = "192.168.1.10 [10/Oct/2026:13:55:36 +0300] \"GET /index.html\" 200";
        stage.process(line, emitter);
        assertTrue(emitter.getEmitted().isEmpty());
        assertEquals(1, stage.getMalformedCount());
    }

    @Test
    void testInvalidTimestamp() {
        String line = "192.168.1.10 - - [HATALI_TARIH] \"GET /index.html HTTP/1.1\" 200 1024";
        stage.process(line, emitter);
        assertTrue(emitter.getEmitted().isEmpty());
        assertEquals(1, stage.getMalformedCount());
    }

    @Test
    void testInvalidStatusCode() {
        String line = "192.168.1.10 - - [10/Oct/2026:13:55:36 +0300] \"GET /index.html HTTP/1.1\" NOT_FOUND 1024";
        stage.process(line, emitter);
        assertTrue(emitter.getEmitted().isEmpty());
        assertEquals(1, stage.getMalformedCount());
    }

    @Test
    void testEmptyLine() {
        stage.process("   ", emitter);
        assertTrue(emitter.getEmitted().isEmpty());
        assertEquals(1, stage.getMalformedCount());
    }

    @Test
    void testExtraSpaces() {
        String line = "192.168.1.10   -  -   [10/Oct/2026:13:55:36 +0300] \"GET /index.html HTTP/1.1\" 200 1024";
        stage.process(line, emitter);
        assertTrue(emitter.getEmitted().isEmpty());
        assertEquals(1, stage.getMalformedCount());
    }

    @Test
    void testQuotedUserAgentWithSpaces() {
        String line = "10.0.0.1 - - [10/Oct/2026:13:55:36 +0300] \"POST /api/login HTTP/1.1\" 200 512 \"-\" \"Mozilla/5.0 (Windows NT 10.0; Win64; x64)\"";
        stage.process(line, emitter);
        assertEquals(1, emitter.getEmitted().size());
        assertEquals("Mozilla/5.0 (Windows NT 10.0; Win64; x64)", emitter.getEmitted().get(0).userAgent());
    }

    @Test
    void testQueryStringInPath() {
        String line = "10.0.0.2 - - [10/Oct/2026:13:55:36 +0300] \"GET /search?q=test&lang=tr HTTP/1.1\" 200 4096";
        stage.process(line, emitter);
        assertEquals(1, emitter.getEmitted().size());
        assertEquals("/search?q=test&lang=tr", emitter.getEmitted().get(0).path());
    }
}
