package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations")
data class TravelLocationEntity(
    @PrimaryKey val id: Int,
    val baslik: String,
    val sehir: String,
    val kategori: String,
    val aciklama: String,
    val resimler: String, // JSON list of URLs
    val etiketler: String, // JSON list of strings
    val ortalamaPuan: Double,
    val yorumlar: String // JSON list of YorumItemJson
)
