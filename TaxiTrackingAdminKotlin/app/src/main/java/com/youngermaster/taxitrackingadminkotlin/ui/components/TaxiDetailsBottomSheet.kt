package com.youngermaster.taxitrackingadminkotlin.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.youngermaster.taxitrackingadminkotlin.data.models.DriverLocationData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxiDetailsContent(taxiData: DriverLocationData) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Encabezado con nombre del conductor y placa
        Text(
            text = taxiData.driverName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Vehículo: ${taxiData.vehicleNumberId}",
            style = MaterialTheme.typography.titleMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Detalles de la ruta
        SectionTitle(text = "Información de Ruta")
        DetailItem(label = "Ruta", value = taxiData.routeName)
        DetailItem(label = "Servicio", value = taxiData.admServiceName)
        
        // Estado de la ruta
        val routeStatus = when {
            taxiData.routeIsFinished -> "Finalizada"
            taxiData.routeIsProcessed -> "En proceso"
            else -> "No iniciada"
        }
        DetailItem(label = "Estado", value = routeStatus)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Puntos de interés
        SectionTitle(text = "Puntos de Interés")
        taxiData.routeInterestPoints.forEachIndexed { index, point ->
            Text(
                text = "${index + 1}. $point",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Timestamp
        SectionTitle(text = "Última Actualización")
        val formattedDate = formatIsoDate(taxiData.timestamp)
        Text(
            text = formattedDate,
            style = MaterialTheme.typography.bodyMedium
        )
        
        // Ubicación actual
        SectionTitle(text = "Ubicación Actual")
        DetailItem(
            label = "Latitud",
            value = taxiData.driverLocation.latitude.toString()
        )
        DetailItem(
            label = "Longitud",
            value = taxiData.driverLocation.longitude.toString()
        )
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun formatIsoDate(isoDateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        
        val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        outputFormat.timeZone = TimeZone.getDefault()
        
        val date: Date = inputFormat.parse(isoDateString) ?: return isoDateString
        outputFormat.format(date)
    } catch (e: Exception) {
        isoDateString // Retorna el original si hay error
    }
} 