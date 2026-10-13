package logflow;

import logflow.model.LogRecord;
import logflow.pipeline.*;

public class Main {
    public static void main(String[] args) {
        String input = "data/access-small.log";
        String output = "output.log";
        Integer minStatus = null;
        String pathPrefix = null;
        String[] fields = null;

        for (int i = 0; i < args.length; i++) {
            if ("--input".equals(args[i]) && i + 1 < args.length) {
                input = args[++i];
            } else if ("--output".equals(args[i]) && i + 1 < args.length) {
                output = args[++i];
            } else if ("--min-status".equals(args[i]) && i + 1 < args.length) {
                minStatus = Integer.parseInt(args[++i]);
            } else if ("--path-prefix".equals(args[i]) && i + 1 < args.length) {
                pathPrefix = args[++i];
            } else if ("--fields".equals(args[i]) && i + 1 < args.length) {
                fields = args[++i].split(",");
            }
        }

        FileLineSource source = new FileLineSource(input);
        FileSink sink = new FileSink(output);

        PipelineBuilder builder = PipelineBuilder.from(source)
                .via(new ParserStage());

        if (minStatus != null) {
            int statusLimit = minStatus;
            builder.via(new PredicateFilterStage(r -> r.status() >= statusLimit));
        }
        if (pathPrefix != null) {
            String prefix = pathPrefix;
            builder.via(new PredicateFilterStage(r -> r.path().startsWith(prefix)));
        }
        if (fields != null) {
            builder.via(new ProjectionStage(fields));
        } else {
            builder.via(new ProjectionStage("timestamp", "clientIp", "status", "path"));
        }

        Pipeline pipeline = builder.to(sink);
        pipeline.execute();

        System.out.println("Pipeline basariyla tamamlandi -> " + output);
    }
}
