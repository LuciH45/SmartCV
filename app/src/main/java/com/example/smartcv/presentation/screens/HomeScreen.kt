package com.example.smartcv.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
 * Home screen displaying recent documents and CVs
 *
 * @param documents List of documents to display
 * @param cvs List of CVs to display
 * @param onDocumentClick Callback when a document is clicked
 * @param onCvClick Callback when a CV is clicked
 */
@Composable
fun HomeScreen(
    documents: List<DocumentData>,
    cvs: List<CvData>,
    onDocumentClick: (DocumentData) -> Unit,
    onCvClick: (CvData) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Recent Activity",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.darkGray,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Show recent documents and CVs
        if (documents.isEmpty() && cvs.isEmpty()) {
            EmptyHomeState()
        } else {
            // Combine and sort by date
            val recentItems = (documents.map { RecentItem.Document(it) } + 
                             cvs.map { RecentItem.Cv(it) })
                .sortedByDescending { 
                    when (it) {
                        is RecentItem.Document -> it.document.updatedAt
                        is RecentItem.Cv -> it.cv.updatedAt
                    }
                }
                .take(5)

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recentItems) { item ->
                    when (item) {
                        is RecentItem.Document -> RecentActivityItem(
                            document = item.document,
                            onClick = { onDocumentClick(item.document) }
                        )
                        is RecentItem.Cv -> RecentActivityItem(
                            cv = item.cv,
                            onClick = { onCvClick(item.cv) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Sealed class to represent different types of recent items
 */
sealed class RecentItem {
    data class Document(val document: DocumentData) : RecentItem()
    data class Cv(val cv: CvData) : RecentItem()
}

/**
 * Empty state component for the home screen
 */
@Composable
fun EmptyHomeState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.Description,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No recent activity",
            fontSize = 16.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Scan a document or create a CV to get started",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Component for displaying a recent document item
 *
 * @param document Document to display
 * @param onClick Callback when the item is clicked
 */
@Composable
fun RecentActivityItem(document: DocumentData, onClick: () -> Unit) {
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
                androidx.compose.foundation.Image(
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
                    text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        .format(Date(document.updatedAt)),
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "View",
                tint = Color.Gray
            )
        }
    }
}

/**
 * Component for displaying a recent CV item
 *
 * @param cv CV to display
 * @param onClick Callback when the item is clicked
 */
@Composable
fun RecentActivityItem(cv: CvData, onClick: () -> Unit) {
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
            if (cv.sourceImageUris.isNotEmpty()) {
                androidx.compose.foundation.Image(
                    painter = rememberAsyncImagePainter(cv.sourceImageUris.first()),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Person,
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
                    text = cv.name,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = AppColors.darkGray
                )

                Text(
                    text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        .format(Date(cv.updatedAt)),
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "View",
                tint = Color.Gray
            )
        }
    }
}

