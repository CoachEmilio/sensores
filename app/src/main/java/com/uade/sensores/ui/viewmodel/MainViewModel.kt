package com.uade.sensores.ui.viewmodel

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

    // Estado actual del sensor (live, no persisted)
    private val _measure = mutableStateOf(AcelerometroMedicion(0f, 0f, 0f))
    val measure: State<AcelerometroMedicion> = _measure

    // Historical registration fo data in Room, expose like StateFlow for Compose
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
        // Solo persist the strong measure for not fill the base.
        // If you want to save al, change IF
        if (m.esBrusco) {
            viewModelScope.launch { repository.guardar(m) }
        }
    }

    fun cleanHistory() {
        viewModelScope.launch { repository.eliminarTodas() }
    }

    /**
     * Factory necessary because MainViewModel have a constructor with parameters.
     * without factory, ViewModelProvider do not know how to create instances.
     */
    class Factory(
        private val repository: MeasurementRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(repository) as T
    }
}