package com.example.book.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.shared.util.http.HttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import org.readium.r2.streamer.parser.epub.EpubParser
import org.readium.r2.streamer.parser.pdf.PdfParser
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
@Suppress("unused")
object AppModule {

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient = DefaultHttpClient()

    @Provides
    @Singleton
    fun provideAssetRetriever(
        @ApplicationContext context: Context,
        httpClient: HttpClient
    ): AssetRetriever {
        // We use context.contentResolver to satisfy the constructor:
        // AssetRetriever(ContentResolver, HttpClient)
        return AssetRetriever(
            contentResolver = context.contentResolver,
            httpClient = httpClient
        )
    }

    @Provides
    @Singleton
    fun providePublicationOpener(
        @ApplicationContext context: Context,
        httpClient: HttpClient,
        assetRetriever: AssetRetriever
    ): PublicationOpener {

        val parser = DefaultPublicationParser(
            context,
            httpClient,
            assetRetriever,
            // For pdfFactory: If you aren't using PDF yet, you can pass null
            // if the library allows, or a default factory if you have the PDF dependency.
            pdfFactory = null,
            // For additionalParsers: Pass an empty list
            additionalParsers = emptyList()
        )

        return PublicationOpener(parser)
    }}