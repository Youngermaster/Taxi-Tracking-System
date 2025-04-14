package com.youngermaster.taxitrackingadminkotlin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.youngermaster.taxitrackingadminkotlin.data.mqtt.MqttRealClient
import com.youngermaster.taxitrackingadminkotlin.ui.screens.MapScreen
import com.youngermaster.taxitrackingadminkotlin.ui.theme.TaxiTrackingAdminKotlinTheme

class MainActivity : ComponentActivity() {
    
    private var mqttClient: MqttRealClient? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Inicializar cliente MQTT
        mqttClient = MqttRealClient(this)
        mqttClient?.connect()
        
        setContent {
            TaxiTrackingAdminKotlinTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MapScreen()
                }
            }
        }
    }
    
    override fun onDestroy() {
        // Desconectar cliente MQTT
        mqttClient?.disconnect()
        super.onDestroy()
    }
}