package logflow.config;

import logflow.model.LogRecord;
import logflow.pipeline.*;
import java.util.*;

public class StageRegistry {
    private final Map<String, StageFactory> registry = new HashMap<>();

    public StageRegistry() {
        registerDefaults();
    }

    public void register(String type, StageFactory factory) {
        registry.put(type.toLowerCase(), factory);
    }

    public StageFactory get(String type) {
        StageFactory factory = registry.get(type.toLowerCase());
        if (factory == null) {
            throw new ConfigurationException("Bilinmeyen asama turu: '" + type + "'. Kayitli asamalar: " + registry.keySet());
        }
        return factory;
    }

    private void registerDefaults() {
        register("parse", params -> {
            String format = (String) params.get("format");
            if (format == null || format.isBlank()) {
                throw new ConfigurationException("'parse' asamasi icin 'format' parametresi zorunludur.");
            }
            if (!"clf".equalsIgnoreCase(format)) {
                throw new ConfigurationException("Desteklenmeyen log formati: '" + format + "'. Sadece 'clf' desteklenir.");
            }
            return new ParserStage();
        });

        register("validate", params -> {
            int minStatus = params.containsKey("min_status") ? ((Number) params.get("min_status")).intValue() : 100;
            int maxStatus = params.containsKey("max_status") ? ((Number) params.get("max_status")).intValue() : 599;
            long minBytes = params.containsKey("min_bytes") ? ((Number) params.get("min_bytes")).longValue() : 0L;
            boolean checkIp = !params.containsKey("check_ip") || Boolean.parseBoolean(String.valueOf(params.get("check_ip")));
            boolean checkTimestamp = !params.containsKey("check_timestamp") || Boolean.parseBoolean(String.valueOf(params.get("check_timestamp")));

            return new ValidationStage(minStatus, maxStatus, minBytes, checkIp, checkTimestamp);
        });

        register("filter", params -> {
            String field = (String) params.get("field");
            String op = (String) params.get("op");
            Object value = params.get("value");

            if (field == null || op == null || value == null) {
                throw new ConfigurationException("'filter' asamasi icin 'field', 'op' ve 'value' parametreleri zorunludur.");
            }

            if ("status".equalsIgnoreCase(field)) {
                if (!(value instanceof Number)) {
                    throw new ConfigurationException("'status' filtresi icin sayisal deger bekleniyor, gelen: " + value);
                }
                int val = ((Number) value).intValue();
                return (PipelineStage<LogRecord, LogRecord>) (record, emitter) -> {
                    boolean matches = false;
                    if (">=".equals(op)) matches = record.status() >= val;
                    else if ("==".equals(op)) matches = record.status() == val;
                    else if ("<=".equals(op)) matches = record.status() <= val;
                    if (matches) {
                        emitter.emit(record);
                    }
                };
            } else if ("path".equalsIgnoreCase(field)) {
                String prefix = String.valueOf(value);
                return (PipelineStage<LogRecord, LogRecord>) (record, emitter) -> {
                    boolean matches;
                    if ("startswith".equalsIgnoreCase(op) || "=".equals(op) || "==".equals(op)) {
                        matches = record.path().startsWith(prefix);
                    } else {
                        matches = record.path().contains(prefix);
                    }
                    if (matches) {
                        emitter.emit(record);
                    }
                };
            } else {
                throw new ConfigurationException("Desteklenmeyen filtre alani: " + field);
            }
        });

        register("project", params -> {
            Object rawFields = params.get("fields");
            if (rawFields == null) {
                throw new ConfigurationException("'project' asamasi icin 'fields' listesi zorunludur.");
            }
            if (!(rawFields instanceof List<?> list)) {
                throw new ConfigurationException("'fields' parametresi liste tipinde olmalidir.");
            }
            String[] fieldArray = list.stream().map(Object::toString).toArray(String[]::new);
            return (PipelineStage<LogRecord, String>) (record, emitter) -> {
                List<String> values = new ArrayList<>();
                for (String f : fieldArray) {
                    switch (f.toLowerCase()) {
                        case "timestamp" -> values.add(record.timestamp().toString());
                        case "clientip", "ip" -> values.add(record.clientIp());
                        case "method" -> values.add(record.method());
                        case "path" -> values.add(record.path());
                        case "status" -> values.add(String.valueOf(record.status()));
                        case "bytes" -> values.add(String.valueOf(record.bytes()));
                        default -> values.add("");
                    }
                }
                emitter.emit(String.join(" | ", values));
            };
        });
    }
}
