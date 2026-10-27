package logflow.pipeline;

import logflow.error.StageException;
import logflow.model.LogRecord;

import java.time.Instant;

public class ValidationStage implements PipelineStage<LogRecord, LogRecord> {
    private final int minStatus;
    private final int maxStatus;
    private final long minBytes;
    private final boolean checkIp;
    private final boolean checkTimestamp;

    public ValidationStage(int minStatus, int maxStatus, long minBytes, boolean checkIp, boolean checkTimestamp) {
        this.minStatus = minStatus;
        this.maxStatus = maxStatus;
        this.minBytes = minBytes;
        this.checkIp = checkIp;
        this.checkTimestamp = checkTimestamp;
    }

    private boolean isValidIpv4(String ip) {
        if (ip == null || ip.isBlank()) return false;
        String[] parts = ip.split("\\.");
        if (parts.length != 4) return false;
        try {
            for (String p : parts) {
                int octet = Integer.parseInt(p);
                if (octet < 0 || octet > 255) return false;
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public void process(LogRecord record, Emitter<LogRecord> emitter) {
        if (record == null) {
            throw new StageException("Kayit null olamaz.");
        }

        if (record.status() < minStatus || record.status() > maxStatus) {
            throw new StageException(String.format("Gecersiz HTTP durum kodu: %d (%d-%d araliginda olmali)",
                    record.status(), minStatus, maxStatus));
        }

        if (record.bytes() < minBytes) {
            throw new StageException("Negatif veya gecersiz byte boyutu: " + record.bytes());
        }

        if (checkIp) {
            if (!isValidIpv4(record.clientIp())) {
                throw new StageException("Gecersiz IP adresi formati: " + record.clientIp());
            }
        }

        if (checkTimestamp && record.timestamp() != null) {
            Instant minTime = Instant.parse("1990-01-01T00:00:00Z");
            Instant maxTime = Instant.parse("2030-01-01T00:00:00Z");
            if (record.timestamp().isBefore(minTime) || record.timestamp().isAfter(maxTime)) {
                throw new StageException("Gecersiz zaman damgasi mantigi (1990-2030 disi): " + record.timestamp());
            }
        }

        emitter.emit(record);
    }
}
