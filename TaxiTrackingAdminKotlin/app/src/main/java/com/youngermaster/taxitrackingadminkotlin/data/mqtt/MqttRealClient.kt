package com.youngermaster.taxitrackingadminkotlin.data.mqtt

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.youngermaster.taxitrackingadminkotlin.data.models.DriverLocationData
import com.youngermaster.taxitrackingadminkotlin.data.store.TaxiStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.UUID

/**
 * Cliente MQTT que se conecta al broker MQTT de dev.grisu.co
 * Usa directamente la biblioteca Eclipse Paho MQTT Client para Java
 */
class MqttRealClient(private val context: Context) {
    
    companion object {
        private const val TAG = "MqttRealClient"
        // Broker real específico
        private const val BROKER_URL = "tcp://dev.grisu.co:1883"
        private const val TOPIC = "location_drivers/#"
        private const val QOS = 1
        private const val MAX_RETRIES = 5
        private const val RETRY_DELAY_MS = 5000L // 5 segundos
    }
    
    // Cliente MQTT directo de Java
    private var mqttClient: MqttClient? = null
    private val clientId = "TaxiTrackingAdmin_" + UUID.randomUUID().toString()
    private var retryCount = 0
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Parser JSON para los mensajes MQTT
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val driverAdapter = moshi.adapter(DriverLocationData::class.java)
    
    /**
     * Iniciar conexión MQTT
     */
    fun connect() {
        Log.d(TAG, "Iniciando conexión MQTT a $BROKER_URL")
        
        // Iniciar conexión en un hilo de fondo para no bloquear la UI
        scope.launch(Dispatchers.IO) {
            try {
                // Crear cliente con persistencia en memoria
                mqttClient = MqttClient(BROKER_URL, clientId, MemoryPersistence())
                Log.d(TAG, "Cliente MQTT creado con ID: $clientId")
                
                // Configurar callbacks para eventos MQTT
                mqttClient?.setCallback(object : MqttCallbackExtended {
                    override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                        Log.d(TAG, "Conexión MQTT completada. Reconexión: $reconnect, URI: $serverURI")
                        if (reconnect) {
                            // Volver a suscribirse tras reconexión
                            subscribe(TOPIC)
                        }
                    }
                    
                    override fun connectionLost(cause: Throwable?) {
                        Log.e(TAG, "Conexión MQTT perdida. Causa: ${cause?.message}", cause)
                        
                        // Intentar reconectar
                        scope.launch {
                            delay(RETRY_DELAY_MS)
                            Log.d(TAG, "Intentando reconectar después de pérdida de conexión")
                            connectWithRetry()
                        }
                    }
                    
                    override fun messageArrived(topic: String?, message: MqttMessage?) {
                        message?.let {
                            val messageContent = String(it.payload)
                            Log.d(TAG, "Mensaje recibido en $topic: ${messageContent.take(100)}...")
                            
                            try {
                                val driverData = driverAdapter.fromJson(messageContent)
                                driverData?.let { data ->
                                    // Actualizar el store con los datos del taxi
                                    TaxiStore.updateTaxi(data)
                                    Log.d(TAG, "Datos de taxi actualizados: ${data.driverName}, ${data.vehicleNumberId}")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error al procesar mensaje MQTT: ${e.message}", e)
                                Log.d(TAG, "Contenido del mensaje problemático: $messageContent")
                            }
                        }
                    }
                    
                    override fun deliveryComplete(token: IMqttDeliveryToken?) {
                        Log.d(TAG, "Entrega de mensaje completada. Token: ${token?.message}")
                    }
                })
                
                // Iniciar conexión con reintentos
                connectWithRetry()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error al inicializar MQTT: ${e.message}", e)
            }
        }
    }
    
    /**
     * Intentar conectar con reintentos en caso de error
     */
    private fun connectWithRetry() {
        try {
            if (mqttClient?.isConnected == true) {
                Log.d(TAG, "MQTT ya está conectado")
                return
            }
            
            // Opciones de conexión
            val options = MqttConnectOptions().apply {
                isCleanSession = true
                connectionTimeout = 30
                keepAliveInterval = 60
                isAutomaticReconnect = true
            }
            
            Log.d(TAG, "Intentando conectar a MQTT (intento ${retryCount + 1})")
            mqttClient?.connect(options)
            
            // Si llegamos aquí sin excepción, la conexión fue exitosa
            Log.d(TAG, "Conexión MQTT exitosa")
            retryCount = 0
            
            // Suscribirse al topic
            subscribe(TOPIC)
            
        } catch (e: MqttException) {
            Log.e(TAG, "Error al conectar a MQTT", e)
            
            // Reintentar si no excedimos el máximo
            if (retryCount < MAX_RETRIES) {
                retryCount++
                Log.d(TAG, "Reintentando en ${RETRY_DELAY_MS/1000} segundos (intento $retryCount/$MAX_RETRIES)")
                
                scope.launch {
                    delay(RETRY_DELAY_MS)
                    connectWithRetry()
                }
            } else {
                Log.e(TAG, "Se excedió el número máximo de intentos de conexión")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado al conectar a MQTT", e)
        }
    }
    
    /**
     * Suscribirse a un topic
     */
    fun subscribe(topic: String, qos: Int = QOS) {
        try {
            if (mqttClient?.isConnected != true) {
                Log.e(TAG, "No se puede suscribir: MQTT no está conectado")
                return
            }
            
            mqttClient?.subscribe(topic, qos)
            Log.d(TAG, "Suscrito al topic: $topic con QoS: $qos")
        } catch (e: MqttException) {
            Log.e(TAG, "Error al suscribirse al topic: $topic", e)
        }
    }
    
    /**
     * Publicar un mensaje en un topic
     */
    fun publish(topic: String, message: String, qos: Int = QOS, retained: Boolean = false) {
        try {
            if (mqttClient?.isConnected != true) {
                Log.e(TAG, "No se puede publicar: MQTT no está conectado")
                return
            }
            
            val mqttMessage = MqttMessage(message.toByteArray()).apply {
                this.qos = qos
                this.isRetained = retained
            }
            
            mqttClient?.publish(topic, mqttMessage)
            Log.d(TAG, "Mensaje publicado en topic: $topic - $message")
        } catch (e: MqttException) {
            Log.e(TAG, "Error al publicar mensaje en topic: $topic", e)
        }
    }
    
    /**
     * Desconectar cliente MQTT
     */
    fun disconnect() {
        try {
            if (mqttClient?.isConnected == true) {
                mqttClient?.disconnect()
                Log.d(TAG, "Cliente MQTT desconectado")
            }
            mqttClient?.close()
        } catch (e: MqttException) {
            Log.e(TAG, "Error al desconectar cliente MQTT", e)
        }
    }
} 