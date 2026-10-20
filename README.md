# LogFlow Pipeline - Increment 2

## Line Coverage (Satır Kapsama)
- `ParserStage` Satır Kapsama Oranı: %100 (JUnit 5 & JaCoCo ile test edilmiştir).

## Artım 4 - Hata Mesajı Örnekleri (Fail Fast)

1. **Bilinmeyen Aşama Türü:**
   `YAPILANDIRMA HATASI: Bilinmeyen asama turu: 'bilinmeyen_asama'. Kayitli asamalar: [filter, project, parse]`

2. **Eksik Parametre:**
   `YAPILANDIRMA HATASI: 'parse' asamasi icin 'format' parametresi zorunludur.`

3. **Hatalı Tipte Parametre:**
   `YAPILANDIRMA HATASI: 'status' filtresi icin sayisal deger bekleniyor, gelen: sayi_degil`

4. **Boş İşlem Hattı:**
   `YAPILANDIRMA HATASI: Islem hatti bos olamaz. En az bir asama tanimlanmalidir.`
