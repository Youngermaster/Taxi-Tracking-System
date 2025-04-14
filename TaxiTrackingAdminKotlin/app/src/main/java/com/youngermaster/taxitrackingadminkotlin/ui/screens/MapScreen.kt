package com.youngermaster.taxitrackingadminkotlin.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.youngermaster.taxitrackingadminkotlin.data.store.TaxiStore
import com.youngermaster.taxitrackingadminkotlin.ui.components.LocationPermissionScreen
import com.youngermaster.taxitrackingadminkotlin.ui.components.TaxiDetailsContent
import com.youngermaster.taxitrackingadminkotlin.ui.components.UserLocationMap
import com.youngermaster.taxitrackingadminkotlin.ui.components.getUserLocation
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen() {
    var hasLocationPermission by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
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
                TaxiMapWithDrivers()
            }
        }
    }
}

@Composable
fun TaxiMapWithDrivers() {
    val context = LocalContext.current
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var hasInitializedCamera by remember { mutableStateOf(false) }
    
    // Default location (Medellín, Colombia) if user location is not available
    val defaultLocation = LatLng(6.2476, -75.5658)
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 13f)
    }

    // Obtener ubicación del usuario (similar a UserLocationMap pero integrado con marcadores de taxis)
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
            
            // Markers para los taxis
            TaxiStore.taxis.forEach { (driverId, taxi) ->
                val taxiLocation = LatLng(
                    taxi.driverLocation.latitude,
                    taxi.driverLocation.longitude
                )
                
                Marker(
                    state = MarkerState(position = taxiLocation),
                    title = taxi.driverName,
                    snippet = taxi.vehicleNumberId,
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW),
                    onClick = {
                        TaxiStore.selectTaxi(driverId)
                        true
                    }
                )
            }
        }
    }
} 