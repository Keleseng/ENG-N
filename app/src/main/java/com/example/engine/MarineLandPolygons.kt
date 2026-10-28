package com.example.engine

import kotlin.math.max
import kotlin.math.min

/**
 * Kapalı Kara Kütlesi veya Ada Poligonu.
 * Koordinatlar [lat0, lon0, lat1, lon1, ...] biçiminde düz DoubleArray içinde tutulur.
 */
class LandPolygon(
  val name: String,
  val isIsland: Boolean,
  val minLat: Double,
  val maxLat: Double,
  val minLon: Double,
  val maxLon: Double,
  val coords: DoubleArray
) {
  val vertexCount: Int get() = coords.size / 2
}

/**
 * Türkiye Çevresi Denizler (Marmara, İstanbul ve Çanakkale Boğazları, İzmit Körfezi,
 * Ege Denizi, Akdeniz, Kıbrıs ve Karadeniz) için Yüksek Hassasiyetli Kara ve Ada Poligonları.
 *
 * Tüm burunlar, yarımadalar, boğaz kıyıları ve adalar gerçek seyir haritası koordinatlarına
 * göre tanımlanmıştır; hiçbir rota veya dönüş noktası (DN) bu poligonların içinden geçemez.
 */
object MarineLandPolygons {

  private fun poly(name: String, isIsland: Boolean = true, vararg latLon: Double): LandPolygon {
    require(latLon.size >= 6 && latLon.size % 2 == 0) { "Invalid polygon $name" }
    var minLat = Double.POSITIVE_INFINITY
    var maxLat = Double.NEGATIVE_INFINITY
    var minLon = Double.POSITIVE_INFINITY
    var maxLon = Double.NEGATIVE_INFINITY
    var i = 0
    while (i < latLon.size) {
      val lat = latLon[i]
      val lon = latLon[i + 1]
      minLat = min(minLat, lat)
      maxLat = max(maxLat, lat)
      minLon = min(minLon, lon)
      maxLon = max(maxLon, lon)
      i += 2
    }
    return LandPolygon(name, isIsland, minLat, maxLat, minLon, maxLon, latLon)
  }

