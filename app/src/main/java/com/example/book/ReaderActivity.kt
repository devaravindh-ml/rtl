package com.example.book

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
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

        val bookUri = IntentCompat.getParcelableExtra(intent, "BOOK_URI", Uri::class.java)
        if (bookUri != null) {
            viewModel.openPublication(bookUri)
        } else {
            viewModel.loadAssetBook("rtl_book.epub")
        }

        observeViewModel()
        
    }

    private fun observeViewModel() {
        val progressBar = findViewById<ProgressBar>(R.id.loading_progress)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    when (state) {
                        is ReaderUiState.Loading -> {
                            progressBar?.visibility = View.VISIBLE
                        }
                        is ReaderUiState.Success -> {
                            progressBar?.visibility = View.GONE
                            if (supportFragmentManager.findFragmentByTag("navigator") == null) {
                                setupNavigator(state.publication)
                            }
                        }
                        is ReaderUiState.Error -> {
                            progressBar?.visibility = View.GONE
                            Toast.makeText(this@ReaderActivity, state.message, Toast.LENGTH_LONG).show()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun setupNavigator(publication: Publication) {
        val navigatorFactory = EpubNavigatorFactory(publication)

        val initialLocator = publication.tableOfContents.firstOrNull()?.let {
            publication.locatorFromLink(it)
        }

        val fragmentFactory = navigatorFactory.createFragmentFactory(
            initialLocator = initialLocator,
            configuration = EpubNavigatorFragment.Configuration()
        )

        supportFragmentManager.fragmentFactory = fragmentFactory

        val navigatorFragment = supportFragmentManager.fragmentFactory.instantiate(
            classLoader,
            EpubNavigatorFragment::class.java.name
        )

        supportFragmentManager.beginTransaction()
            .replace(R.id.reader_container, navigatorFragment, "navigator")
            .commitNow()
    }
}