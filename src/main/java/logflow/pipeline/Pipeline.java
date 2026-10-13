package logflow.pipeline;

import java.io.IOException;
import java.util.List;

public class Pipeline {
    private final FileLineSource source;
    private final List<PipelineStage<?, ?>> stages;
    private final FileSink sink;

    public Pipeline(FileLineSource source, List<PipelineStage<?, ?>> stages, FileSink sink) {
        this.source = source;
        this.stages = stages;
        this.sink = sink;
    }

    @SuppressWarnings("unchecked")
    public void execute() {
        try {
            source.open();
            sink.open();

            String line;
            while ((line = source.nextLine()) != null) {
                runStage(0, line);
            }
        } catch (IOException e) {
            throw new RuntimeException("Pipeline calisma hatasi", e);
        } finally {
            sink.close();
            source.close();
        }
    }

    @SuppressWarnings("unchecked")
    private void runStage(int index, Object item) {
        if (index >= stages.size()) {
            sink.consume(item);
            return;
        }

        PipelineStage stage = stages.get(index);
        stage.process(item, nextItem -> runStage(index + 1, nextItem));
    }
}
