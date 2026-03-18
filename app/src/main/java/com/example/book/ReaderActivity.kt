package com.example.book

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.book.database.BookChunk
import com.example.book.database.ObjectBox
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.LocatorCollection
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.epub.landmarks
import org.readium.r2.shared.publication.services.search.search

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

        // Capture the chunk ID from intent
        val chunkId = intent.getLongExtra("CHUNK_ID", -1L)

        // Pass the ID to the observer logic
        observeViewModel(chunkId)
    }

    private fun observeViewModel(pendingChunkId: Long) {
        val progressBar = findViewById<ProgressBar>(R.id.loading_progress)
        val btnTOC = findViewById<ImageButton>(R.id.btnTOC)
        val btnSettings = findViewById<ImageButton>(R.id.btnSettings)
        val btnSearch = findViewById<ImageButton>(R.id.btnSearch)
        val etSearch = findViewById<EditText>(R.id.etSearch)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    when (state) {
                        is ReaderUiState.Loading -> {
                            progressBar?.visibility = View.VISIBLE
                        }

                        is ReaderUiState.Success -> {
                            progressBar?.visibility = View.GONE

                            // 1. Setup Navigator if not already present
                            if (supportFragmentManager.findFragmentByTag("navigator") == null) {
                                setupNavigator(state.publication)
                            }

                            // 2. TRIGGER JUMP: Only if we have a pending ID and the navigator is ready
                            if (pendingChunkId != -1L) {
                                handleJumpToChunk(state.publication, pendingChunkId)
                            }

                            btnTOC?.setOnClickListener { showTableOfContents(state.publication) }
                            btnSettings?.setOnClickListener { showAppearanceSettings() }

                            btnSearch?.setOnClickListener {
                                etSearch.visibility = if (etSearch.visibility == View.GONE) View.VISIBLE else View.GONE
                                if (etSearch.visibility == View.VISIBLE) etSearch.requestFocus()
                            }

                            etSearch?.setOnEditorActionListener { v, actionId, _ ->
                                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                                    performSearch(state.publication, v.text.toString())
                                    true
                                } else false
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

    @OptIn(ExperimentalReadiumApi::class)
    private fun handleJumpToChunk(publication: Publication, chunkId: Long) {
        lifecycleScope.launch {
            val bookBox = ObjectBox.store.boxFor(BookChunk::class.java)
            val chunk = withContext(Dispatchers.IO) { bookBox.get(chunkId) }

            if (chunk != null) {
                val iterator = publication.search(chunk.text)
                val collection = iterator?.next()?.getOrNull()

                val locator = collection?.locators?.firstOrNull()
                if (locator != null) {
                    val navigator = supportFragmentManager.findFragmentByTag("navigator") as? EpubNavigatorFragment
                    navigator?.go(locator, animated = true)
                }
            }
        }
    }

    @OptIn(ExperimentalReadiumApi::class)
    private fun setupNavigator(publication: Publication) {
        val navigatorFactory = EpubNavigatorFactory(publication)
        val initialLocator = publication.readingOrder.firstOrNull()?.let {
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

    private fun showTableOfContents(publication: Publication) {
        val dialog = BottomSheetDialog(this)
        val listView = android.widget.ListView(this)
        listView.layoutDirection = View.LAYOUT_DIRECTION_RTL

        val navItems = if (publication.tableOfContents.isNotEmpty()) {
            publication.tableOfContents
        } else {
            publication.readingOrder
        }

        val chapters = navItems.mapIndexed { index, link -> link.title ?: "Chapter ${index + 1}" }
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_list_item_1, chapters)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val navigator = supportFragmentManager.findFragmentByTag("navigator") as? EpubNavigatorFragment
            navigator?.go(navItems[position], animated = true)
            dialog.dismiss()
        }

        dialog.setContentView(listView)
        dialog.show()
    }

    @OptIn(ExperimentalReadiumApi::class)
    private fun showAppearanceSettings() {
        val navigator = supportFragmentManager.findFragmentByTag("navigator") as? EpubNavigatorFragment
        val currentSize = navigator?.settings?.value?.fontSize ?: 1.0
        val nextSize = if (currentSize >= 2.0) 1.0 else currentSize + 0.1
        navigator?.submitPreferences(EpubPreferences(fontSize = nextSize))
        Toast.makeText(this, "Font Size: ${(nextSize * 100).toInt()}%", Toast.LENGTH_SHORT).show()
    }

    @OptIn(ExperimentalReadiumApi::class)
    private fun performSearch(publication: Publication, query: String) {
        if (query.isBlank()) return
        lifecycleScope.launch {
            try {
                val iterator = publication.search(query) ?: return@launch
                val collection = iterator.next()?.getOrNull()

                if (collection == null || collection.locators.isEmpty()) {
                    Toast.makeText(this@ReaderActivity, "No results found", Toast.LENGTH_SHORT).show()
                } else {
                    val navigator = supportFragmentManager.findFragmentByTag("navigator") as? EpubNavigatorFragment
                    navigator?.go(collection.locators.first(), animated = true)
                }
            } catch (e: Exception) {
                Toast.makeText(this@ReaderActivity, "Search failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
}