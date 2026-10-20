package logflow.config;

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

        register("filter", params -> {
            String field = (String) params.get("field");
            String op = (String) params.get("op");
            Object value = params.get("value");

            if (field == null || op == null || value == null) {
                throw new ConfigurationException("'filter' asamasi icin 'field', 'op' ve 'value' parametreleri zorunludur.");
            }

            return switch (field.toLowerCase()) {
                case "status" -> {
                    if (!(value instanceof Number)) {
                        throw new ConfigurationException("'status' filtresi icin sayisal deger bekleniyor, gelen: " + value);
                    }
                    int val = ((Number) value).intValue();
                    yield new PredicateFilterStage(r -> {
                        if (">=".equals(op)) return r.status() >= val;
                        if ("==".equals(op)) return r.status() == val;
                        if ("<=".equals(op)) return r.status() <= val;
                        return true;
                    });
                }
                case "path" -> {
                    String prefix = String.valueOf(value);
                    yield new PredicateFilterStage(r -> {
                        if ("startswith".equalsIgnoreCase(op) || "=".equals(op) || "==".equals(op)) {
                            return r.path().startsWith(prefix);
                        }
                        return r.path().contains(prefix);
                    });
                }
                default -> throw new ConfigurationException("Desteklenmeyen filtre alani: " + field);
            };
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
            return new ProjectionStage(fieldArray);
        });
    }
}
