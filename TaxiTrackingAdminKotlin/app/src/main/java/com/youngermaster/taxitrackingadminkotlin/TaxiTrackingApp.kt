package com.youngermaster.taxitrackingadminkotlin

import android.app.Application
import android.util.Log
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapsSdkInitializedCallback

/**
 * Clase principal de la aplicación que se inicia antes que cualquier Activity
 * Ideal para inicializar componentes que se usarán en toda la app
 */
class TaxiTrackingApp : Application(), OnMapsSdkInitializedCallback {
    
    override fun onCreate() {
        super.onCreate()
        
        try {
            // Inicialización de Google Maps con callback para verificar que se inicializó correctamente
            MapsInitializer.initialize(applicationContext, MapsInitializer.Renderer.LATEST, this)
            Log.d(TAG, "Inicialización de Google Maps solicitada")
        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar Google Maps", e)
        }
    }
    
    override fun onMapsSdkInitialized(renderer: MapsInitializer.Renderer) {
        when (renderer) {
            MapsInitializer.Renderer.LATEST -> 
                Log.d(TAG, "Google Maps inicializado con el renderizador más reciente")
            MapsInitializer.Renderer.LEGACY -> 
                Log.d(TAG, "Google Maps inicializado con el renderizador legacy")
            else -> 
                Log.d(TAG, "Google Maps inicializado con un renderizador desconocido")
        }
    }
    
    companion object {
        private const val TAG = "TaxiTrackingApp"
    }
} 