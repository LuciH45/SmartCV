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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import com.example.smartcv.utils.AppColors
import com.example.smartcv.utils.rememberAsyncImagePainter
import kotlin.math.abs

/**
 * Screen for editing a document
 *
 * @param images List of image URIs in the document
 * @param currentName Current name of the document
 * @param documentId ID of the document
 * @param onNameChange Callback when name is changed
 * @param onReorderImages Callback when images are reordered
 * @param onDeleteImage Callback when an image is deleted
 * @param onSaveDocument Callback when document is saved
 * @param onShareDocument Callback when document is shared
 * @param onDownloadDocument Callback when document is downloaded
 */
@Composable
fun EditDocumentScreen(
    images: List<android.net.Uri>,
    currentName: String,
    documentId: String,
    onNameChange: (String) -> Unit,
    onReorderImages: (Int, Int) -> Unit,
    onDeleteImage: (Int) -> Unit,
    onSaveDocument: () -> Unit,
    onShareDocument: (List<android.net.Uri>, String) -> Unit,
    onDownloadDocument: (List<android.net.Uri>, String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(currentName) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Document name editor (compact version with pencil icon)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isEditing) {
                // Editing mode - use TextField
                TextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    placeholder = { Text("Document name") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        cursorColor = Color(0xFF2980B9), // Dark blue cursor
                        focusedIndicatorColor = Color(0xFF2980B9), // Dark blue underline when focused
                        unfocusedIndicatorColor = Color.LightGray,
                    ),
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        onNameChange(tempName)
                        isEditing = false
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Confirm",
                        tint = AppColors.primaryTeal
                    )
                }
            } else {
                // Display mode
                Text(
                    text = currentName,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp,
                    color = AppColors.darkGray,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        tempName = currentName
                        isEditing = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Name",
                        tint = AppColors.primaryTeal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons - Share and Download
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { onShareDocument(images.toList(), currentName) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.primaryTeal
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share")
            }

            Button(
                onClick = { onDownloadDocument(images.toList(), currentName) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.primaryTeal
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Download")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                Spacer(modifier = Modifier.width(8.dp))
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
                        text = "Use the + button to add images",
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
                                        // Swipe left (negative swipeAmount) moves to left position (index - 1)
                                        // Swipe right (positive swipeAmount) moves to right position (index + 1)
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
    }
}

