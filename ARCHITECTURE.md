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
