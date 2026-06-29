package com.example.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ResimResponse(
    @Json(name = "lokasyonlar") val lokasyonlar: List<ResimJson>
)

@JsonClass(generateAdapter = true)
data class YorumResponse(
    @Json(name = "yorumlar") val yorumlar: List<YorumListJson>
)

@JsonClass(generateAdapter = true)
data class ResimJson(
    @Json(name = "id") val id: Int,
    @Json(name = "baslik") val baslik: String?,
    @Json(name = "sehir") val sehir: String?,
    @Json(name = "kategori") val kategori: String?,
    @Json(name = "aciklama") val aciklama: String?,
    @Json(name = "resimler") val resimler: List<String>?,
    @Json(name = "etiketler") val etiketler: List<String>?
)

@JsonClass(generateAdapter = true)
data class YorumListJson(
    @Json(name = "id") val id: Int,
    @Json(name = "ortalama_puan") val ortalamaPuan: Double?,
    @Json(name = "yorum_listesi") val yorumListesi: List<YorumItemJson>?
)

@JsonClass(generateAdapter = true)
data class YorumItemJson(
    @Json(name = "kullanici") val kullanici: String?,
    @Json(name = "puan") val puan: Int?,
    @Json(name = "tarih") val tarih: String?,
    @Json(name = "yorum") val yorum: String?
)