  // 1. TRAKYA, İSTANBUL AVRUPA YAKASI, GELİBOLU YARIMADASI VE YUNANİSTAN ANAKARASI
  private val thraceAndGreeceMainland = poly(
    name = "TRAKYA_GELIBOLU_AVRUPA_ANAKARA",
    isIsland = false,
    // Kuzeybatı iç sınır -> Bulgaristan / Karadeniz Trakya kıyısı
    42.3000, 22.0000,
    42.3000, 27.9600,
    41.9800, 28.0200, // Rezve
    41.8820, 27.9820, // İğneada Burnu içi
    41.6400, 28.0880, // Kıyıköy
    41.4800, 28.3200, // Çilingoz
    41.3500, 28.6800, // Terkos / Karaburun (Karadeniz)
    41.2580, 28.9800, // Kilyos
    41.2345, 29.1125, // Rumeli Feneri
    // İstanbul Boğazı Avrupa Kıyısı (Kuzeyden Güneye)
    41.2060, 29.1000, // Garipçe Burnu
    41.1820, 29.0745, // Rumeli Kavağı
    41.1745, 29.0680, // Telli Baba
    41.1685, 29.0565, // Sarıyer
    41.1610, 29.0460, // Büyükdere
    41.1475, 29.0510, // Kireçburnu
    41.1390, 29.0560, // Tarabya
    41.1285, 29.0645, // Kalender
    41.1205, 29.0700, // Yeniköy Köybaşı Burnu
    41.1125, 29.0570, // İstinye
    41.1040, 29.0555, // Emirgan
    41.0955, 29.0540, // Baltalimanı
    41.0845, 29.0558, // Rumeli Hisarı / Aşiyan
    41.0765, 29.0435, // Bebek
    41.0672, 29.0445, // Arnavutköy Akıntıburnu
    41.0580, 29.0360, // Kuruçeşme
    41.0472, 29.0265, // Ortaköy
    41.0415, 29.0055, // Beşiktaş
    41.0330, 28.9930, // Kabataş
    41.0235, 28.9775, // Karaköy
    41.0360, 28.9520, // Haliç İç Kıyı
    41.0210, 28.9720, // Eminönü
    41.0168, 28.9860, // Sarayburnu
    41.0035, 28.9825, // Ahırkapı
    // Marmara Kuzey Kıyısı
    41.0020, 28.9530, // Yenikapı
    40.9850, 28.9020, // Zeytinburnu
    40.9715, 28.8750, // Ataköy Marina / Bakırköy Burnu
    40.9555, 28.8260, // Yeşilköy Burnu
    40.9750, 28.7650, // Küçükçekmece / Avcılar
    40.9640, 28.6850, // Ambarlı Burnu
    41.0050, 28.5750, // Büyükçekmece
    41.0020, 28.5450, // Baba Burnu
    41.0720, 28.2450, // Silivri
    41.0450, 28.0500, // Gümüşyaka
    40.9675, 27.9620, // Marmaraereğlisi Burnu
    40.9920, 27.8000, // Yeniçiftlik
    40.9750, 27.5200, // Tekirdağ
    40.8680, 27.4600, // Kumbağ
    40.7060, 27.3150, // Hoşköy / Hora Feneri
    40.6150, 27.1120, // Şarköy
    40.5450, 26.9800, // Mürefte / İnceburun
    // Çanakkale Boğazı Avrupa (Gelibolu) Kıyısı
    40.4120, 26.6720, // Gelibolu
    40.3450, 26.6080, // Sütlüce (1915 Köprüsü Batı Ayağı)
    40.2950, 26.5350, // Karakova
    40.2320, 26.4360, // Akbaş Burnu
    40.2120, 26.3920, // Bigalı
    40.2040, 26.3600, // Kilya Koyu
    40.1845, 26.3565, // Eceabat
    40.1480, 26.3798, // Kilitbahir Burnu (En Dar Geçit Batı Kıyısı)
    40.1050, 26.3320, // Soğanlıdere
    40.0560, 26.2150, // Morto Koyu
    40.0430, 26.1775, // Seddülbahir / Mehmetçik Burnu
    // Gelibolu Batı Kıyısı & Saros Körfezi
    40.0900, 26.1680, // Tekke Burnu
    40.1950, 26.2620, // Kabatepe
    40.3120, 26.2220, // Suvla / Büyükkemikli Burnu
    40.4600, 26.4800, // Despot
    40.6250, 26.8500, // Saros Körfezi İç (Bolayır Kuzeyi / Kavak)
    40.6400, 26.4500, // Erikli / Mecidiye
    40.6100, 26.0750, // Büyükevren / Enez Güney
    40.7250, 26.0600, // Enez
    // Kuzey Yunanistan & Halkidiki & Mora Anakarası
    40.8500, 25.8000, // Dedeağaç
    40.9200, 24.4200, // Kavala
    40.1200, 24.3000, // Aynoroz (Athos) Doğu
    39.9600, 23.9200, // Sikia
    39.9200, 23.4200, // Kassandra
    40.5500, 22.9200, // Selanik İç
    39.1000, 23.0500, // Volos / Pilion
    38.0500, 24.0200, // Attika Doğu
    37.6500, 24.0200, // Sounion Burnu
    37.9400, 23.6200, // Pire
    37.4500, 23.5200, // Argolis
    36.4300, 23.1800, // Malea Burnu
    36.3800, 22.4800, // Matapan Burnu
    36.8000, 21.6800, // Methoni
    39.0000, 20.6000  // Batı Yunanistan
  )

