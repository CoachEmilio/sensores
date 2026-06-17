package com.uade.sensores.ui.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.uade.sensores.data.repository.MeasurementRepository
import com.uade.sensores.model.AcelerometroMedicion
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: MeasurementRepository
) : ViewModel() {

    // Live sensor state (not persisted)
    private val _measure = mutableStateOf(AcelerometroMedicion(0f, 0f, 0f))
    val measure: State<AcelerometroMedicion> = _measure

    // Persisted history from Room, exposed as StateFlow for Compose
    val historical: StateFlow<List<AcelerometroMedicion>> =
        repository.observarMediciones().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val total: StateFlow<Int> =
        repository.contarMediciones().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun onNewMeasure(m: AcelerometroMedicion) {
        _measure.value = m
        // Persist only strong measurements to avoid flooding the database.
        if (m.esBrusco) {
            viewModelScope.launch { repository.guardar(m) }
        }
    }

    fun cleanHistory() {
        viewModelScope.launch { repository.eliminarTodas() }
    }

    /**
     * Triggers a sync with the Supabase backend:
     *   - Uploads pending local measurements (those not yet sent).
     *   - Downloads everything stored in the backend and persists it locally.
     */
    fun syncWithBackend() {
        viewModelScope.launch {
            try {
                repository.sincronizar()
                Log.d("Sync", "Sync OK")
            } catch (e: Exception) {
                Log.e("Sync", "Error: ${e.message}", e)
            }
        }
    }

    /**
     * Factory necessary because MainViewModel has a constructor with parameters.
     * Without factory, ViewModelProvider does not know how to create instances.
     */
    class Factory(
        private val repository: MeasurementRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(repository) as T
    }
}