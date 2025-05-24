package com.example.smartcv.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartcv.data.model.CvData
import com.example.smartcv.utils.AppColors
import com.example.smartcv.utils.OcrUtils
import com.example.smartcv.utils.rememberAsyncImagePainter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Screen for preparing scanned images for OCR processing
 */
@Composable
fun CvScanScreen(
    images: List<android.net.Uri>,
    onReorderImages: (Int, Int) -> Unit,
    onDeleteImage: (Int) -> Unit,
    onProceedToOcr: (CvData) -> Unit,
    coroutineScope: CoroutineScope
) {
    var isProcessing by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Title
        Text(
            text = "Prepare Images for CV Extraction",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.darkGray,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Instructions
        Text(
            text = "Arrange your CV images in the correct order. The text will be extracted and categorized automatically.",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Images count with hint about swipe-to-reorder
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${images.size} ${if (images.size == 1) "Image" else "Images"}",
                fontWeight = FontWeight.Medium,
                color = AppColors.darkGray
            )

            if (images.size > 1) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "(Swipe to reorder images)",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontStyle = FontStyle.Italic
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (images.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "No images yet",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Use the scan button to add images",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Image grid with swipe-to-reorder
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(images) { index, uri ->
                    // Using draggable for reordering
                    var swipeAmount by remember { mutableStateOf(0f) }
                    val swipeThreshold = 50f

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.75f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = 1.dp,
                                color = Color.LightGray,
                                shape = RoundedCornerShape(8.dp)
                            )
                            // Use draggable for reordering
                            .draggable(
                                orientation = Orientation.Horizontal,
                                state = rememberDraggableState { delta ->
                                    swipeAmount += delta
                                },
                                onDragStopped = {
                                    if (abs(swipeAmount) > swipeThreshold) {
                                        // Determine the direction of the swipe
                                        val direction = if (swipeAmount < 0) -1 else 1

                                        // Calculate the new position
                                        val newPosition = (index + direction).coerceIn(0, images.size - 1)

                                        // Only reorder if the position actually changes
                                        if (newPosition != index) {
                                            onReorderImages(index, newPosition)

                                            // Show feedback
                                            val message = if (direction < 0) "Image moved left" else "Image moved right"
                                            Toast.makeText(
                                                context,
                                                message,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                    // Reset swipe amount
                                    swipeAmount = 0f
                                }
                            )
                    ) {
                        // Image
                        Image(
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            painter = rememberAsyncImagePainter(uri),
                            contentDescription = "Document Image ${index + 1}"
                        )

                        // Page number indicator
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .background(AppColors.primaryTeal, CircleShape)
                                .size(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Delete button
                        IconButton(
                            onClick = { onDeleteImage(index) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(32.dp)
                                .background(Color.White.copy(alpha = 0.8f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Delete",
                                tint = Color.Red,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Process button
        Button(
            onClick = {
                if (images.isNotEmpty()) {
                    isProcessing = true
                    coroutineScope.launch {
                        try {
                            // Extract text from images
                            val extractedText = OcrUtils.extractTextFromImages(context, images)

                            // Categorize text into CV sections using AI
                            val cvData = OcrUtils.categorizeTextWithAI(extractedText).copy(
                                sourceImageUris = images
                            )

                            // Proceed to edit screen
                            onProceedToOcr(cvData)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(
                                context,
                                "Error processing images: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        } finally {
                            isProcessing = false
                        }
                    }
                } else {
                    Toast.makeText(
                        context,
                        "Please add at least one image",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            enabled = !isProcessing && images.isNotEmpty(),
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
                Spacer(modifier = Modifier.size(8.dp))
                Text("Processing...")
            } else {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Extract Text & Continue")
            }
        }
    }
}