  // 2. ANADOLU ANAKARASI (KARADENİZ, BOĞAZ ANADOLU, İZMİT KÖRFEZİ, GÜNEY MARMARA, ÇANAKKALE ANADOLU, EGE, AKDENİZ)
  private val anatoliaMainland = poly(
    name = "ANADOLU_ANAKARA",
    isIsland = false,
    // Doğu ve Güneydoğu İç Sınır Köşeleri
    41.5500, 42.5000,
    41.5200, 41.5400, // Sarp Sınırı
    41.4050, 41.4340, // Hopa
    41.3500, 41.3100, // Arhavi
    41.2650, 41.1420, // Fındıklı
    41.1760, 40.8880, // Pazar
    41.0900, 40.7300, // Çayeli
    41.0300, 40.5280, // Rize
    40.9420, 40.2650, // Of
    40.9160, 40.2120, // Sürmene Çamburnu
    40.9120, 40.1200, // Sürmene
    41.0020, 39.7400, // Trabzon
    41.0200, 39.5750, // Akçaabat
    41.1050, 39.4200, // Yoros Burnu
    41.0320, 39.0400, // Görele
    41.0050, 38.8180, // Tirebolu
    40.9500, 38.7120, // Espiye
    40.9180, 38.3880, // Giresun
    40.9850, 37.8880, // Ordu
    41.1320, 37.6800, // Yason Burnu (Çaka)
    41.0300, 37.5020, // Fatsa
    41.1320, 37.2900, // Ünye
    41.3850, 36.6500, // Çarşamba Burnu
    41.2380, 36.4580, // Tekkeköy
    41.2920, 36.3480, // Samsun
    41.5650, 35.9120, // Bafra Yaklaşım
    41.7320, 35.9580, // Bafra Burnu (Kızılırmak Deltası)
    41.6800, 35.4500, // Alaçam
    41.8000, 35.2020, // Gerze
    // Sinop Yarımadası & İnceburun
    42.0180, 35.1500, // Sinop İç Liman Kıstak
    42.0220, 35.2080, // Boztepe Burnu Doğu Ucu
    42.0460, 35.1820, // Boztepe Kuzey
    42.0400, 35.0600, // Akliman
    42.0965, 34.9430, // Sinop İnceburun (En Kuzey Uç)
    41.9460, 34.5880, // Ayancık
    41.9760, 34.0180, // Abana
    41.9760, 33.7680, // İnebolu
    42.0160, 33.3350, // Kerempe Burnu
    41.8930, 32.9620, // Cide
    41.8460, 32.7220, // Kurucaşile
    41.7480, 32.3880, // Amasra
    41.6820, 32.2260, // Bartın Liman Ağzı
    41.5710, 32.0260, // Filyos
    41.4540, 31.7890, // Zonguldak
    41.2920, 31.3920, // Kdz. Ereğli Baba Burnu
    41.2780, 31.4220, // Kdz. Ereğli İç
    41.0900, 31.1200, // Akçakoca
    41.1120, 30.6900, // Karasu
    41.1730, 30.2180, // Kefken / Cebeci
    41.1400, 29.8550, // Ağva
    41.1755, 29.6120, // Şile Burnu
    41.2240, 29.2200, // Riva / Elmasburnu
    41.2180, 29.1520, // Anadolu Feneri
    // İstanbul Boğazı Anadolu Kıyısı (Kuzeyden Güneye)
    41.2040, 29.1310, // Poyrazköy
    41.1920, 29.1080, // Filburnu
    41.1740, 29.0890, // Anadolu Kavağı
    41.1620, 29.0780, // Macar Tabya
    41.1480, 29.0770, // Selvi Burnu / Umuryeri
    41.1350, 29.0905, // Beykoz
    41.1180, 29.0930, // Paşabahçe
    41.1085, 29.0820, // Çubuklu
    41.0998, 29.0655, // Kanlıca Burnu
    41.0822, 29.0668, // Anadolu Hisarı
    41.0745, 29.0575, // Kandilli Burnu
    41.0640, 29.0538, // Vaniköy
    41.0505, 29.0518, // Çengelköy
    41.0430, 29.0408, // Beylerbeyi
    41.0355, 29.0285, // Kuzguncuk
    41.0265, 29.0135, // Üsküdar
    41.0180, 29.0068, // Salacak
    40.9990, 29.0155, // Haydarpaşa
    // İstanbul Anadolu Marmara & Tuzla & İzmit Körfezi Kuzey Kıyısı
    40.9790, 29.0240, // Moda Burnu
    40.9685, 29.0355, // Fenerbahçe Burnu
    40.9615, 29.0620, // Caddebostan / Dalyan Burnu
    40.9520, 29.0955, // Bostancı
    40.9160, 29.1320, // Maltepe / Dragos
    40.8850, 29.1880, // Kartal
    40.8745, 29.2330, // Pendik Burnu
    40.8615, 29.2575, // Pavli Yarımadası (Pendik Tersane)
    40.8570, 29.2760, // Güzelyalı
    40.8445, 29.2930, // Tuzla Tersane Koyu İç
    40.8240, 29.2880, // Tuzla İçmeler Sahil
    40.8180, 29.2720, // Tuzla Yarımadası Kıstak
    40.8145, 29.2605, // Tuzla DHO Batı
    40.8068, 29.2580, // Tuzla Burnu (Deniz Harp Okulu Ucu)
    40.8065, 29.2660, // Tuzla Burnu Güney
    40.8105, 29.2850, // Tuzla Mercan
    40.8055, 29.3030, // Tuzla Viaport Marina Ucu
    40.8115, 29.3140, // Tuzla Doğu Koyu
    40.7850, 29.3270, // Bayramoğlu Yarımadası Burnu
    40.7820, 29.3360, // Bayramoğlu Doğu
    40.7570, 29.3545, // Darıca Yelkenkaya Burnu
    40.7640, 29.3920, // Darıca Doğu
    40.7680, 29.4190, // Eskihisar
    40.7690, 29.4680, // Tavşancıl
    40.7642, 29.5148, // Dilburnu (Osmangazi Kuzey Burnu)
    40.7685, 29.5350, // Diliskelesi
    40.7820, 29.6200, // Hereke
    40.7680, 29.7450, // Körfez
    40.7580, 29.7580, // Tüpraş / Yarımca Burnu
    40.7530, 29.8320, // Derince
    40.7625, 29.9450, // İzmit Körfez Sonu Kuzeydoğu
    // İzmit Körfezi Güney Kıyısı & Armutlu Yarımadası & Gemlik Körfezi
    40.7160, 29.9350, // Başiskele Güneydoğu
    40.7165, 29.8380, // Gölcük Kavaklı
    40.7150, 29.8150, // Gölcük
    40.7180, 29.7800, // Değirmendere
    40.7215, 29.7420, // Halıdere / Gonca Burnu
    40.7000, 29.6850, // Ulaşlı / Ereğli
    40.6925, 29.6160, // Karamürsel
    40.7010, 29.5250, // Altınova / Kaytazdere
    40.7425, 29.5150, // Hersek Burnu (Osmangazi Güney Burnu)
    40.7180, 29.4900, // Hersek Batı
    40.7050, 29.4020, // Topçular
    40.6880, 29.3650, // Taşköprü
    40.6580, 29.2700, // Yalova
    40.6520, 29.1550, // Koru / Çınarcık Doğu
    40.6425, 29.1200, // Çınarcık
    40.6180, 28.9500, // Esenköy
    40.5345, 28.7820, // Bozburun (Armutlu Yarımadası Batı Ucu)
    40.5195, 28.8260, // Armutlu İskele Kıyısı
    40.4820, 28.9200, // Fıstıklı / Kapaklı
    40.4780, 29.0450, // Narlı
    40.4250, 29.1560, // Gemlik İç Körfez
    40.3780, 29.0420, // Kurşunlu
    40.3690, 28.9060, // Güzelyalı (Bursa)
    40.3820, 28.8840, // Mudanya
    40.3940, 28.7950, // Trilye / Zeytinbağı
    40.3960, 28.4200, // Karacabey Boğazı
    // Bandırma & Kapıdağ Yarımadası & Erdek & Güney Marmara
    40.3650, 28.0200, // Bandırma Doğu
    40.3540, 27.9720, // Bandırma İç
    40.3880, 27.8950, // Belkıs Kıstağı Doğu (Tatlısu)
    40.4150, 27.9450, // Aşağıyapıcı
    40.4600, 28.0150, // Kurşunlu / Çakılköy (Kapıdağ Doğu Ucu)
    40.5180, 27.9720, // Kapsül Burnu (Kapıdağ Kuzeydoğu)
    40.5300, 27.8650, // Kestanelik / Çayağzı (Kapıdağ Kuzey)
    40.5200, 27.7750, // Ormanlı / Ballıpınar (Kapıdağ Kuzeybatı)
    40.4680, 27.6760, // Doğanlar / İlhanlar (Kapıdağ Batı Ucu)
    40.4350, 27.7200, // Ocaklar
    40.3990, 27.7880, // Erdek
    40.3840, 27.8680, // Belkıs Kıstağı Batı (Düzler)
    40.3150, 27.6400, // Gönen Çayı Deltası
    40.4050, 27.3080, // Karabiga Burnu
    40.4400, 27.1000, // Kemer / İçdaş Burnu
    40.3760, 26.7160, // Çardak Burnu
    // Çanakkale Boğazı Anadolu Kıyısı (Kuzeyden Güneye)
    40.3440, 26.6820, // Lapseki
    40.2750, 26.5820, // Çamburnu / Umurbey
    40.2150, 26.4720, // Yapıldak
    40.1965, 26.3988, // Nara Burnu (Keskin Dönüş Doğu Kıyısı)
    40.1840, 26.4045, // Nara Askeri Liman İçi
    40.1465, 26.3995, // Çanakkale Çimenlik Kalesi (En Dar Geçit Doğu Kıyısı)
    40.0965, 26.3685, // Kepez Burnu
    40.0250, 26.3250, // Erenköy / Karanfil
    40.0075, 26.2020, // Kumkale Burnu
    // Ege Denizi Anadolu Kıyısı (Kuzeyden Güneye)
    39.7950, 26.1550, // Geyikli / Odunluk
    39.4780, 26.0635, // Babakale Burnu (Anadolu En Batı Ucu)
    39.4880, 26.3380, // Assos / Behramkale
    39.5480, 26.6100, // Küçükkuyu
    39.5880, 26.9320, // Akçay / Edremit İç Körfez
    39.5020, 26.9700, // Burhaniye
    39.3200, 26.6950, // Ayvalık
    39.2650, 26.6200, // Sarımsaklı / Badavut Burnu
    39.0700, 26.8880, // Dikili
    38.9350, 27.0200, // Çandarlı İç
    38.9280, 26.9350, // Çandarlı Burnu
    38.8650, 26.8820, // Aliağa Karaağaç / Teke Burnu
    38.8280, 26.9680, // Aliağa / Nemrut İç
    38.7450, 26.8380, // Yeni Foça
    38.6700, 26.7520, // Eski Foça
    38.5350, 26.8220, // Gediz Deltası / Kuş Cenneti
    38.4580, 27.1100, // Karşıyaka
    38.4520, 27.1680, // İzmir Bayraklı İç Körfez Sonu
    38.4250, 27.1320, // Alsancak / Konak
    38.4090, 27.0320, // Yenikale / İnciraltı
    38.3720, 26.7850, // Güzelbahçe / Urla İskele
    38.3950, 26.7480, // Urla Çeşmealtı Burnu
    38.3380, 26.6550, // Gülbahçe Koyu İç
    // Karaburun & Çeşme Yarımadası
    38.5150, 26.6280, // Mordoğan
    38.6520, 26.5180, // Karaburun Kuzey
    38.6650, 26.4200, // Küçükbahçe / Kömür Burnu
    38.6580, 26.3640, // Sarpıncık Feneri (Karaburun Batı Ucu)
    38.4800, 26.3950, // Gerence
    38.3950, 26.4550, // Ildır Koyu
    38.3520, 26.2880, // Çeşme Kuzey / Dalyan
    38.3200, 26.3020, // Çeşme
    38.2250, 26.2480, // Çeşme Teke Burnu (Güneybatı Uç)
    38.2450, 26.3850, // Alaçatı Liman
    38.1920, 26.7820, // Sığacık
    38.0150, 26.8820, // Doğanbey Burnu
    38.0050, 27.1800, // Pamucak / Selçuk Sahil
    37.8620, 27.2580, // Kuşadası
    // Dilek Yarımadası (Samsun Dağı)
    37.7100, 27.2200, // Güzelçamlı
    37.6845, 27.0245, // Dipburun (Dilek Boğazı Doğu Kıyısı)
    37.6280, 27.1150, // Karina / Doğanbey
    37.5000, 27.2200, // Büyük Menderes Deltası
    37.3480, 27.1950, // Didim Tekağaç Burnu
    37.3340, 27.2600, // Altınkum / Didim
    37.3800, 27.4400, // Akbük İç
    37.2460, 27.6050, // Güllük İç
    // Bodrum Yarımadası & Gökova Körfezi
    37.1450, 27.5600, // Güvercinlik
    37.1480, 27.3500, // Türkbükü / Gündoğan
    37.1120, 27.2620, // Yalıkavak
    37.0550, 27.2280, // Gümüşlük
    37.0020, 27.2520, // Turgutreis
    36.9655, 27.2640, // Hüseyin Burnu (Bodrum Güneybatı Ucu)
    36.9680, 27.3200, // Akyarlar / Karaincir
    37.0280, 27.4320, // Bodrum İç
    37.0050, 27.7800, // Çökertme
    37.0250, 27.9750, // Ören
    37.0480, 28.3280, // Akyaka (Gökova Körfezi Sonu)
    // Datça Yarımadası & Hisarönü & Bozburun Yarımadası
    36.8500, 28.0500, // Gökova Güney Kıyısı
    36.7900, 27.6200, // Körmen (Datça Kuzey)
    36.7300, 27.4200, // Tekir / Mersincik
    36.6855, 27.3680, // Deveboynu / Knidos Burnu (Datça En Batı Ucu)
    36.6680, 27.5050, // Palamutbükü
    36.7180, 27.6900, // Datça Liman Kıyısı
    36.7880, 28.1250, // Hisarönü İç Körfez
    36.6880, 28.0400, // Bozburun
    36.5680, 28.0140, // Bozukkale / Karaburun (Bozburun Yarımadası Güney Ucu)
    36.6300, 28.1400, // Çiftlik / Serçe
    36.7285, 28.3025, // Kadırga Burnu
    36.8550, 28.2700, // Marmaris İç Körfez
    36.8420, 28.4050, // Aksaz İç Körfez
    36.7850, 28.6250, // Ekincik / Dalyan İztuzu
    36.6320, 28.8680, // Göcek Kapıdağ Yarımadası Burnu (Körfez Batı Girişi)
    36.7580, 28.9450, // Göcek İç Körfez (Kuzey)
    36.6850, 29.0450, // Günlüklü / Çalış Kuzey
    36.6200, 29.1180, // Fethiye İç Körfez
    36.5150, 29.0200, // İblis Burnu / Ölüdeniz
    // Akdeniz Anadolu Kıyısı (Batıdan Doğuya)
    36.3420, 29.1150, // Yedi Burunlar
    36.2550, 29.3150, // Patara
    36.2660, 29.4160, // Kalkan
    36.1935, 29.6055, // Kaş Çukurbağ Yarımadası Ucu
    36.1980, 29.6420, // Kaş İç
    36.1940, 29.8650, // Kaleköy / Üçağız Kıyısı
    36.2180, 30.0050, // Demre / Andriake
    36.2980, 30.1550, // Finike
    36.2750, 30.3000, // Kumluca Sahil
    36.2120, 30.4050, // Gelidonya / Taşlık Burnu
    36.3850, 30.4820, // Adrasan / Çıralı
    36.6020, 30.5680, // Kemer
    36.8400, 30.6180, // Antalya Liman Kıyısı
    36.8850, 30.7200, // Antalya Lara
    36.7800, 31.4350, // Side / Manavgat
    36.6150, 31.7500, // İncekum
    36.5280, 31.9880, // Alanya Kalesi Yarımadası
    36.2550, 32.2800, // Gazipaşa
    36.0220, 32.8050, // Anamur Burnu (Anadolu En Güney Ucu)
    36.1420, 33.3250, // Aydıncık
    36.1350, 33.6800, // Ovacık / Boğsak Yarımadası
    36.3180, 33.8850, // Taşucu İç
    36.2380, 33.9550, // İncekum Burnu (Silifke/Göksu Deltası)
    36.6060, 34.3120, // Erdemli
    36.7950, 34.6350, // Mersin İç
    36.5380, 35.3520, // Karataş Burnu
    36.7700, 35.7920, // Yumurtalık
    36.9150, 36.0200, // Ceyhan / İskenderun Körfezi Kuzey Ucu
    36.8460, 36.1850, // Dörtyol
    36.5980, 36.1820, // İskenderun
    36.4160, 35.8850, // Arsuz
    36.3250, 35.7820, // Akıncı (Hınzır) Burnu
    36.0600, 35.9500, // Samandağ / Asi Nehri Ağzı
    35.5200, 35.7800, // Lazkiye
    34.5000, 35.9500, // Doğu Akdeniz Kıyısı
    34.5000, 42.5000  // Güneydoğu Dış Kapanış
  )

