package logflow.pipeline;

import java.util.ArrayList;
import java.util.List;

public class PipelineBuilder {
    private final FileLineSource source;
    private final List<PipelineStage<?, ?>> stages = new ArrayList<>();

    private PipelineBuilder(FileLineSource source) {
        this.source = source;
    }

    public static PipelineBuilder from(FileLineSource source) {
        return new PipelineBuilder(source);
    }

    public PipelineBuilder via(PipelineStage<?, ?> stage) {
        this.stages.add(stage);
        return this;
    }

    public Pipeline to(FileSink sink) {
        return new Pipeline(this.source, this.stages, sink);
    }
}
