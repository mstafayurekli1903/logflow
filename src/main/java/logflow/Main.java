package logflow;

import logflow.config.*;
import logflow.error.RunSummary;
import logflow.pipeline.Pipeline;

public class Main {
    public static void main(String[] args) {
        String configFile = null;

        for (int i = 0; i < args.length; i++) {
            if ("--config".equals(args[i]) && i + 1 < args.length) {
                configFile = args[++i];
            }
        }

        if (configFile == null) {
            System.err.println("Hata: '--config <dosya.yaml>' parametresi zorunludur.");
            System.exit(1);
        }

        try {
            StageRegistry registry = new StageRegistry();
            PipelineLoader loader = new PipelineLoader(registry);

            Pipeline pipeline = loader.load(configFile);
            RunSummary summary = pipeline.execute();
            summary.printReport();
        } catch (ConfigurationException e) {
            System.err.println("YAPILANDIRMA HATASI: " + e.getMessage());
            System.exit(2);
        } catch (Exception e) {
            System.err.println("CALISMA ZAMANI HATASI: " + e.getMessage());
            System.exit(3);
        }
    }
}
