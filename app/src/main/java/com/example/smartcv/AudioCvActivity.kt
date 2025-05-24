package com.example.smartcv

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartcv.data.model.CvData
import com.example.smartcv.data.repository.CvRepository
import com.example.smartcv.data.model.Education
import com.example.smartcv.data.model.Experience
import com.example.smartcv.data.model.PersonalInfo
import com.example.smartcv.presentation.screens.CvEditScreen
import com.example.smartcv.ui.theme.SmartCVTheme
import com.example.smartcv.utils.AppColors
import com.example.smartcv.utils.OpenAIService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Activity for processing audio transcripts and generating CVs
 */
class AudioCvActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get transcript from intent
        val transcript = intent.getStringExtra("transcript") ?: ""
        val cvId = intent.getStringExtra("cv_id") ?: UUID.randomUUID().toString()

        setContent {
            SmartCVTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppColors.backgroundGray
                ) {
                    AudioCvScreen(transcript, cvId)
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AudioCvScreen(transcript: String, cvId: String) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val cvRepository = CvRepository(context)

        var isProcessing by remember { mutableStateOf(true) }
        var processingError by remember { mutableStateOf<String?>(null) }
        var cvData by remember { mutableStateOf<CvData?>(null) }

        // Process transcript on launch
        LaunchedEffect(transcript) {
            if (transcript.isNotEmpty()) {
                try {
                    // Process transcript with OpenAI
                    val processedCv = withContext(Dispatchers.IO) {
                        OpenAIService.processCvFromTranscript(transcript)
                    }

                    // Create CV data
                    cvData = processedCv.copy(
                        id = cvId,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    Log.e("AudioCvActivity", "Error processing transcript: ${e.message}", e)
                    processingError = "Error processing transcript: ${e.message}"

                    // Create a basic CV with just the transcript as summary if processing fails
                    cvData = CvData(
                        id = cvId,
                        name = "Voice CV ${java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(java.util.Date())}",
                        personalInfo = PersonalInfo(summary = transcript),
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                } finally {
                    isProcessing = false
                }
            } else {
                processingError = "No transcript provided"
                isProcessing = false
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Voice CV Generator") },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = AppColors.darkGray
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (isProcessing) {
                    // Loading state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.weight(0.3f))

                        CircularProgressIndicator(
                            color = AppColors.primaryTeal,
                            modifier = Modifier.size(64.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Processing your voice input...",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.darkGray
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "We're extracting information to create your CV",
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.weight(0.7f))
                    }
                } else if (processingError != null && cvData == null) {
                    // Error state with no CV data
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.weight(0.3f))

                        Text(
                            text = "Error",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = processingError ?: "Unknown error",
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { finish() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.primaryTeal
                            )
                        ) {
                            Text("Go Back")
                        }

                        Spacer(modifier = Modifier.weight(0.7f))
                    }
                } else if (cvData != null) {
                    // CV edit screen
                    CvEditScreen(
                        cvData = cvData!!,
                        onSave = { updatedCv ->
                            coroutineScope.launch {
                                try {
                                    // Save CV
                                    cvRepository.saveCvData(updatedCv)

                                    Toast.makeText(
                                        context,
                                        "CV saved successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    // Go back to main activity
                                    finish()
                                } catch (e: Exception) {
                                    Log.e("AudioCvActivity", "Error saving CV: ${e.message}", e)
                                    Toast.makeText(
                                        context,
                                        "Error saving CV: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

