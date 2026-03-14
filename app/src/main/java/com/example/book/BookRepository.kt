package com.example.book

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.streamer.PublicationOpener
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
        val assetUrl = AbsoluteUrl("asset:///$fileName")!!
        openPublication(assetUrl)
    }

    // --- 2. For URIs (This fixes the 'openEpub' error) ---
    suspend fun openEpub(uri: Uri): Publication = withContext(Dispatchers.IO) {
        // Converts the Android Uri to a Readium AbsoluteUrl
        val assetUrl = AbsoluteUrl(uri.toString())!!
        openPublication(assetUrl)
    }

    // Internal helper to handle the common opening logic
    private suspend fun openPublication(url: AbsoluteUrl): Publication {
        val assetTry = assetRetriever.retrieve(url)
        val asset = assetTry.getOrNull()
            ?: throw Exception("Failed to retrieve asset from: $url")

        val pubTry = publicationOpener.open(asset, allowUserInteraction = false)
        return pubTry.getOrNull()
            ?: throw Exception("Failed to parse publication")
    }
}