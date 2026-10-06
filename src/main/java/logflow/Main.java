package logflow;

import logflow.pipeline.ConsoleSink;
import logflow.pipeline.ParserStage;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        String logFilePath = (args.length > 0) ? args[0] : "data/access-small.log";
        ParserStage parser = new ParserStage();
        ConsoleSink sink = new ConsoleSink();

        try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                parser.process(line, sink::print);
            }
        } catch (IOException e) {
            System.err.println("Dosya hatasi: " + e.getMessage());
            return;
        }

        System.out.println("----------------------------------------");
        System.out.println("Toplam atlanan hatali satir sayisi: " + parser.getMalformedCount());
    }
}
