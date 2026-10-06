# Değişiklik Günlüğü (Change Log)

## [v2] - Artım 2: Tipli Kayıtlar ve İlk Gerçek Filtre

### Eklenen Dosyalar
- `src/main/java/logflow/model/LogRecord.java`: Değişmez alan modeli (domain model).
- `src/main/java/logflow/pipeline/Emitter.java`: Olay yayma arayüzü.
- `src/main/java/logflow/pipeline/PipelineStage.java`: İşlem hattı aşama arayüzü.
- `src/main/java/logflow/pipeline/ParserStage.java`: CLF ayrıştırıcı aşaması.
- `src/main/java/logflow/pipeline/ConsoleSink.java`: Tipli kayıt konsol çıktısı.
- `src/test/java/logflow/pipeline/CollectingEmitter.java`: Test toplayıcı dublörü.
- `src/test/java/logflow/pipeline/ParserStageTest.java`: 8 adet dosyasız birim testi.
- `data/access-small.log`: Örnek log verisi.

### Güncellenen Dosyalar
- `src/main/java/logflow/Main.java`: Pipeline zincirine ParserStage bağlandı.
- `pom.xml`: JUnit 5 ve JaCoCo yapılandırması eklendi.
- `README.md`: Satır kapsama oranı dokümante edildi.
