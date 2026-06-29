package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.TravelLocation
import com.example.network.TravelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class TravelUiState {
    object Loading : TravelUiState()
    data class Success(val locations: List<TravelLocation>) : TravelUiState()
    data class Error(val message: String) : TravelUiState()
}

class TravelViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TravelRepository(application)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _syncProgress = MutableStateFlow<String?>(null)
    val syncProgress: StateFlow<String?> = _syncProgress.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val uiState: StateFlow<TravelUiState> = combine(
        repository.allLocations,
        _isLoading,
        _error
    ) { locations, loading, err ->
        when {
            err != null && locations.isEmpty() -> TravelUiState.Error(err)
            locations.isEmpty() && loading -> TravelUiState.Loading
            else -> TravelUiState.Success(locations)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TravelUiState.Loading
    )

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _syncProgress.value = "Başlatılıyor..."
            try {
                repository.syncData { progress ->
                    _syncProgress.value = progress
                }
            } catch (e: Throwable) {
                _error.value = "Veriler güncellenemedi:\n${e.stackTraceToString()}"
            } finally {
                _isLoading.value = false
                _syncProgress.value = null
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
}
