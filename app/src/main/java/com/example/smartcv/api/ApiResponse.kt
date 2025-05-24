package com.example.smartcv.api

/**
 * Clase genérica para manejar respuestas de API
 */
sealed class ApiResponse<out T> {
    data class Success<out T>(val data: T) : ApiResponse<T>()
    data class Error(val errorMessage: String, val errorCode: Int = 0) : ApiResponse<Nothing>()
    object Loading : ApiResponse<Nothing>()
}

/**
 * Respuesta específica para la sincronización con Magneto
 */
data class MagnetoSyncResponse(
    val success: Boolean,
    val message: String,
    val cvId: String,
    val timestamp: Long = System.currentTimeMillis()
)
