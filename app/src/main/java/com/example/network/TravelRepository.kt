package com.example.network

import android.content.Context
import android.util.Log
import com.example.db.AppDatabase
import com.example.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipInputStream

class TravelRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val dao = db.travelDao()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder().build()

    val allLocations: Flow<List<TravelLocation>> = dao.getAllLocations().map { entities ->
        entities.map { entity ->
            val resimler = try {
                moshi.adapter<List<String>>(com.squareup.moshi.Types.newParameterizedType(List::class.java, String::class.java))
                    .fromJson(entity.resimler) ?: emptyList()
            } catch (e: Exception) { emptyList() }
            
            val etiketler = try {
                moshi.adapter<List<String>>(com.squareup.moshi.Types.newParameterizedType(List::class.java, String::class.java))
                    .fromJson(entity.etiketler) ?: emptyList()
            } catch (e: Exception) { emptyList() }
            
            val yorumlar = try {
                moshi.adapter<List<YorumItemJson>>(com.squareup.moshi.Types.newParameterizedType(List::class.java, YorumItemJson::class.java))
                    .fromJson(entity.yorumlar) ?: emptyList()
            } catch (e: Exception) { emptyList() }

            TravelLocation(
                id = entity.id,
                baslik = entity.baslik,
                sehir = entity.sehir,
                kategori = entity.kategori,
                aciklama = entity.aciklama,
                resimler = resimler,
                etiketler = etiketler,
                ortalamaPuan = entity.ortalamaPuan,
                yorumlar = yorumlar
            )
        }
    }

    suspend fun syncData(onProgress: (String) -> Unit = {}) {
        withContext(Dispatchers.IO) {
            try {
                onProgress("1. Adım: Veriler indiriliyor...")
                val destDir = File(context.filesDir, "travel_data")
                
                val zipFile = File(context.cacheDir, "data.zip")
                
                downloadZip(zipFile)
                
                onProgress("2. Adım: ZIP dosyası çıkartılıyor...")
                extractZip(zipFile, destDir)
                zipFile.delete()

                onProgress("3. Adım: JSON ve resimler bulunup entegre ediliyor...")
                val allFiles = mutableMapOf<String, File>()
                fun buildFileMap(root: File) {
                    if (root.isDirectory) {
                        root.listFiles()?.forEach { buildFileMap(it) }
                    } else {
                        allFiles[root.name] = root
                    }
                }
                buildFileMap(destDir)

                val resimFile = allFiles["resim.json"] ?: throw Exception("resim.json dosyası ZIP içinde bulunamadı.")
                val yorumFile = allFiles["yorum.json"] ?: throw Exception("yorum.json dosyası ZIP içinde bulunamadı.")

                val resimJsonString = resimFile.readText()
                val yorumJsonString = yorumFile.readText()

                val resimResponse = moshi.adapter(ResimResponse::class.java).fromJson(resimJsonString)
                val yorumResponse = moshi.adapter(YorumResponse::class.java).fromJson(yorumJsonString)

                val resimList = resimResponse?.lokasyonlar ?: emptyList()
                val yorumList = yorumResponse?.yorumlar ?: emptyList()

                onProgress("Doğrulama yapılıyor...")
                if (resimList.isEmpty()) {
                    throw Exception("Geçerli veri bulunamadı, sunucu JSON dosyası boş.")
                }

                var validItemCount = 0

                val entities = resimList.mapNotNull { resim ->
                    try {
                        val yorum = yorumList.find { it.id == resim.id }

                        val resimlerUrls = resim.resimler?.mapNotNull { imageName ->
                            allFiles[imageName]?.absolutePath?.let { "file://$it" }
                        } ?: emptyList()
                        
                        val resimlerStr = moshi.adapter<List<String>>(
                            com.squareup.moshi.Types.newParameterizedType(List::class.java, String::class.java)
                        ).toJson(resimlerUrls)
                        
                        val etiketlerStr = moshi.adapter<List<String>>(
                            com.squareup.moshi.Types.newParameterizedType(List::class.java, String::class.java)
                        ).toJson(resim.etiketler ?: emptyList())

                        val yorumStr = moshi.adapter<List<YorumItemJson>>(
                            com.squareup.moshi.Types.newParameterizedType(List::class.java, YorumItemJson::class.java)
                        ).toJson(yorum?.yorumListesi ?: emptyList())

                        validItemCount++
                        TravelLocationEntity(
                            id = resim.id,
                            baslik = resim.baslik ?: "Bilinmeyen Başlık",
                            sehir = resim.sehir ?: "",
                            kategori = resim.kategori ?: "",
                            aciklama = resim.aciklama ?: "",
                            resimler = resimlerStr,
                            etiketler = etiketlerStr,
                            ortalamaPuan = yorum?.ortalamaPuan ?: 0.0,
                            yorumlar = yorumStr
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                if (validItemCount == 0) {
                    throw Exception("Tüm veriler bozuk, geçerli lokasyon bulunamadı.")
                }

                onProgress("4. Adım: Kontrol ediliyor ve kaydediliyor...")
                dao.clearAll()
                dao.insertAll(entities)
                
                onProgress("Tüm adımlar başarıyla tamamlandı")
                
            } catch (e: Exception) {
                Log.e("TravelRepository", "Sync failed", e)
                throw e
            }
        }
    }

    private fun downloadZip(destFile: File) {
        val zipUrl = "https://codeload.github.com/Lenon-studio-Develpore/Mobile-App-Json-/zip/refs/heads/main"
        val request = Request.Builder().url(zipUrl).build()
        
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("Bağlantı hatası: ${response.code}")
        
        val body = response.body ?: throw IOException("Boş yanıt alındı")
        
        FileOutputStream(destFile).use { fos ->
            body.byteStream().use { input ->
                input.copyTo(fos)
            }
        }
    }
    
    private fun extractZip(zipFile: File, destDir: File) {
        if (destDir.exists()) {
            destDir.deleteRecursively()
        }
        destDir.mkdirs()
        
        java.io.FileInputStream(zipFile).use { fis ->
            ZipInputStream(fis).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val file = File(destDir, entry.name)
                    val canonicalDestDir = destDir.canonicalPath
                    val canonicalFile = file.canonicalPath
                    if (!canonicalFile.startsWith(canonicalDestDir + File.separator)) {
                        entry = zis.nextEntry
                        continue
                    }

                    if (entry.isDirectory) {
                        file.mkdirs()
                    } else {
                        file.parentFile?.mkdirs()
                        FileOutputStream(file).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    entry = zis.nextEntry
                }
            }
        }
    }
}

