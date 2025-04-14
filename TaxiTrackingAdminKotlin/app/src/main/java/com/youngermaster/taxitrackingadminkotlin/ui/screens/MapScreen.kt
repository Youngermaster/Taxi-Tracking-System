package com.youngermaster.taxitrackingadminkotlin.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.youngermaster.taxitrackingadminkotlin.ui.components.LocationPermissionScreen
import com.youngermaster.taxitrackingadminkotlin.ui.components.UserLocationMap

@Composable
fun MapScreen() {
    var hasLocationPermission by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (!hasLocationPermission) {
            LocationPermissionScreen(
                onPermissionGranted = {
                    hasLocationPermission = true
                }
            )
        } else {
            UserLocationMap()
        }
    }
} 