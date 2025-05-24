package com.example.smartcv.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartcv.data.model.CvData
import com.example.smartcv.data.model.DocumentData
import com.example.smartcv.utils.AppColors
import com.example.smartcv.utils.rememberAsyncImagePainter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen displaying a list of all documents and CVs
 *
 * @param documents List of documents to display
 * @param cvs List of CVs to display
 * @param onDocumentClick Callback when a document is clicked
 * @param onCvClick Callback when a CV is clicked
 * @param onDeleteDocument Callback when a document is deleted
 * @param onDeleteCv Callback when a CV is deleted
 * @param onShareDocument Callback when a document is shared
 * @param onShareCv Callback when a CV is shared
 */
@Composable
fun DocumentListScreen(
    documents: List<DocumentData>,
    cvs: List<CvData> = emptyList(),
    onDocumentClick: (DocumentData) -> Unit,
    onCvClick: (CvData) -> Unit = {},
    onDeleteDocument: (DocumentData) -> Unit,
    onDeleteCv: (CvData) -> Unit = {},
    onShareDocument: (DocumentData) -> Unit,
    onShareCv: (CvData) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "All Documents",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.darkGray,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        if (documents.isEmpty() && cvs.isEmpty()) {
            EmptyLibraryState()
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Display regular documents
                if (documents.isNotEmpty()) {
                    item {
                        Text(
                            text = "Documents",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.darkGray,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(documents) { document ->
                        DocumentListItem(
                            document = document,
                            onClick = { onDocumentClick(document) },
                            onDelete = { onDeleteDocument(document) },
                            onShare = { onShareDocument(document) }
                        )
                    }
                }

                // Display CVs
                if (cvs.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "CVs",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.darkGray,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(cvs) { cv ->
                        CvListItem(
                            cv = cv,
                            onClick = { onCvClick(cv) },
                            onDelete = { onDeleteCv(cv) },
                            onShare = { onShareCv(cv) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Empty state component for the library screen
 */
@Composable
fun EmptyLibraryState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.MenuBook,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your library is empty",
            fontSize = 16.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Scanned documents will appear here",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Component for displaying a document in the list
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentListItem(
    document: DocumentData,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    var showOptions by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Show first image as thumbnail if available
            if (document.imageUris.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(document.imageUris.first()),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    tint = AppColors.primaryTeal,
                    modifier = Modifier.size(48.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = document.name,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = AppColors.darkGray
                )

                Text(
                    text = "${document.imageUris.size} ${if (document.imageUris.size == 1) "image" else "images"} • ${
                        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(document.updatedAt))
                    }",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }

            Box {
                IconButton(onClick = { showOptions = !showOptions }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More Options",
                        tint = Color.Gray
                    )
                }

                DropdownMenu(
                    expanded = showOptions,
                    onDismissRequest = { showOptions = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    DropdownMenuItem(
                        text = { Text("Share", color = AppColors.darkGray) },
                        onClick = {
                            onShare()
                            showOptions = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = null,
                                tint = AppColors.primaryTeal
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = AppColors.darkGray) },
                        onClick = {
                            onDelete()
                            showOptions = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = null,
                                tint = Color.Red
                            )
                        }
                    )
                }
            }
        }
    }
}

/**
 * Component for displaying a CV in the list
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CvListItem(
    cv: CvData,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    var showOptions by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // CV icon
            Icon(
                imageVector = Icons.Filled.TextFields,
                contentDescription = null,
                tint = AppColors.primaryTeal,
                modifier = Modifier.size(48.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = cv.name,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = AppColors.darkGray
                )

                Text(
                    text = "CV • ${
                        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(cv.updatedAt))
                    }",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }

            Box {
                IconButton(onClick = { showOptions = !showOptions }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More Options",
                        tint = Color.Gray
                    )
                }

                DropdownMenu(
                    expanded = showOptions,
                    onDismissRequest = { showOptions = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    DropdownMenuItem(
                        text = { Text("Share", color = AppColors.darkGray) },
                        onClick = {
                            onShare()
                            showOptions = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = null,
                                tint = AppColors.primaryTeal
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = AppColors.darkGray) },
                        onClick = {
                            onDelete()
                            showOptions = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = null,
                                tint = Color.Red
                            )
                        }
                    )
                }
            }
        }
    }
}

