package logflow.config;

import logflow.error.*;
import logflow.pipeline.*;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

@SuppressWarnings("unchecked")
public class PipelineLoader {
    private final StageRegistry registry;

    public PipelineLoader(StageRegistry registry) {
        this.registry = registry;
    }

    public Pipeline load(String configFilePath) {
        Map<String, Object> yamlData;
        try (InputStream in = new FileInputStream(configFilePath)) {
            yamlData = new Yaml().load(in);
        } catch (Exception e) {
            throw new ConfigurationException("Yapilandirma dosyasi okunamadi: " + configFilePath + " (" + e.getMessage() + ")");
        }

        if (yamlData == null || !yamlData.containsKey("pipeline")) {
            throw new ConfigurationException("Gecersiz yapilandirma: Kok eleman 'pipeline' bulunamadi.");
        }

        Map<String, Object> pConfig = (Map<String, Object>) yamlData.get("pipeline");

        // 1. Source
        Map<String, Object> sourceMap = (Map<String, Object>) pConfig.get("source");
        if (sourceMap == null) throw new ConfigurationException("'source' tanimi eksik.");
        Map<String, Object> sourceParams = (Map<String, Object>) sourceMap.get("params");
        if (sourceParams == null || !sourceParams.containsKey("path")) {
            throw new ConfigurationException("Source icin 'path' parametresi zorunludur.");
        }
        FileLineSource source = new FileLineSource((String) sourceParams.get("path"));

        // 2. Dead Letter Sink
        DeadLetterSink deadLetterSink = null;
        if (pConfig.containsKey("dead_letter")) {
            Map<String, Object> dlMap = (Map<String, Object>) pConfig.get("dead_letter");
            Map<String, Object> dlParams = (Map<String, Object>) dlMap.get("params");
            if (dlParams != null && dlParams.containsKey("path")) {
                deadLetterSink = new DeadLetterSink((String) dlParams.get("path"));
            }
        }

        // 3. Stages
        List<Map<String, Object>> stagesList = (List<Map<String, Object>>) pConfig.get("stages");
        if (stagesList == null || stagesList.isEmpty()) {
            throw new ConfigurationException("Islem hatti bos olamaz. En az bir asama tanimlanmalidir.");
        }

        List<PipelineStageWrapper> stageWrappers = new ArrayList<>();
        for (Map<String, Object> stageMap : stagesList) {
            String type = (String) stageMap.get("type");
            if (type == null || type.isBlank()) {
                throw new ConfigurationException("Asama taniminda 'type' alani eksik.");
            }
            Map<String, Object> params = (Map<String, Object>) stageMap.getOrDefault("params", Collections.emptyMap());
            StageFactory factory = registry.get(type);
            PipelineStage<?, ?> stage = factory.create(params);

            String errorPolicyStr = (String) stageMap.getOrDefault("on_error", "FAIL_FAST");
            ErrorPolicy policy;
            try {
                policy = ErrorPolicy.valueOf(errorPolicyStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException("Bilinmeyen on_error politikasi: " + errorPolicyStr);
            }

            stageWrappers.add(new PipelineStageWrapper(type, stage, policy));
        }

        // 4. Sink
        Map<String, Object> sinkMap = (Map<String, Object>) pConfig.get("sink");
        if (sinkMap == null) throw new ConfigurationException("'sink' tanimi eksik.");
        Map<String, Object> sinkParams = (Map<String, Object>) sinkMap.get("params");
        if (sinkParams == null || !sinkParams.containsKey("path")) {
            throw new ConfigurationException("Sink icin 'path' parametresi zorunludur.");
        }
        FileSink sink = new FileSink((String) sinkParams.get("path"));

        return new Pipeline(source, stageWrappers, sink, deadLetterSink, new RunSummary());
    }
}
