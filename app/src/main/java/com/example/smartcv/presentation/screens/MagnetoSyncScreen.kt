package com.example.smartcv.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.smartcv.data.repository.CvRepository
import com.example.smartcv.data.model.CvData
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.example.smartcv.api.ApiResponse
import com.example.smartcv.utils.AppColors
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color

@Composable
fun MagnetoSyncScreen() {
    val context = LocalContext.current
    val repository = remember { CvRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var cvs by remember { mutableStateOf<List<CvData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var syncStatus by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var syncTimestamps by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var loadingItems by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Cargar CVs al inicio
    LaunchedEffect(Unit) {
        cvs = repository.loadAllCvs()
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Título
        Text(
            text = "Sincronización con Magneto",
            style = MaterialTheme.typography.headlineMedium,
            color = AppColors.darkGray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Botón de sincronización masiva
        if (cvs.isNotEmpty()) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        cvs.forEach { cv ->
                            loadingItems = loadingItems + cv.id
                            val response = repository.sendCvToMagneto(cv.id)
                            when (response) {
                                is ApiResponse.Success -> {
                                    val data = response.data
                                    if (data.success) {
                                        syncStatus = syncStatus + (cv.id to true)
                                        syncTimestamps = syncTimestamps + (cv.id to data.timestamp)
                                    } else {
                                        syncStatus = syncStatus + (cv.id to false)
                                    }
                                }
                                else -> {
                                    syncStatus = syncStatus + (cv.id to false)
                                }
                            }
                            loadingItems = loadingItems - cv.id
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.primaryTeal
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Sincronizar todos")
            }
        }

        // Lista de CVs
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = AppColors.primaryTeal
                )
            }
        } else if (cvs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay CVs disponibles para sincronizar",
                    color = AppColors.darkGray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cvs) { cv ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 1.dp
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = cv.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = AppColors.darkGray
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Estado de sincronización
                            val isSynced = syncStatus[cv.id] ?: false
                            val timestamp = syncTimestamps[cv.id] ?: 0L
                            val isLoading = loadingItems.contains(cv.id)
                            
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = AppColors.primaryTeal
                                )
                            } else {
                                Text(
                                    text = if (isSynced) {
                                        if (timestamp > 0) {
                                            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                            "Sincronizado: ${dateFormat.format(Date(timestamp))}"
                                        } else {
                                            "Sincronizado"
                                        }
                                    } else {
                                        "No sincronizado"
                                    },
                                    color = if (isSynced) AppColors.primaryTeal else Color(0xFFE74C3C)
                                )
                                
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            loadingItems = loadingItems + cv.id
                                            val response = repository.sendCvToMagneto(cv.id)
                                            when (response) {
                                                is ApiResponse.Success -> {
                                                    val data = response.data
                                                    if (data.success) {
                                                        syncStatus = syncStatus + (cv.id to true)
                                                        syncTimestamps = syncTimestamps + (cv.id to data.timestamp)
                                                    } else {
                                                        syncStatus = syncStatus + (cv.id to false)
                                                    }
                                                }
                                                else -> {
                                                    syncStatus = syncStatus + (cv.id to false)
                                                }
                                            }
                                            loadingItems = loadingItems - cv.id
                                        }
                                    },
                                    modifier = Modifier.align(Alignment.End),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AppColors.primaryTeal
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Sincronizar")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
