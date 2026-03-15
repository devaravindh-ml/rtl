package com.example.book

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.asset.Asset
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.streamer.PublicationOpener
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val assetRetriever: AssetRetriever,
    private val publicationOpener: PublicationOpener
) {

    // --- 1. For your Asset Book (rtl_book.epub) ---
    suspend fun openBookFromAssets(fileName: String): Publication = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, fileName)

        context.assets.open(fileName).use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        // Explicitly handle the Try result
        val assetResult = assetRetriever.retrieve(tempFile)

        val asset = assetResult.getOrElse {
            throw Exception("Retrieve failed: $it")
        }

        openPublication(asset)
    }
    // --- 2. For URIs (File Picker) ---
    suspend fun openEpub(uri: Uri): Publication = withContext(Dispatchers.IO) {
        // Convert the Android Uri (content://...) to a Readium AbsoluteUrl
        val url = AbsoluteUrl(uri.toString())
            ?: throw Exception("Invalid URI: $uri")

        val asset = assetRetriever.retrieve(url)
            .getOrElse { throw Exception("Failed to retrieve URI: $it") }

        openPublication(asset)
    }

    private suspend fun openPublication(asset: Asset): Publication {
        return publicationOpener.open(asset, allowUserInteraction = false)
            .getOrElse { throw Exception("Failed to parse publication: $it") }
    }
}