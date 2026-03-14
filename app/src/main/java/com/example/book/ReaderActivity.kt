package com.example.book

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.shared.publication.Publication

@AndroidEntryPoint
class ReaderActivity : AppCompatActivity() {

    private val viewModel: ReaderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader)

        // 1. Full-screen experience
        enableImmersiveMode()

        // 2. Modern way to get the Uri (Fixes the deprecation warning)
        val bookUri = IntentCompat.getParcelableExtra(intent, "BOOK_URI", Uri::class.java)

        if (bookUri != null) {
            // If we came from a file picker
            viewModel.openPublication(bookUri)
        } else {
            // Default: Load the converted Calibre book from assets
            viewModel.loadAssetBook("rtl_book.epub")
        }

        // 3. Observe the UI state to show the book
        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    when (state) {
                        is ReaderUiState.Idle -> {
                            // Do nothing, just waiting for the user to pick a book
                        }
                        is ReaderUiState.Loading -> {
                            // Show progress bar
                        }
                        is ReaderUiState.Success -> {
                            if (supportFragmentManager.findFragmentById(R.id.reader_container) == null) {
                                setupNavigator(state.publication)
                            }
                        }
                        is ReaderUiState.Error -> {
                            Toast.makeText(this@ReaderActivity, state.message, Toast.LENGTH_LONG).show()
                        }

                    }
                }
            }
        }
    }

    private fun setupNavigator(publication: Publication) {
        val navigatorFactory = EpubNavigatorFactory(publication)

        // Start at the first item in the Table of Contents
        val initialLocator = publication.tableOfContents.firstOrNull()?.let { link ->
            publication.locatorFromLink(link)
        }

        // Create the Fragment via Readium's Factory
        val fragmentFactory = navigatorFactory.createFragmentFactory(
            initialLocator = initialLocator,
            configuration = EpubNavigatorFragment.Configuration()
        )

        supportFragmentManager.fragmentFactory = fragmentFactory

        val navigatorFragment = supportFragmentManager.fragmentFactory.instantiate(
            classLoader,
            EpubNavigatorFragment::class.java.name
        ) as EpubNavigatorFragment

        // Transaction to add the Navigator to your layout
        supportFragmentManager.beginTransaction()
            .replace(R.id.reader_container, navigatorFragment)
            .commit()
    }

    private fun enableImmersiveMode() {
        val windowInsetsController = ViewCompat.getWindowInsetsController(window.decorView) ?: return
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
}