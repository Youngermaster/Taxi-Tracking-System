package com.youngermaster.taxitrackingadminkotlin.data.store

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.youngermaster.taxitrackingadminkotlin.data.models.DriverLocationData

/**
 * Singleton para almacenar y gestionar el estado de los taxis
 * Funciona de forma similar a una tienda de estado como Zustand en React Native
 */
object TaxiStore {
    // Mapa de todos los taxis, con el driverId como clave
    private var _taxis by mutableStateOf<Map<String, DriverLocationData>>(emptyMap())
    val taxis: Map<String, DriverLocationData> get() = _taxis
    
    // Taxi seleccionado actualmente
    private var _selectedTaxi by mutableStateOf<DriverLocationData?>(null)
    val selectedTaxi: DriverLocationData? get() = _selectedTaxi
    
    // Indica si el bottom sheet debería mostrarse
    private var _isBottomSheetVisible by mutableStateOf(false)
    val isBottomSheetVisible: Boolean get() = _isBottomSheetVisible
    
    /**
     * Actualiza o añade información de un taxi
     */
    fun updateTaxi(taxi: DriverLocationData) {
        _taxis = _taxis.toMutableMap().apply {
            this[taxi.driverId] = taxi
        }
    }
    
    /**
     * Selecciona un taxi para mostrar sus detalles
     */
    fun selectTaxi(driverId: String) {
        _selectedTaxi = _taxis[driverId]
        _isBottomSheetVisible = true
    }
    
    /**
     * Deselecciona el taxi actual
     */
    fun clearSelectedTaxi() {
        _selectedTaxi = null
        _isBottomSheetVisible = false
    }
    
    /**
     * Oculta el bottom sheet sin deseleccionar el taxi
     */
    fun hideBottomSheet() {
        _isBottomSheetVisible = false
    }
    
    /**
     * Muestra el bottom sheet (si hay un taxi seleccionado)
     */
    fun showBottomSheet() {
        if (_selectedTaxi != null) {
            _isBottomSheetVisible = true
        }
    }
} 