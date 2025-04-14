package com.youngermaster.taxitrackingadminkotlin.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.youngermaster.taxitrackingadminkotlin.R
import com.youngermaster.taxitrackingadminkotlin.data.store.TaxiStore
import com.youngermaster.taxitrackingadminkotlin.ui.components.LocationPermissionScreen
import com.youngermaster.taxitrackingadminkotlin.ui.components.TaxiDetailsContent
import com.youngermaster.taxitrackingadminkotlin.ui.components.getUserLocation
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import kotlinx.coroutines.launch
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hasLocationPermission by remember { mutableStateOf(false) }
    
    // Cargar el icono de taxi de forma segura
    val taxiIcon = remember(context) {
        safeLoadTaxiIcon(context)
    }
    
    // Estado del bottom sheet
    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Hidden,
        skipHiddenState = false
    )
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = bottomSheetState
    )
    
    // Actualizar el estado del bottom sheet basado en TaxiStore
    LaunchedEffect(TaxiStore.isBottomSheetVisible) {
        if (TaxiStore.isBottomSheetVisible) {
            scope.launch {
                bottomSheetState.expand()
            }
        } else {
            scope.launch {
                bottomSheetState.hide()
            }
        }
    }
    
    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContent = {
            TaxiStore.selectedTaxi?.let { taxiData ->
                TaxiDetailsContent(taxiData = taxiData)
            }
        },
        sheetPeekHeight = 0.dp,
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetContentColor = MaterialTheme.colorScheme.onSurface,
        sheetShadowElevation = 8.dp
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!hasLocationPermission) {
                LocationPermissionScreen(
                    onPermissionGranted = {
                        hasLocationPermission = true
                    }
                )
            } else {
                TaxiMapWithDrivers(taxiIcon)
            }
        }
    }
}

/**
 * Carga de forma segura el icono del taxi, con manejo adecuado de errores
 */
fun safeLoadTaxiIcon(context: Context): BitmapDescriptor {
    return try {
        // Intenta cargar directamente desde el recurso y escalar
        val drawable = ContextCompat.getDrawable(context, R.drawable.taxi_icon)
        if (drawable != null) {
            // Escalar a 40dp x 40dp (convertir dp a píxeles)
            val density = context.resources.displayMetrics.density
            val widthPx = (40 * density).toInt()
            val heightPx = (40 * density).toInt()
            
            val scaledBitmap = Bitmap.createScaledBitmap(
                drawable.toBitmap(),
                widthPx,
                heightPx,
                true
            )
            BitmapDescriptorFactory.fromBitmap(scaledBitmap)
        } else {
            Log.e("MapScreen", "No se pudo cargar el drawable del taxi")
            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
        }
    } catch (e: Exception) {
        Log.e("MapScreen", "Error cargando icono como recurso", e)
        
        try {
            // Intenta cargar usando el drawable
            val drawable = ContextCompat.getDrawable(context, R.drawable.taxi_icon)
            if (drawable != null) {
                getBitmapDescriptorFromDrawable(drawable, context)
            } else {
                // Fallback final si todo falla
                Log.e("MapScreen", "No se pudo cargar el drawable", e)
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
            }
        } catch (e2: Exception) {
            Log.e("MapScreen", "Error en fallback del icono", e2)
            // Último recurso - un marcador amarillo
            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
        }
    }
}

/**
 * Composable para mostrar una imagen del taxi
 * Se puede usar en otros lugares de la UI donde no necesites un BitmapDescriptor
 */
@Composable
fun TaxiIconImage() {
    Image(
        modifier = Modifier.size(24.dp),
        painter = painterResource(id = R.drawable.taxi_icon),
        contentDescription = "Icono de taxi que muestra la ubicación de un vehículo en el mapa"
    )
}

@Composable
fun TaxiMapWithDrivers(taxiIcon: BitmapDescriptor) {
    val context = LocalContext.current
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var hasInitializedCamera by remember { mutableStateOf(false) }
    
    // Default location (Medellín, Colombia) if user location is not available
    val defaultLocation = LatLng(6.2476, -75.5658)
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 13f)
    }

    // Obtener ubicación del usuario
    LaunchedEffect(key1 = true) {
        getUserLocation(context) { location ->
            val newLocation = LatLng(location.latitude, location.longitude)
            userLocation = newLocation
            
            if (!hasInitializedCamera) {
                cameraPositionState.position = CameraPosition.fromLatLngZoom(newLocation, 13f)
                hasInitializedCamera = true
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = true),
            onMapClick = {
                // Doble tap para deseleccionar (se maneja con clics repetidos)
                TaxiStore.clearSelectedTaxi()
            }
        ) {
            // Marker para la ubicación del usuario (opcional, ya que isMyLocationEnabled=true)
            userLocation?.let { location ->
                Marker(
                    state = MarkerState(position = location),
                    title = "Mi ubicación",
                    snippet = "Estás aquí"
                )
            }
            
            // Markers para los taxis con icono personalizado
            TaxiStore.taxis.forEach { (driverId, taxi) ->
                val taxiLocation = LatLng(
                    taxi.driverLocation.latitude,
                    taxi.driverLocation.longitude
                )
                
                Marker(
                    state = MarkerState(position = taxiLocation),
                    title = taxi.driverName,
                    snippet = taxi.vehicleNumberId,
                    icon = taxiIcon,
                    onClick = {
                        TaxiStore.selectTaxi(driverId)
                        true
                    }
                )
            }
        }
    }
}

/**
 * Función para convertir un drawable en un BitmapDescriptor que puede ser usado como icono de marcador
 * Escala el icono a 40dp x 40dp por defecto
 */
private fun getBitmapDescriptorFromDrawable(
    drawable: Drawable, 
    context: Context,
    widthDp: Int = 40, 
    heightDp: Int = 40
): BitmapDescriptor {
    // Convertir dp a píxeles
    val density = context.resources.displayMetrics.density
    val widthPx = (widthDp * density).toInt()
    val heightPx = (heightDp * density).toInt()
    
    // Crear un bitmap del tamaño deseado
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    // Escalar el drawable para que llene el bitmap
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    
    return BitmapDescriptorFactory.fromBitmap(bitmap)
} 