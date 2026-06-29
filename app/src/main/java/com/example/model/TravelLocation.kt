package com.example.model

data class TravelLocation(
    val id: Int,
    val baslik: String,
    val sehir: String,
    val kategori: String,
    val aciklama: String,
    val resimler: List<String>,
    val etiketler: List<String>,
    val ortalamaPuan: Double,
    val yorumlar: List<YorumItemJson>
)
