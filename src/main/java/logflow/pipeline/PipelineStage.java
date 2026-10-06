package logflow.pipeline;

public interface PipelineStage<I, O> {
    void process(I input, Emitter<O> emitter);
}
