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
