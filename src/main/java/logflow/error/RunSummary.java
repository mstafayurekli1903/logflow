package logflow.error;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class RunSummary {
    private long totalIn = 0;
    private long totalOut = 0;
    private long totalDeadLetter = 0;
    private long startTimeMs = 0;
    private long endTimeMs = 0;
    private final Map<String, Long> skippedPerStage = new LinkedHashMap<>();

    public void start() {
        this.startTimeMs = System.currentTimeMillis();
    }

    public void stop() {
        this.endTimeMs = System.currentTimeMillis();
    }

    public void incrementIn() { totalIn++; }
    public void incrementOut() { totalOut++; }
    public void incrementDeadLetter() { totalDeadLetter++; }

    public void incrementSkipped(String stageName) {
        skippedPerStage.put(stageName, skippedPerStage.getOrDefault(stageName, 0L) + 1);
    }

    public long getTotalIn() { return totalIn; }
    public long getTotalOut() { return totalOut; }
    public long getTotalDeadLetter() { return totalDeadLetter; }
    public long getElapsedTimeMs() { return endTimeMs - startTimeMs; }
    public Map<String, Long> getSkippedPerStage() { return Collections.unmodifiableMap(skippedPerStage); }

    public void printReport() {
        System.out.println("==================================================");
        System.out.println("            LOGFLOW PIPELINE RUN SUMMARY          ");
        System.out.println("==================================================");
        System.out.printf("  Gelen Kayıt Sayısı (In)          : %d%n", totalIn);
        System.out.printf("  Çıkan Kayıt Sayısı (Out)         : %d%n", totalOut);
        System.out.printf("  Dead-Letter Kanalına Giden Sayı  : %d%n", totalDeadLetter);
        long totalSkipped = skippedPerStage.values().stream().mapToLong(Long::longValue).sum();
        System.out.printf("  Atlanan Kayıt Sayısı (Skipped)   : %d%n", totalSkipped);
        skippedPerStage.forEach((stage, count) ->
            System.out.printf("    - Aşama [%s]: %d atlandı%n", stage, count)
        );
        System.out.printf("  Geçen Süre (Elapsed Time)        : %d ms%n", getElapsedTimeMs());
        System.out.println("==================================================");
    }
}
