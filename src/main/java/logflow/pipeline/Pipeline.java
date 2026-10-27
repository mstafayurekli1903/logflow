package logflow.pipeline;

import logflow.error.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Pipeline {
    private final FileLineSource source;
    private final List<PipelineStageWrapper> stages;
    private final FileSink sink;
    private final DeadLetterSink deadLetterSink;
    private final RunSummary summary;

    public Pipeline(FileLineSource source,
                    List<PipelineStageWrapper> stages,
                    FileSink sink,
                    DeadLetterSink deadLetterSink,
                    RunSummary summary) {
        this.source = source;
        this.stages = stages;
        this.sink = sink;
        this.deadLetterSink = deadLetterSink;
        this.summary = summary != null ? summary : new RunSummary();
    }

    @SuppressWarnings("unchecked")
    public Pipeline(FileLineSource source, List<PipelineStage<?, ?>> oldStages, FileSink sink) {
        this.source = source;
        this.sink = sink;
        this.deadLetterSink = null;
        this.summary = new RunSummary();
        this.stages = new ArrayList<>();
        if (oldStages != null) {
            for (PipelineStage<?, ?> s : oldStages) {
                this.stages.add(new PipelineStageWrapper(s.getClass().getSimpleName(), s, ErrorPolicy.SKIP));
            }
        }
    }

    @SuppressWarnings("unchecked")
    public RunSummary execute() {
        summary.start();
        try {
            source.open();
            sink.open();
            if (deadLetterSink != null) {
                deadLetterSink.open();
            }

            for (PipelineStageWrapper w : stages) {
                Object stageObj = w.getStage();
                if (stageObj instanceof ParserStage ps) {
                    ps.setThrowOnMalformed(true);
                }
            }

            String rawLine;
            while ((rawLine = source.nextLine()) != null) {
                summary.incrementIn();

                List<Object> currentBatch = new ArrayList<>(Collections.singletonList(rawLine));
                boolean droppedToDeadLetterOrSkip = false;

                for (PipelineStageWrapper wrapper : stages) {
                    List<Object> nextBatch = new ArrayList<>();
                    PipelineStage<Object, Object> stage = wrapper.getStage();
                    Emitter<Object> emitter = nextBatch::add;

                    for (Object item : currentBatch) {
                        try {
                            stage.process(item, emitter);
                        } catch (Exception ex) {
                            droppedToDeadLetterOrSkip = true;
                            ErrorPolicy policy = wrapper.getErrorPolicy();

                            if (policy == ErrorPolicy.FAIL_FAST) {
                                throw new PipelineException("FAIL_FAST politikasi tetiklendi! Asama: " + wrapper.getName()
                                        + " Hata: " + ex.getMessage(), ex);
                            } else if (policy == ErrorPolicy.SKIP) {
                                summary.incrementSkipped(wrapper.getName());
                            } else if (policy == ErrorPolicy.DEAD_LETTER) {
                                summary.incrementDeadLetter();
                                if (deadLetterSink != null) {
                                    FailedRecord failed = new FailedRecord(
                                            rawLine,
                                            wrapper.getName(),
                                            ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName(),
                                            Instant.now()
                                    );
                                    deadLetterSink.write(failed);
                                }
                            }
                            break;
                        }
                    }

                    if (droppedToDeadLetterOrSkip) {
                        break;
                    }
                    currentBatch = nextBatch;
                    if (currentBatch.isEmpty()) {
                        break;
                    }
                }

                if (!droppedToDeadLetterOrSkip) {
                    for (Object outItem : currentBatch) {
                        sink.consume(outItem.toString());
                        summary.incrementOut();
                    }
                }
            }
        } catch (PipelineException pe) {
            throw pe;
        } catch (Exception e) {
            throw new PipelineException("Islem hatti yurutulurken hata: " + e.getMessage(), e);
        } finally {
            summary.stop();
            try { source.close(); } catch (Exception ignored) {}
            try { sink.close(); } catch (Exception ignored) {}
            try { if (deadLetterSink != null) deadLetterSink.close(); } catch (Exception ignored) {}
        }
        return summary;
    }

    public RunSummary getSummary() {
        return summary;
    }
}
