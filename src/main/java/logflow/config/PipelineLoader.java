package logflow.config;

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

        Map<String, Object> sourceMap = (Map<String, Object>) pConfig.get("source");
        if (sourceMap == null) {
            throw new ConfigurationException("'source' tanimi eksik.");
        }
        Map<String, Object> sourceParams = (Map<String, Object>) sourceMap.get("params");
        if (sourceParams == null || !sourceParams.containsKey("path")) {
            throw new ConfigurationException("Source icin 'path' parametresi zorunludur.");
        }
        String inputPath = (String) sourceParams.get("path");
        FileLineSource source = new FileLineSource(inputPath);

        List<Map<String, Object>> stagesList = (List<Map<String, Object>>) pConfig.get("stages");
        if (stagesList == null || stagesList.isEmpty()) {
            throw new ConfigurationException("Islem hatti bos olamaz. En az bir asama tanimlanmalidir.");
        }

        PipelineBuilder builder = PipelineBuilder.from(source);
        for (Map<String, Object> stageMap : stagesList) {
            String type = (String) stageMap.get("type");
            if (type == null || type.isBlank()) {
                throw new ConfigurationException("Asama taniminda 'type' alani eksik.");
            }
            Map<String, Object> params = (Map<String, Object>) stageMap.getOrDefault("params", Collections.emptyMap());
            StageFactory factory = registry.get(type);
            PipelineStage<?, ?> stage = factory.create(params);
            builder.via((PipelineStage) stage);
        }

        Map<String, Object> sinkMap = (Map<String, Object>) pConfig.get("sink");
        if (sinkMap == null) {
            throw new ConfigurationException("'sink' tanimi eksik.");
        }
        Map<String, Object> sinkParams = (Map<String, Object>) sinkMap.get("params");
        if (sinkParams == null || !sinkParams.containsKey("path")) {
            throw new ConfigurationException("Sink icin 'path' parametresi zorunludur.");
        }
        String outputPath = (String) sinkParams.get("path");
        FileSink sink = new FileSink(outputPath);

        return builder.to(sink);
    }
}
