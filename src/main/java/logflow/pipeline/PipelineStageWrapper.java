package logflow.pipeline;

import logflow.error.ErrorPolicy;

public class PipelineStageWrapper {
    private final String name;
    private final PipelineStage<Object, Object> stage;
    private final ErrorPolicy errorPolicy;

    @SuppressWarnings("unchecked")
    public PipelineStageWrapper(String name, PipelineStage<?, ?> stage, ErrorPolicy errorPolicy) {
        this.name = name;
        this.stage = (PipelineStage<Object, Object>) stage;
        this.errorPolicy = errorPolicy;
    }

    public String getName() { return name; }
    public PipelineStage<Object, Object> getStage() { return stage; }
    public ErrorPolicy getErrorPolicy() { return errorPolicy; }
}