  // 3. MARMARA, EGE, AKDENİZ VE KARADENİZ ADALARI (KAPALI POLİGONLAR)
  private val islands: List<LandPolygon> = listOf(
    // --- İstanbul Prens Adaları & Tuzla Adaları ---
    poly("BUYUKADA", true,
      40.8730, 29.1240, 40.8710, 29.1360, 40.8580, 29.1320,
      40.8440, 29.1180, 40.8520, 29.1110, 40.8680, 29.1150
    ),
    poly("HEYBELIADA", true,
      40.8830, 29.0920, 40.8790, 29.1040, 40.8660, 29.0980,
      40.8670, 29.0830, 40.8780, 29.0820
    ),
    poly("BURGAZADA", true,
      40.8870, 29.0620, 40.8820, 29.0710, 40.8740, 29.0650,
      40.8760, 29.0540, 40.8840, 29.0550
    ),
    poly("KINALIADA", true,
      40.9150, 29.0490, 40.9110, 29.0570, 40.9035, 29.0530,
      40.9050, 29.0430, 40.9120, 29.0430
    ),
    poly("SEDEF_ADASI", true,
      40.8540, 29.1420, 40.8510, 29.1480, 40.8450, 29.1440, 40.8480, 29.1380
    ),
    poly("YASSIADA", true,
      40.8675, 28.9940, 40.8650, 28.9980, 40.8620, 28.9940, 40.8645, 28.9905
    ),
    poly("SIVRIADA", true,
      40.8775, 28.9730, 40.8750, 28.9765, 40.8725, 28.9730, 40.8750, 28.9695
    ),
    poly("TUZLA_INCIR_ADASI", true,
      40.7990, 29.2740, 40.7965, 29.2790, 40.7925, 29.2760, 40.7950, 29.2715
    ),

    // --- Güney Marmara Adaları ---
    poly("IMRALI_ADASI", true,
      40.5680, 28.5320, 40.5520, 28.5480, 40.5180, 28.5380,
      40.5220, 28.5160, 40.5550, 28.5180
    ),
    poly("MARMARA_ADASI", true,
      40.6650, 27.6200, 40.6520, 27.7120, 40.6050, 27.7050,
      40.5840, 27.5600, 40.5920, 27.5150, 40.6380, 27.5250
    ),
    poly("AVSA_ADASI", true,
      40.5320, 27.4980, 40.5150, 27.5240, 40.4820, 27.5120,
      40.4860, 27.4760, 40.5180, 27.4780
    ),
    poly("PASALIMANI_ADASI", true,
      40.5080, 27.6050, 40.4920, 27.6520, 40.4560, 27.6380,
      40.4620, 27.5720, 40.4920, 27.5680
    ),
    poly("EKINLIK_ADASI", true,
      40.5560, 27.4880, 40.5500, 27.5030, 40.5420, 27.4880, 40.5480, 27.4720
    ),
    poly("KOYUN_ADASI", true,
      40.5180, 27.5800, 40.5100, 27.5900, 40.5020, 27.5800, 40.5100, 27.5700
    ),

    // --- Kuzey & Orta Ege Adaları ---
    poly("GOKCEADA", true,
      40.2420, 25.8950, 40.2150, 26.0120, 40.1450, 25.9500,
      40.1050, 25.8000, 40.1250, 25.6680, 40.1950, 25.7350
    ),
    poly("BOZCAADA", true,
      39.8510, 26.0150, 39.8360, 26.0740, 39.7980, 26.0580,
      39.8050, 25.9680, 39.8380, 25.9650
    ),
    poly("TAVSAN_ADASI_CANAKKALE", true,
      39.9420, 26.0650, 39.9350, 26.0760, 39.9270, 26.0650, 39.9340, 26.0560
    ),
    poly("SEMADIREK", true,
      40.5150, 25.5600, 40.4650, 25.7050, 40.3980, 25.6200,
      40.4250, 25.4750, 40.4950, 25.4850
    ),
    poly("TASOZ", true,
      40.7950, 24.7100, 40.6800, 24.7850, 40.5650, 24.6600,
      40.6150, 24.5150, 40.7550, 24.5450
    ),
    poly("LIMNI", true,
      40.0250, 25.3600, 39.9200, 25.4450, 39.7850, 25.2800,
      39.8350, 25.0400, 39.9850, 25.1200
    ),
    poly("MIDILLI_LESBOS", true,
      39.3850, 26.3350, 39.2950, 26.4450, 39.1050, 26.5650,
      38.9680, 26.5450, 38.9950, 26.3200, 39.1550, 25.8350,
      39.2850, 25.9200, 39.3750, 26.1750
    ),
    poly("CUNDA_ALIBEY_ADASI", true,
      39.3800, 26.6450, 39.3550, 26.6880, 39.3280, 26.6620,
      39.3350, 26.6150, 39.3650, 26.6180
    ),
    poly("UZUNADA_IZMIR", true,
      38.5460, 26.7120, 38.5150, 26.7450, 38.4700, 26.7320,
      38.4750, 26.6960, 38.5250, 26.6950
    ),
    poly("HEKIM_ADASI_IZMIR", true,
      38.4560, 26.7650, 38.4480, 26.7740, 38.4400, 26.7650, 38.4480, 26.7560
    ),
    poly("SAKIZ_CHIOS", true,
      38.5950, 26.0600, 38.5100, 26.1550, 38.3650, 26.1480,
      38.1550, 26.0150, 38.2050, 25.8550, 38.4850, 25.8650
    ),
    poly("KOYUN_ADALARI_OINOUSSES", true,
      38.5400, 26.2200, 38.5150, 26.3050, 38.4980, 26.2650, 38.5180, 26.1950
    ),
    poly("IPSARA", true,
      38.6150, 25.5850, 38.5750, 25.6250, 38.5320, 25.5750, 38.5750, 25.5450
    ),
    poly("SISAM_SAMOS", true,
      37.8050, 26.6800, 37.7680, 26.9850, 37.7040, 27.0110,
      37.6850, 26.9400, 37.6450, 26.8200, 37.7050, 26.5680
    ),
    poly("AHIKERYA_IKARIA", true,
      37.6850, 26.3650, 37.6050, 26.3200, 37.5050, 26.0150, 37.5850, 25.9850
    ),

    // --- Güney Ege & Oniki Adalar ---
    poly("BATNOZ_PATMOS", true,
      37.3850, 26.5550, 37.3200, 26.5850, 37.2750, 26.5450, 37.3350, 26.5150
    ),
    poly("ILERIYE_LEROS", true,
      37.2150, 26.8350, 37.1450, 26.8850, 37.0950, 26.8550, 37.1550, 26.7950
    ),
    poly("KILIMLI_KALYMNOS", true,
      37.0850, 26.9550, 37.0150, 27.0550, 36.9350, 27.0150,
      36.9550, 26.9150, 37.0350, 26.9050
    ),
    poly("ISTANKOY_KOS", true,
      36.8980, 27.2850, 36.8860, 27.3320, 36.8250, 27.2550,
      36.7550, 27.0550, 36.6780, 26.9450, 36.7450, 26.9250, 36.8550, 27.1250
    ),
    poly("KARAADA_BODRUM", true,
      36.9840, 27.4520, 36.9680, 27.4920, 36.9560, 27.4750, 36.9720, 27.4380
    ),
    poly("INCIRLI_NISYROS", true,
      36.6180, 27.1750, 36.5850, 27.2120, 36.5520, 27.1750, 36.5850, 27.1350
    ),
    poly("ILYAKI_TILOS", true,
      36.4820, 27.3450, 36.4250, 27.4280, 36.3850, 27.3850, 36.4450, 27.3020
    ),
    poly("SOMBEKI_SYMI", true,
      36.6520, 27.8350, 36.5950, 27.8880, 36.5220, 27.8450,
      36.5450, 27.7780, 36.6250, 27.7850
    ),
    poly("RODOS_RHODES", true,
      36.4520, 28.2220, 36.2850, 28.1850, 36.0550, 28.0850,
      35.8850, 27.7550, 36.0850, 27.7150, 36.3450, 27.9550
    ),
    poly("TERSANE_ADASI_GOCEK", true,
      36.6780, 28.9180, 36.6650, 28.9320, 36.6520, 28.9180, 36.6650, 28.9040
    ),
    poly("DOMUZ_ADASI_GOCEK", true,
      36.6720, 28.8920, 36.6620, 28.9020, 36.6540, 28.8880, 36.6640, 28.8800
    ),
    poly("SOVALYE_ADASI_FETHIYE", true,
      36.6610, 29.0980, 36.6550, 29.1040, 36.6480, 29.0960, 36.6540, 29.0910
    ),

    // --- Akdeniz, Kıbrıs, Girit ve Karadeniz Adaları ---
    poly("MEIS_KASTELLORIZO", true,
      36.1560, 29.5950, 36.1420, 29.6060, 36.1260, 29.5850, 36.1420, 29.5680
    ),
    poly("KEKOVA_ADASI", true,
      36.1850, 29.8950, 36.1780, 29.9120, 36.1600, 29.8650,
      36.1650, 29.8450, 36.1780, 29.8680
    ),
    poly("SULUADA_ADRASAN", true,
      36.2450, 30.4720, 36.2360, 30.4800, 36.2280, 30.4720, 36.2360, 30.4650
    ),
    poly("KIBRIS_CYPRUS", true,
      35.4020, 32.9220, // Koruçam Burnu
      35.3350, 33.3280, // Girne Kıyısı
      35.3950, 33.9000, // Kantara / Bafra
      35.6950, 34.5880, // Zafer Burnu (Karpaz Ucu)
      35.5200, 34.3500, // Karpaz Güney
      35.3000, 33.9800, // Boğaz / İskele
      35.1200, 33.9400, // Gazimağusa Kıyısı
      34.9650, 34.0850, // Greco Burnu
      34.8800, 33.6400, // Larnaka
      34.5650, 33.0200, // Ağrotur / Limasol
      34.7500, 32.4000, // Baf
      35.0950, 32.2750, // Arnavut Burnu (Akamas)
      35.1800, 32.6500  // Güzelyurt Körfezi
    ),
    poly("GIRIT_CRETE", true,
      35.6500, 23.5800, 35.5200, 24.1800, 35.3800, 25.1500,
      35.3200, 26.3100, 35.0100, 26.2200, 34.9200, 24.7500, 35.2200, 23.5400
    ),
    poly("KERPE_KARPATHOS", true,
      35.8900, 27.2400, 35.6200, 27.2200, 35.3900, 27.1200, 35.5800, 27.0500
    ),
    poly("EGRIBOZ_EUBOEA", true,
      39.0200, 23.1200, 38.6500, 24.1500, 37.9500, 24.5800,
      38.0500, 24.2200, 38.4500, 23.5800, 38.8500, 22.8500
    ),
    poly("NAXOS_PAROS_CYCLADES", true,
      37.2500, 25.5500, 36.9500, 25.6200, 36.9200, 25.1200, 37.2200, 25.1000
    ),
    poly("KEFKEN_ADASI", true,
      41.2195, 30.2590, 41.2165, 30.2650, 41.2130, 30.2600, 41.2160, 30.2550
    ),
    poly("GIRESUN_ADASI", true,
      40.9315, 38.4360, 40.9290, 38.4395, 40.9265, 38.4360, 40.9290, 38.4325
    )
  )

  val allPolygons: List<LandPolygon> = listOf(thraceAndGreeceMainland, anatoliaMainland) + islands
}
