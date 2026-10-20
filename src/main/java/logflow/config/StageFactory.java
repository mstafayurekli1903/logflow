package logflow.config;

import logflow.pipeline.PipelineStage;
import java.util.Map;

@FunctionalInterface
public interface StageFactory {
    PipelineStage<?, ?> create(Map<String, Object> params);
}
