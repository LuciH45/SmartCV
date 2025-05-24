package com.example.smartcv.presentation.screens

import android.Manifest
import android.content.pm.PackageManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.smartcv.data.model.CvData
import com.example.smartcv.data.repository.CvRepository
import com.example.smartcv.data.model.PersonalInfo
import com.example.smartcv.utils.AppColors
import com.example.smartcv.utils.OpenAIService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Screen for generating CVs from voice input in Spanish
 */
@Composable
fun GenerateScreen(
    onCvGenerated: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val cvRepository = CvRepository(context)

    var isRecording by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var transcribedText by remember { mutableStateOf("") }
    var processingComplete by remember { mutableStateOf(false) }
    var generatedCvId by remember { mutableStateOf("") }

    // Check if API key is configured
    val isApiKeyConfigured = remember { OpenAIService.isApiKeyConfigured() }

    // State for permission
    var hasRecordPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Permission launcher
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasRecordPermission = isGranted
        if (isGranted) {
            Toast.makeText(
                context,
                "Permission granted. You can now record audio.",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                context,
                "Permission denied. Voice recording is not available.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Check permission on start
    LaunchedEffect(Unit) {
        if (!hasRecordPermission) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Speech recognition functions
    val startRecording = {
        if (hasRecordPermission) {
            val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            val speechIntent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                // Set language to Spanish
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: android.os.Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val recognizedText = matches?.get(0) ?: "No se reconoció ningún discurso"
                    transcribedText = recognizedText
                    isRecording = false
                    speechRecognizer.destroy()
                }

                override fun onPartialResults(partialResults: android.os.Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val recognizedText = matches?.get(0) ?: ""
                    if (recognizedText.isNotEmpty()) {
                        transcribedText = recognizedText
                    }
                }

                override fun onError(error: Int) {
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Error de grabación de audio"
                        SpeechRecognizer.ERROR_CLIENT -> "Error del cliente"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permisos insuficientes"
                        SpeechRecognizer.ERROR_NETWORK -> "Error de red"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Tiempo de espera de red agotado"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No se encontró coincidencia"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Servicio de reconocimiento ocupado"
                        SpeechRecognizer.ERROR_SERVER -> "Error del servidor"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No se detectó voz"
                        else -> "Error desconocido"
                    }
                    Log.e("SpeechRecognizer", "Error: $errorMessage ($error)")
                    Toast.makeText(context, "Error de reconocimiento de voz: $errorMessage", Toast.LENGTH_SHORT).show()
                    isRecording = false
                    speechRecognizer.destroy()
                }

                override fun onReadyForSpeech(params: android.os.Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
            })

            try {
                speechRecognizer.startListening(speechIntent)
                isRecording = true
            } catch (e: Exception) {
                Log.e("SpeechRecognizer", "Error al iniciar el reconocimiento de voz: ${e.message}")
                Toast.makeText(context, "Error al iniciar el reconocimiento de voz", Toast.LENGTH_SHORT).show()
                speechRecognizer.destroy()
            }
        } else {
            Toast.makeText(
                context,
                "Se requiere permiso de micrófono",
                Toast.LENGTH_SHORT
            ).show()
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val stopRecording = {
        isRecording = false
        // The speech recognizer will be destroyed in the onResults or onError callbacks
    }

    val generateCvFromTranscript = {
        if (transcribedText.isNotEmpty()) {
            isProcessing = true
            coroutineScope.launch {
                try {
                    // Process transcript with OpenAI
                    val processedCv = withContext(Dispatchers.IO) {
                        OpenAIService.processCvFromTranscript(transcribedText, true)
                    }

                    // Create CV data
                    val timestamp = System.currentTimeMillis()
                    val cvId = UUID.randomUUID().toString()
                    val cvName = if (processedCv.personalInfo.fullName.isNotEmpty()) {
                        "${processedCv.personalInfo.fullName}'s CV"
                    } else {
                        "CV de Voz ${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}"
                    }

                    val cvData = processedCv.copy(
                        id = cvId,
                        name = cvName,
                        createdAt = timestamp,
                        updatedAt = timestamp
                    )

                    // Save the processed CV
                    cvRepository.saveCvData(cvData)

                    // Store the CV ID for navigation
                    generatedCvId = cvId
                    processingComplete = true

                    // Call the callback with the generated CV ID
                    onCvGenerated(cvId)
                } catch (e: Exception) {
                    Log.e("GenerateScreen", "Error al generar CV: ${e.message}", e)
                    
                    // Create a basic CV with just the transcript as summary
                    val timestamp = System.currentTimeMillis()
                    val cvId = UUID.randomUUID().toString()
                    val cvName = "CV de Voz ${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}"

                    val cvData = CvData(
                        id = cvId,
                        name = cvName,
                        personalInfo = PersonalInfo(summary = transcribedText),
                        createdAt = timestamp,
                        updatedAt = timestamp
                    )

                    // Save the basic CV
                    cvRepository.saveCvData(cvData)

                    // Store the CV ID for navigation
                    generatedCvId = cvId
                    processingComplete = true

                    // Call the callback with the generated CV ID
                    onCvGenerated(cvId)
                } finally {
                    isProcessing = false
                }
            }
        } else {
            Toast.makeText(
                context,
                "Por favor, graba algo de voz primero",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Voice CV Generator",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.darkGray,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // API Key Warning
        if (!isApiKeyConfigured) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF3E0) // Light orange background
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 1.dp
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF9800), // Orange warning color
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "OpenAI API Key Not Configured",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF795548) // Dark brown text
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "The generated CV will only contain the transcription in the summary. For a structured CV, configure the API key in OpenAIService.kt",
                            fontSize = 14.sp,
                            color = Color(0xFF795548) // Dark brown text
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 1.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            if (isRecording) Color.Red.copy(alpha = 0.1f)
                            else AppColors.primaryTeal.copy(alpha = 0.1f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.KeyboardVoice,
                        contentDescription = null,
                        tint = if (isRecording) Color.Red else AppColors.primaryTeal,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isRecording) "Recording" else "Ready to record",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isRecording) Color.Red else AppColors.primaryTeal
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Speak clearly about your education, work experience, and skills in Spanish",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (!isRecording) {
                            startRecording()
                        } else {
                            stopRecording()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) Color.Red else AppColors.primaryTeal
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.KeyboardVoice,
                        contentDescription = if (isRecording) "Stop Recording" else "Start Recording"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isRecording) "Stop Recording" else "Start Recording")
                }
            }
        }

        // Transcribed text card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 1.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transcribed Text (Spanish)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.darkGray
                    )

                    if (isRecording) {
                        Spacer(modifier = Modifier.width(8.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = AppColors.primaryTeal,
                            strokeWidth = 2.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (transcribedText.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Record your voice to generate content for your CV",
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            fontStyle = FontStyle.Italic
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = transcribedText,
                            color = AppColors.darkGray,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }

        // Generate CV button
        if (transcribedText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { generateCvFromTranscript() },
                enabled = !isProcessing && transcribedText.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.primaryTeal,
                    disabledContainerColor = Color.Gray
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Processing...")
                } else {
                    Icon(
                        imageVector = Icons.Default.KeyboardVoice,
                        contentDescription = "Generate CV"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate CV from Voice")
                }
            }
        }
    }
}

