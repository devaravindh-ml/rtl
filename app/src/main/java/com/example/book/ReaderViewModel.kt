package com.example.book

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers // Added
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext // Added
import org.readium.r2.shared.publication.Publication
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val repository: BookRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReaderUiState>(ReaderUiState.Idle)
    val uiState: StateFlow<ReaderUiState> = _uiState

    fun loadAssetBook(fileName: String) {
        viewModelScope.launch {
            _uiState.value = ReaderUiState.Loading
            try {
                // withContext(Dispatchers.IO) moves the work off the Main Thread
                val publication = withContext(Dispatchers.IO) {
                    repository.openBookFromAssets(fileName)
                }
                _uiState.value = ReaderUiState.Success(publication)
            } catch (e: Exception) {
                _uiState.value = ReaderUiState.Error(e.message ?: "Failed to load asset")
            }
        }
    }

    fun openPublication(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = ReaderUiState.Loading
            try {
                val publication = withContext(Dispatchers.IO) {
                    repository.openEpub(uri)
                }
                _uiState.value = ReaderUiState.Success(publication)
            } catch (e: Exception) {
                _uiState.value = ReaderUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed class ReaderUiState {
    data object Idle : ReaderUiState() // 'data object' is preferred in modern Kotlin
    data object Loading : ReaderUiState()
    data class Success(val publication: Publication) : ReaderUiState()
    data class Error(val message: String) : ReaderUiState()
}