package logflow.pipeline;

import logflow.error.StageException;
import logflow.model.LogRecord;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParserStage implements PipelineStage<String, LogRecord> {
    private static final String CLF_REGEX = "^(\\S+) (?:\\S+) (?:\\S+) \\[([\\w:/]+\\s[+\\-]\\d{4})\\] \"(\\S+)\\s?(\\S+)?\\s?(\\S+)?\" (\\d{3}|-) (\\d+|-)(?:\\s+\"([^\"]*)\"\\s+\"([^\"]*)\")?$";
    private static final Pattern CLF_PATTERN = Pattern.compile(CLF_REGEX);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH);

    private static final int MAX_LINE_LENGTH = 16384;

    private long malformedCount = 0;
    private boolean throwOnMalformed = false;

    public ParserStage() {}

    public ParserStage(boolean throwOnMalformed) {
        this.throwOnMalformed = throwOnMalformed;
    }

    public long getMalformedCount() {
        return malformedCount;
    }

    public void setThrowOnMalformed(boolean throwOnMalformed) {
        this.throwOnMalformed = throwOnMalformed;
    }

    private boolean containsCorruptedEncoding(String text) {
        if (text == null) return false;
        if (text.contains("\uFFFD")) return true;
        for (char c : text.toCharArray()) {
            // Standart HTTP loglarindaki bozuk binary/non-utf-8 baytlari yakalar
            if (c > 127 && c != 'ğ' && c != 'Ğ' && c != 'ü' && c != 'Ü' && c != 'ş' && c != 'Ş' 
                    && c != 'ı' && c != 'İ' && c != 'ö' && c != 'Ö' && c != 'ç' && c != 'Ç') {
                return true;
            }
        }
        return false;
    }

    @Override
    public void process(String input, Emitter<LogRecord> emitter) {
        if (input == null || input.isBlank()) {
            malformedCount++;
            if (throwOnMalformed) {
                throw new StageException("Bos veya tanimsiz log satiri.");
            }
            return;
        }

        if (input.length() > MAX_LINE_LENGTH) {
            malformedCount++;
            if (throwOnMalformed) {
                throw new StageException("Satir boyutu izin verilen siniri asti (" + input.length() + " > " + MAX_LINE_LENGTH + ")");
            }
            return;
        }

        if (containsCorruptedEncoding(input)) {
            malformedCount++;
            if (throwOnMalformed) {
                throw new StageException("Gecersiz veya bozuk karakter kodlamasi iceren satir.");
            }
            return;
        }

        Matcher matcher = CLF_PATTERN.matcher(input.trim());
        if (!matcher.matches()) {
            malformedCount++;
            if (throwOnMalformed) {
                throw new StageException("CLF formatina uymayan log satiri: " + input);
            }
            return;
        }

        try {
            String clientIp = matcher.group(1);
            Instant timestamp = Instant.from(DATE_FORMATTER.parse(matcher.group(2)));
            String method = matcher.group(3);
            String path = matcher.group(4) != null ? matcher.group(4) : "";
            int status = Integer.parseInt(matcher.group(6));
            long bytes = "-".equals(matcher.group(7)) ? 0L : Long.parseLong(matcher.group(7));
            String userAgent = matcher.group(9) != null ? matcher.group(9) : "";

            LogRecord record = new LogRecord(timestamp, clientIp, method, path, status, bytes, userAgent, Collections.emptyMap(), input);
            emitter.emit(record);
        } catch (StageException se) {
            throw se;
        } catch (Exception e) {
            malformedCount++;
            if (throwOnMalformed) {
                throw new StageException("Ayristirma hatasi: " + e.getMessage(), e);
            }
        }
    }
}
