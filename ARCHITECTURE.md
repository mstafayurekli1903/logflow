# Mimari Analiz: Bileşilebilirlik ve Sıralama Deneyi (Artım 3)

## 1. Modülerlik ve Tek Sorumluluk İlkesi (SRP)
- **PredicateFilterStage:** Yalnızca koşulu sağlayan `LogRecord` nesnelerini iletir, dönüştürme yapmaz.
- **ProjectionStage:** Yalnızca kaydın belirli alanlarını seçerek veriyi daraltır (veri minimizasyonu).
- **FileSink:** Çıktıyı dosyaya yazar ve `close()` metodu ile yazma akışını flush ederek veri kaybını engeller.

## 2. Sıralama Deneyi ve Ödünleşimler (Trade-offs)

### Parse Öncesi Filtreleme Neden Olamaz?
Ham metin ayrıştırılmadan önce `status >= 400` gibi alan bazlı mantıksal kontroller uygulanamaz. Bu yüzden `ParserStage` her zaman ilk sırada yer almalıdır.

### Filtreleme mi Önce, Projeksiyon mu Önce?
1. **Durum A: Filter -> Projection (Önerilen)**
   - Kayıtlar önce filtrelenir, sadece eşleşen az sayıdaki kayda alan seçimi uygulanır.
   - **Performans:** Elenecek satırlar için gereksiz String ve liste tahsisi yapılmaz; bellek ve CPU verimliliği yüksektir.
   - **Esneklik:** Filtre, kaydın tüm orijinal alanlarına erişebilir.

2. **Durum B: Projection -> Filter**
   - Tüm kayıtlara projeksiyon uygulanır, ardından filtre çalışır.
   - **Dezavantaj:** Filtrenin ihtiyaç duyduğu alan projeksiyonda dışarıda bırakılırsa filtreleme yapılamaz. Ayrıca elenecek kayıtlar için gereksiz yere String işlemleri yapıldığı için performans düşer.

**Sonuç:** İki sıra da mantıksal olarak aynı veri sonucunu üretse de `Filter -> Projection` sırası belirgin bir kaynak tasarrufu sağlar.

## Artım 4 - Yapılandırma Şeması ve Registry Tasarımı

### 1. Yapılandırma Şeması (YAML)
İşlem hattı topolojisi bildirimsel (declarative) olarak YAML formatında tanımlanır:
- **pipeline.name**: İşlem hattı tanımı.
- **source.params.path**: Girdi dosyası yolu.
- **stages**: Sıralı aşamalar listesi (`type` ve `params`).
- **sink.params.path**: Çıktı hedef dosyası yolu.

### 2. Registry & Factory Kalıbı
`StageRegistry` bileşeni, sabit kodlanmış bir `switch` yapısı yerine genişletilebilir bir `Map<String, StageFactory>` yapısına dayanır. Yeni aşamalar sisteme dinamik olarak kaydedilebilir.

### 3. Fail Fast Doğrulama
Yapılandırma zamanı hataları (eksik parametre, hatalı tip, bilinmeyen aşama), hat başlatılmadan önce `ConfigurationException` ile yakalanır ve erken aşamada kullanıcıya bildirilir.

## Artırım 5 - Hata Yönetimi, Doğrulama ve Dead-Letter Mimarisi

### 1. Hata Modeli ve Sorumluluk Ayrımı
- StageException (Kurtarılabilir / Kayıt Düzeyi): Tek bir log kaydının ayrıştırılması veya doğrulanması sırasında oluşan veri hatasıdır. Pipeline durdurulmaz; kaydın belirlenen politikasına (SKIP, DEAD_LETTER) göre ikincil kanala aktarılır veya atlanır.
- PipelineException (Kurtarılamaz / Sistem Düzeyi): Hatalı YAML konfigürasyonu, erişilemeyen kaynak dosya, dolan disk veya FAIL_FAST politikasının tetiklenmesi durumlarında işlem hattının güvenli bir şekilde derhal durmasını sağlar.

### 2. İkincil Kanal ve Topoloji (Dead-Letter Channel)
İşlem hattı artık düz bir çizgi değildir; hatalı kayıtlar ana akıştan izole edilerek ikincil bir boru hattına yönlendirilir:

[FileLineSource] -> [ParserStage] -(hata)-> [DeadLetterSink (out/dead-letter.txt)]
                         | (başarılı)
                 [ValidationStage] -(hata)-> [DeadLetterSink (out/dead-letter.txt)]
                         | (geçerli)
                   [FilterStage]
                         |
                  [ProjectStage]
                         |
                    [FileSink (out/clean-output.txt)]

### 3. Kaos Testi: Bozuk Veri Türleri ve Ele Alan Politikalar Tablosu

| Bozuk Veri Örneği | Oluşan Hata | Tespit Eden Aşama | Uygulanan Politika | Yönlendirilen Hedef |
| :--- | :--- | :--- | :--- | :--- |
| BU_KESILMIS_BOZUK_BIR_LOG_SATIRIDIR | CLF format uyumsuzluğu | ParserStage | DEAD_LETTER | out/dead-letter.txt |
| 01/Jan/1970 veya 31/Dec/2099 | 1990-2030 dışı mantıksız tarih | ValidationStage | DEAD_LETTER | out/dead-letter.txt |
| 999.999.999.999 | Geçersiz IPv4 formatı | ValidationStage | DEAD_LETTER | out/dead-letter.txt |
| bytes: -500 | Negatif veri boyutu | ValidationStage | DEAD_LETTER | out/dead-letter.txt |
| status: 999 | HTTP durum kodu (100-599 dışı) | ValidationStage | DEAD_LETTER | out/dead-letter.txt |
