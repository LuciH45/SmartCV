package com.example.smartcv

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartcv.data.CvData
import com.example.smartcv.data.CvRepository
import com.example.smartcv.data.DocumentData
import com.example.smartcv.data.DocumentRepository
import com.example.smartcv.presentation.components.AppBottomNavigation
import com.example.smartcv.presentation.components.AppTopBar
import com.example.smartcv.presentation.screens.CvEditScreen
import com.example.smartcv.presentation.screens.CvScanScreen
import com.example.smartcv.presentation.screens.CvViewScreen
import com.example.smartcv.presentation.screens.DocumentListScreen
import com.example.smartcv.presentation.screens.DocumentViewScreen
import com.example.smartcv.presentation.screens.EditDocumentScreen
import com.example.smartcv.presentation.screens.GenerateScreen
import com.example.smartcv.presentation.screens.HomeScreen
import com.example.smartcv.presentation.screens.ProfileScreen
import com.example.smartcv.presentation.screens.SettingsScreen
import com.example.smartcv.ui.theme.SmartCVTheme
import com.example.smartcv.utils.AppColors
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Main Activity for the SmartCV application
 * Handles navigation between screens and document scanning functionality
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure document scanner options
        val options = GmsDocumentScannerOptions.Builder()
            .setScannerMode(SCANNER_MODE_FULL)
            .setGalleryImportAllowed(true)
            .setPageLimit(10)
            .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
            .build()
        val scanner = GmsDocumentScanning.getClient(options)

        setContent {
            SmartCVTheme {
                val context = LocalContext.current
                val coroutineScope = rememberCoroutineScope()
                val documentRepository = DocumentRepository(context)
                val cvRepository = CvRepository(context)

                // State for tracking screen navigation
                var currentScreen by remember { mutableStateOf("home") }

                // State for scanned images
                val scannedImages = remember { mutableStateListOf<android.net.Uri>() }

                // State for documents
                var documents by remember { mutableStateOf<List<DocumentData>>(emptyList()) }

                // State for CVs
                var cvs by remember { mutableStateOf<List<CvData>>(emptyList()) }

                // State for current document
                var currentDocumentId by remember { mutableStateOf("") }
                var currentDocumentName by remember { mutableStateOf("") }

                // State for selected document
                var selectedDocument by remember { mutableStateOf<DocumentData?>(null) }

                // CV-related states
                var currentCvId by remember { mutableStateOf("") }
                var selectedCv by remember { mutableStateOf<CvData?>(null) }

                // State for bottom navigation
                var selectedTab by remember { mutableStateOf("home") }

                // State for scan option dialog
                var showScanOptionDialog by remember { mutableStateOf(false) }

                // Load documents and CVs on startup
                androidx.compose.runtime.LaunchedEffect(key1 = Unit) {
                    documents = documentRepository.loadAllDocuments()
                    cvs = cvRepository.loadAllCvs()
                }

                // Scanner launcher
                val scannerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartIntentSenderForResult(),
                    onResult = { result ->
                        if (result.resultCode == RESULT_OK) {
                            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)

                            // Clear existing images if coming from a new scan
                            if (currentScreen != "editDocument" && currentScreen != "cvEdit") {
                                scannedImages.clear()
                            }

                            // Add new scanned images to the list
                            scanResult?.pages?.map { it.imageUri }?.let { newImages ->
                                scannedImages.addAll(newImages)
                            }

                            // Auto-generate document name if not already set
                            if (currentDocumentName.isBlank()) {
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                currentDocumentName = "Scan_$timestamp"
                            }

                            // Generate a new document ID if needed
                            if (currentDocumentId.isBlank()) {
                                currentDocumentId = UUID.randomUUID().toString()
                            }

                            // Go to edit document screen or CV scan screen based on the current mode
                            if (currentScreen == "cvScan") {
                                // Stay on CV scan screen
                            } else if (currentScreen == "cvEdit") {
                                // Process the new images for OCR and update the CV
                                if (selectedCv != null) {
                                    coroutineScope.launch {
                                        try {
                                            // Extract text from new images
                                            val extractedText = com.example.smartcv.utils.OcrUtils.extractTextFromImages(context, scannedImages.toList())

                                            // Categorize text into CV sections
                                            val updatedCvData = com.example.smartcv.utils.OcrUtils.categorizeText(extractedText).copy(
                                                id = selectedCv!!.id,
                                                name = selectedCv!!.name,
                                                createdAt = selectedCv!!.createdAt,
                                                sourceImageUris = scannedImages.toList()
                                            )

                                            // Update selected CV
                                            selectedCv = updatedCvData
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            Toast.makeText(
                                                context,
                                                "Error processing images: ${e.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            } else {
                                // Go to edit document screen
                                currentScreen = "editDocument"
                            }
                        } else {
                            // User cancelled or error occurred
                            if (scannedImages.isEmpty() && currentScreen != "cvEdit") {
                                // If no images, go back to home
                                selectedTab = "home"
                                currentScreen = "home"
                            }
                            // Otherwise stay on current screen if we already have images
                        }
                    }
                )

                // Main app content
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppColors.backgroundGray
                ) {
                    Scaffold(
                        topBar = {
                            if (currentScreen != "profile") {
                                AppTopBar(
                                    currentScreen = currentScreen,
                                    onBackClick = {
                                        when (currentScreen) {
                                            "editDocument" -> {
                                                // Save document before going back
                                                if (scannedImages.isNotEmpty()) {
                                                    coroutineScope.launch {
                                                        documentRepository.saveDocumentMetadata(
                                                            currentDocumentId,
                                                            currentDocumentName,
                                                            scannedImages.toList()
                                                        )

                                                        // Reload documents
                                                        documents = documentRepository.loadAllDocuments()

                                                        // Clear current document data
                                                        scannedImages.clear()
                                                        currentDocumentId = ""
                                                        currentDocumentName = ""
                                                    }
                                                }
                                                selectedTab = "home"
                                                currentScreen = "home"
                                            }
                                            "viewDocument" -> {
                                                currentScreen = "library"
                                            }
                                            "cvScan" -> {
                                                // Go back to home from CV scan
                                                scannedImages.clear()
                                                selectedTab = "home"
                                                currentScreen = "home"
                                            }
                                            "cvEdit" -> {
                                                // Go back to CV scan from CV edit
                                                currentScreen = "cvScan"
                                            }
                                            "cvView" -> {
                                                // Go back to library from CV view
                                                currentScreen = "library"
                                            }
                                        }
                                    },
                                    onProfileClick = { currentScreen = "profile" }
                                )
                            }
                        },
                        bottomBar = {
                            AppBottomNavigation(
                                selectedTab = selectedTab,
                                onTabSelected = { id ->
                                    // Don't allow navigation away from edit screen without confirmation
                                    if (currentScreen == "editDocument" && scannedImages.isNotEmpty() && id != "scan") {
                                        // Save document before navigating away
                                        coroutineScope.launch {
                                            documentRepository.saveDocumentMetadata(
                                                currentDocumentId,
                                                currentDocumentName,
                                                scannedImages.toList()
                                            )

                                            // Reload documents
                                            documents = documentRepository.loadAllDocuments()

                                            // Clear current document data
                                            scannedImages.clear()
                                            currentDocumentId = ""
                                            currentDocumentName = ""

                                            // Navigate to selected tab
                                            selectedTab = id
                                            currentScreen = id
                                        }
                                        return@AppBottomNavigation
                                    }

                                    selectedTab = id
                                    when (id) {
                                        "home" -> currentScreen = "home"
                                        "library" -> currentScreen = "library"
                                        "scan" -> {
                                            selectedTab = "scan"

                                            // Show scan options dialog
                                            showScanOptionDialog = true
                                        }
                                        "generate" -> currentScreen = "generate"
                                        "settings" -> currentScreen = "settings"
                                    }
                                }
                            )
                        },
                        containerColor = AppColors.backgroundGray,
                        floatingActionButton = {
                            // Show FAB only on edit screen, CV scan screen, or CV edit screen
                            when (currentScreen) {
                                "editDocument" -> {
                                    FloatingActionButton(
                                        onClick = {
                                            scanner.getStartScanIntent(context as Activity)
                                                .addOnSuccessListener { intentSender ->
                                                    scannerLauncher.launch(
                                                        IntentSenderRequest.Builder(intentSender).build()
                                                    )
                                                }
                                                .addOnFailureListener { exception ->
                                                    Toast.makeText(
                                                        context,
                                                        exception.message,
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                        },
                                        containerColor = AppColors.primaryTeal,
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add Pages",
                                            tint = Color.White
                                        )
                                    }
                                }
                                "cvScan" -> {
                                    FloatingActionButton(
                                        onClick = {
                                            scanner.getStartScanIntent(context as Activity)
                                                .addOnSuccessListener { intentSender ->
                                                    scannerLauncher.launch(
                                                        IntentSenderRequest.Builder(intentSender).build()
                                                    )
                                                }
                                                .addOnFailureListener { exception ->
                                                    Toast.makeText(
                                                        context,
                                                        exception.message,
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                        },
                                        containerColor = AppColors.primaryTeal,
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add Pages",
                                            tint = Color.White
                                        )
                                    }
                                }
                                "cvEdit" -> {
                                    FloatingActionButton(
                                        onClick = {
                                            scanner.getStartScanIntent(context as Activity)
                                                .addOnSuccessListener { intentSender ->
                                                    scannerLauncher.launch(
                                                        IntentSenderRequest.Builder(intentSender).build()
                                                    )
                                                }
                                                .addOnFailureListener { exception ->
                                                    Toast.makeText(
                                                        context,
                                                        exception.message,
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                        },
                                        containerColor = AppColors.primaryTeal,
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add Pages for OCR",
                                            tint = Color.White
                                        )
                                    }
                                }
                                else -> {}
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentScreen) {
                                "home" -> {
                                    HomeScreen(
                                        documents = documents,
                                        onDocumentClick = { document ->
                                            selectedDocument = document

                                            // Load document images into scannedImages
                                            scannedImages.clear()
                                            scannedImages.addAll(document.imageUris)

                                            // Set current document data
                                            currentDocumentId = document.id
                                            currentDocumentName = document.name

                                            // Navigate to view document screen
                                            currentScreen = "viewDocument"
                                        }
                                    )
                                }

                                "library" -> {
                                    DocumentListScreen(
                                        documents = documents,
                                        cvs = cvs,
                                        onDocumentClick = { document ->
                                            selectedDocument = document

                                            // Load document images into scannedImages
                                            scannedImages.clear()
                                            scannedImages.addAll(document.imageUris)

                                            // Set current document data
                                            currentDocumentId = document.id
                                            currentDocumentName = document.name

                                            // Navigate to view document screen
                                            currentScreen = "viewDocument"
                                        },
                                        onCvClick = { cv ->
                                            selectedCv = cv
                                            currentCvId = cv.id

                                            // Navigate to CV view screen
                                            currentScreen = "cvView"
                                        },
                                        onDeleteDocument = { document ->
                                            coroutineScope.launch {
                                                if (documentRepository.deleteDocument(document.id)) {
                                                    // Reload documents
                                                    documents = documentRepository.loadAllDocuments()

                                                    Toast.makeText(
                                                        context,
                                                        "Document deleted",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        },
                                        onDeleteCv = { cv ->
                                            coroutineScope.launch {
                                                if (cvRepository.deleteCv(cv.id)) {
                                                    // Reload CVs
                                                    cvs = cvRepository.loadAllCvs()

                                                    Toast.makeText(
                                                        context,
                                                        "CV deleted",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        },
                                        onShareDocument = { document ->
                                            coroutineScope.launch {
                                                documentRepository.shareDocument(context, document)
                                            }
                                        },
                                        onShareCv = { cv ->
                                            coroutineScope.launch {
                                                cvRepository.shareCvAsPdf(context, cv)
                                            }
                                        }
                                    )
                                }

                                "profile" -> {
                                    ProfileScreen(
                                        onBackClick = {
                                            currentScreen = selectedTab
                                        }
                                    )
                                }

                                "viewDocument" -> {
                                    DocumentViewScreen(
                                        images = scannedImages.toList(),
                                        documentName = currentDocumentName,
                                        onBackClick = {
                                            currentScreen = "library"
                                        },
                                        onEditClick = {
                                            // Navigate to edit document screen
                                            currentScreen = "editDocument"
                                        },
                                        onShareDocument = {
                                            coroutineScope.launch {
                                                documentRepository.shareDocument(
                                                    context,
                                                    scannedImages.toList(),
                                                    currentDocumentName
                                                )
                                            }
                                        },
                                        onDownloadDocument = {
                                            coroutineScope.launch {
                                                documentRepository.downloadDocument(
                                                    context,
                                                    scannedImages.toList(),
                                                    currentDocumentName
                                                )
                                            }
                                        }
                                    )
                                }

                                "editDocument" -> {
                                    EditDocumentScreen(
                                        images = scannedImages,
                                        currentName = currentDocumentName,
                                        documentId = currentDocumentId,
                                        onNameChange = { newName ->
                                            currentDocumentName = newName
                                        },
                                        onReorderImages = { fromIndex, toIndex ->
                                            if (fromIndex != toIndex && fromIndex in scannedImages.indices && toIndex in scannedImages.indices) {
                                                val image = scannedImages.removeAt(fromIndex)
                                                scannedImages.add(toIndex, image)
                                            }
                                        },
                                        onDeleteImage = { index ->
                                            if (index in scannedImages.indices) {
                                                scannedImages.removeAt(index)
                                            }
                                        },
                                        onSaveDocument = {
                                            // Only proceed if we have images
                                            if (scannedImages.isNotEmpty()) {
                                                coroutineScope.launch {
                                                    // Save document metadata
                                                    documentRepository.saveDocumentMetadata(
                                                        currentDocumentId,
                                                        currentDocumentName,
                                                        scannedImages.toList()
                                                    )

                                                    // Reload documents
                                                    documents = documentRepository.loadAllDocuments()

                                                    Toast.makeText(
                                                        context,
                                                        "Document saved successfully",
                                                        Toast.LENGTH_SHORT
                                                    ).show()

                                                    // Navigate to home screen
                                                    selectedTab = "home"
                                                    currentScreen = "home"
                                                }
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "No images to save",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        },
                                        onShareDocument = { imageList, name ->
                                            coroutineScope.launch {
                                                documentRepository.shareDocument(context, imageList, name)
                                            }
                                        },
                                        onDownloadDocument = { imageList, name ->
                                            coroutineScope.launch {
                                                documentRepository.downloadDocument(context, imageList, name)
                                            }
                                        }
                                    )
                                }

                                "cvScan" -> {
                                    CvScanScreen(
                                        images = scannedImages.toList(),
                                        onReorderImages = { fromIndex, toIndex ->
                                            if (fromIndex != toIndex && fromIndex in scannedImages.indices && toIndex in scannedImages.indices) {
                                                val image = scannedImages.removeAt(fromIndex)
                                                scannedImages.add(toIndex, image)
                                            }
                                        },
                                        onDeleteImage = { index ->
                                            if (index in scannedImages.indices) {
                                                scannedImages.removeAt(index)
                                            }
                                        },
                                        onProceedToOcr = { cvData ->
                                            // Set the current CV data and navigate to CV edit screen
                                            selectedCv = cvData
                                            currentScreen = "cvEdit"
                                        },
                                        coroutineScope = coroutineScope
                                    )
                                }

                                "cvEdit" -> {
                                    selectedCv?.let { cv ->
                                        CvEditScreen(
                                            cvData = cv,
                                            onSave = { updatedCv ->
                                                coroutineScope.launch {
                                                    // Save CV data
                                                    cvRepository.saveCvData(updatedCv)

                                                    // Update selected CV
                                                    selectedCv = updatedCv
                                                    currentCvId = updatedCv.id

                                                    // Reload CVs
                                                    cvs = cvRepository.loadAllCvs()

                                                    // Navigate to CV view screen
                                                    currentScreen = "cvView"

                                                    Toast.makeText(
                                                        context,
                                                        "CV saved successfully",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            },
                                            onAddMoreImages = {
                                                // Start scanning to add more pages
                                                scanner.getStartScanIntent(context as Activity)
                                                    .addOnSuccessListener { intentSender ->
                                                        scannerLauncher.launch(
                                                            IntentSenderRequest.Builder(intentSender).build()
                                                        )
                                                    }
                                                    .addOnFailureListener { exception ->
                                                        Toast.makeText(
                                                            context,
                                                            exception.message,
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                            }
                                        )
                                    } ?: run {
                                        // If no CV data, go back to home
                                        selectedTab = "home"
                                        currentScreen = "home"
                                    }
                                }

                                "cvView" -> {
                                    val cvToView = selectedCv ?: run {
                                        // If no selected CV, try to load from ID
                                        if (currentCvId.isNotEmpty()) {
                                            // We can't directly load here, so we'll show a loading state
                                            // and trigger the load in a LaunchedEffect
                                            var isLoading by remember { mutableStateOf(true) }
                                            var loadedCv by remember { mutableStateOf<CvData?>(null) }

                                            androidx.compose.runtime.LaunchedEffect(currentCvId) {
                                                val cv = cvRepository.loadCv(currentCvId)
                                                if (cv != null) {
                                                    selectedCv = cv
                                                    loadedCv = cv
                                                } else {
                                                    // If CV not found, go back to home
                                                    selectedTab = "home"
                                                    currentScreen = "home"
                                                }
                                                isLoading = false
                                            }

                                            if (isLoading) {
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    androidx.compose.material3.CircularProgressIndicator(
                                                        color = AppColors.primaryTeal
                                                    )
                                                }
                                                return@Box
                                            }

                                            loadedCv
                                        } else {
                                            // If no CV ID, go back to home
                                            selectedTab = "home"
                                            currentScreen = "home"
                                            null
                                        }
                                    }

                                    if (cvToView != null) {
                                        CvViewScreen(
                                            cvData = cvToView,
                                            onEditClick = {
                                                currentScreen = "cvEdit"
                                            },
                                            onShareClick = {
                                                coroutineScope.launch {
                                                    cvRepository.shareCvAsPdf(context, cvToView)
                                                }
                                            },
                                            onDownloadClick = {
                                                coroutineScope.launch {
                                                    cvRepository.downloadCvAsPdf(context, cvToView)
                                                }
                                            }
                                        )
                                    }
                                }

                                "generate" -> {
                                    GenerateScreen()
                                }

                                "settings" -> {
                                    SettingsScreen()
                                }
                            }
                        }
                    }

                    // Scan option dialog
                    if (showScanOptionDialog) {
                        AlertDialog(
                            onDismissRequest = { showScanOptionDialog = false },
                            title = {
                                Text(
                                    text = "Scan Options",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            },
                            text = {
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Choose how you want to scan your document",
                                        fontSize = 14.sp
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                                    ) {
                                        // Regular scan option
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Button(
                                                onClick = {
                                                    showScanOptionDialog = false

                                                    // Generate a timestamp-based filename automatically
                                                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                                    currentDocumentName = "Scan_$timestamp"
                                                    currentDocumentId = UUID.randomUUID().toString()

                                                    // Start scanning directly
                                                    scanner.getStartScanIntent(context as Activity)
                                                        .addOnSuccessListener { intentSender ->
                                                            scannerLauncher.launch(
                                                                IntentSenderRequest.Builder(intentSender).build()
                                                            )
                                                        }
                                                        .addOnFailureListener { exception ->
                                                            Toast.makeText(
                                                                context,
                                                                exception.message,
                                                                Toast.LENGTH_LONG
                                                            ).show()
                                                            // Redirect to home on failure
                                                            selectedTab = "home"
                                                            currentScreen = "home"
                                                        }
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(100.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = AppColors.primaryTeal
                                                ),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DocumentScanner,
                                                        contentDescription = "Regular Scan",
                                                        modifier = Modifier.size(32.dp)
                                                    )

                                                    Spacer(modifier = Modifier.height(8.dp))

                                                    Text("Regular Scan")
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Text(
                                                text = "Scan and save document",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                        }

                                        // OCR scan option
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(start = 8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Button(
                                                onClick = {
                                                    showScanOptionDialog = false

                                                    // Clear any existing images
                                                    scannedImages.clear()

                                                    // Set current screen to CV scan
                                                    currentScreen = "cvScan"

                                                    // Start scanning
                                                    scanner.getStartScanIntent(context as Activity)
                                                        .addOnSuccessListener { intentSender ->
                                                            scannerLauncher.launch(
                                                                IntentSenderRequest.Builder(intentSender).build()
                                                            )
                                                        }
                                                        .addOnFailureListener { exception ->
                                                            Toast.makeText(
                                                                context,
                                                                exception.message,
                                                                Toast.LENGTH_LONG
                                                            ).show()
                                                            // Redirect to home on failure
                                                            selectedTab = "home"
                                                            currentScreen = "home"
                                                        }
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(100.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = AppColors.primaryTeal
                                                ),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.TextFields,
                                                        contentDescription = "OCR Scan",
                                                        modifier = Modifier.size(32.dp)
                                                    )

                                                    Spacer(modifier = Modifier.height(8.dp))

                                                    Text("OCR Scan")
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Text(
                                                text = "Extract text from CV",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            },
                            confirmButton = {},
                            dismissButton = {
                                Button(
                                    onClick = {
                                        showScanOptionDialog = false
                                        selectedTab = "home"
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Gray
                                    )
                                ) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    // Handle back button presses
                    BackHandler(enabled = currentScreen == "editDocument" || currentScreen == "viewDocument" || currentScreen == "cvScan" || currentScreen == "cvEdit" || currentScreen == "cvView") {
                        when (currentScreen) {
                            "editDocument" -> {
                                // Save document before going back
                                if (scannedImages.isNotEmpty()) {
                                    coroutineScope.launch {
                                        documentRepository.saveDocumentMetadata(
                                            currentDocumentId,
                                            currentDocumentName,
                                            scannedImages.toList()
                                        )

                                        // Reload documents
                                        documents = documentRepository.loadAllDocuments()

                                        // Clear current document data
                                        scannedImages.clear()
                                        currentDocumentId = ""
                                        currentDocumentName = ""

                                        // Go back to home
                                        selectedTab = "home"
                                        currentScreen = "home"
                                    }
                                } else {
                                    selectedTab = "home"
                                    currentScreen = "home"
                                }
                            }
                            "viewDocument" -> {
                                // When back is pressed from viewDocument, go to library
                                currentScreen = "library"
                            }
                            "cvScan" -> {
                                // When back is pressed from cvScan, go to home
                                scannedImages.clear()
                                selectedTab = "home"
                                currentScreen = "home"
                            }
                            "cvEdit" -> {
                                // When back is pressed from cvEdit, go to cvScan
                                currentScreen = "cvScan"
                            }
                            "cvView" -> {
                                // When back is pressed from cvView, go to library
                                currentScreen = "library"
                            }
                        }
                    }
                }
            }
        }
    }
}
