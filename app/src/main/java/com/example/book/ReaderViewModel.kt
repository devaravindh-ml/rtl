package com.example.book

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.readium.r2.shared.publication.Publication
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val repository: BookRepository
) : ViewModel() {

    // Represents the current state of the reader
    private val _uiState = MutableStateFlow<ReaderUiState>(ReaderUiState.Idle)
    val uiState: StateFlow<ReaderUiState> = _uiState

    /**
     * NEW: Opens the specific asset file you created (rtl_book.epub)
     */
    fun loadAssetBook(fileName: String) {
        viewModelScope.launch {
            _uiState.value = ReaderUiState.Loading
            try {
                // This calls the asset-specific logic in your repository
                val publication = repository.openBookFromAssets(fileName)
                _uiState.value = ReaderUiState.Success(publication)
            } catch (e: Exception) {
                _uiState.value = ReaderUiState.Error(e.message ?: "Failed to load asset")
            }
        }
    }

    /**
     * Opens a publication from the given URI (e.g., from a file picker).
     */
    fun openPublication(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = ReaderUiState.Loading
            try {
                // Ensure your repository has an 'openEpub' method for Uris
                val publication = repository.openEpub(uri)
                _uiState.value = ReaderUiState.Success(publication)
            } catch (e: Exception) {
                _uiState.value = ReaderUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

/**
 * Sealed class representing the different states of the Reader UI.
 * Make sure this definition exists ONLY here in your project.
 */
sealed class ReaderUiState {
    object Idle : ReaderUiState()
    object Loading : ReaderUiState()
    data class Success(val publication: Publication) : ReaderUiState()
    data class Error(val message: String) : ReaderUiState()
}