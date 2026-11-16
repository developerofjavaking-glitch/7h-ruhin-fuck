package com.example.apkextractor.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.apkextractor.data.AppDetails
import com.example.apkextractor.data.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DetailsUiState {
    data object Loading : DetailsUiState
    data class Success(val details: AppDetails) : DetailsUiState
    data object NotFound : DetailsUiState
}

class DetailsViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<DetailsUiState>(DetailsUiState.Loading)
    val state: StateFlow<DetailsUiState> = _state.asStateFlow()

    private var loadedPackage: String? = null

    fun load(packageName: String) {
        if (loadedPackage == packageName && _state.value is DetailsUiState.Success) return
        loadedPackage = packageName
        viewModelScope.launch {
            _state.value = DetailsUiState.Loading
            val details = AppRepository.loadAppDetails(getApplication(), packageName)
            _state.value = if (details != null) {
                DetailsUiState.Success(details)
            } else {
                DetailsUiState.NotFound
            }
        }
    }
}
