package com.example.smartcv.presentation.screens

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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartcv.data.model.CvData
import com.example.smartcv.utils.AppColors
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Screen for viewing a CV
 */
@Composable
fun CvViewScreen(
    cvData: CvData,
    onEditClick: () -> Unit,
    onShareClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onSyncClick: (String) -> Unit,
    onViewMagnetoProfile: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            // CV Title
            Text(
                text = cvData.name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.darkGray,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onShareClick,
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
                    onClick = onDownloadClick,
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

            // Magneto Sync buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { onSyncClick(cvData.id) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.primaryTeal
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Sincronizar con Magneto")
                }

                Button(
                    onClick = { onViewMagnetoProfile(cvData.id) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.primaryTeal
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Ver en Magneto")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Personal Information Section
            SectionHeader(
                title = "Personal Information",
                icon = Icons.Default.Person
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
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
                    if (cvData.personalInfo.fullName.isNotEmpty()) {
                        InfoRow(label = "Name", value = cvData.personalInfo.fullName)
                    }

                    if (cvData.personalInfo.email.isNotEmpty()) {
                        InfoRow(label = "Email", value = cvData.personalInfo.email)
                    }

                    if (cvData.personalInfo.phone.isNotEmpty()) {
                        InfoRow(label = "Phone", value = cvData.personalInfo.phone)
                    }

                    if (cvData.personalInfo.address.isNotEmpty()) {
                        InfoRow(label = "Address", value = cvData.personalInfo.address)
                    }

                    if (cvData.personalInfo.summary.isNotEmpty()) {
                        Text(
                            text = "Summary",
                            fontWeight = FontWeight.Medium,
                            color = AppColors.darkGray
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = cvData.personalInfo.summary,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Education Section
            if (cvData.education.isNotEmpty()) {
                SectionHeader(
                    title = "Education",
                    icon = Icons.Default.School
                )

                cvData.education.forEach { education ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
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
                            if (education.institution.isNotEmpty()) {
                                Text(
                                    text = education.institution,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.darkGray
                                )
                            }

                            if (education.degree.isNotEmpty() || education.fieldOfStudy.isNotEmpty()) {
                                val degreeField = buildString {
                                    if (education.degree.isNotEmpty()) {
                                        append(education.degree)
                                        if (education.fieldOfStudy.isNotEmpty()) {
                                            append(" in ")
                                        }
                                    }
                                    if (education.fieldOfStudy.isNotEmpty()) {
                                        append(education.fieldOfStudy)
                                    }
                                }

                                Text(
                                    text = degreeField,
                                    color = AppColors.darkGray
                                )
                            }

                            if (education.startDate.isNotEmpty() || education.endDate.isNotEmpty()) {
                                val dateRange = buildString {
                                    if (education.startDate.isNotEmpty()) {
                                        append(education.startDate)
                                    }
                                    if (education.startDate.isNotEmpty() && education.endDate.isNotEmpty()) {
                                        append(" - ")
                                    }
                                    if (education.endDate.isNotEmpty()) {
                                        append(education.endDate)
                                    }
                                }

                                Text(
                                    text = dateRange,
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }

                            if (education.description.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = education.description,
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Experience Section
            if (cvData.experience.isNotEmpty()) {
                SectionHeader(
                    title = "Work Experience",
                    icon = Icons.Default.Work
                )

                cvData.experience.forEach { experience ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
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
                            if (experience.company.isNotEmpty()) {
                                Text(
                                    text = experience.company,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.darkGray
                                )
                            }

                            if (experience.position.isNotEmpty()) {
                                Text(
                                    text = experience.position,
                                    color = AppColors.darkGray
                                )
                            }

                            if (experience.startDate.isNotEmpty() || experience.endDate.isNotEmpty()) {
                                val dateRange = buildString {
                                    if (experience.startDate.isNotEmpty()) {
                                        append(experience.startDate)
                                    }
                                    if (experience.startDate.isNotEmpty() && experience.endDate.isNotEmpty()) {
                                        append(" - ")
                                    }
                                    if (experience.endDate.isNotEmpty()) {
                                        append(experience.endDate)
                                    }
                                }

                                Text(
                                    text = dateRange,
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }

                            if (experience.description.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = experience.description,
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Skills & Abilities Section
            if (cvData.abilities.isNotEmpty()) {
                SectionHeader(
                    title = "Skills & Abilities",
                    icon = Icons.Default.Star
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
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
                        cvData.abilities.forEach { ability ->
                            Text(
                                text = "• $ability",
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Created/Updated info
            Spacer(modifier = Modifier.height(24.dp))

            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            Text(
                text = "Created: ${dateFormat.format(cvData.createdAt)}",
                color = Color.Gray,
                fontSize = 12.sp
            )

            Text(
                text = "Last Updated: ${dateFormat.format(cvData.updatedAt)}",
                color = Color.Gray,
                fontSize = 12.sp
            )

            // Extra space at bottom for FAB
            Spacer(modifier = Modifier.height(80.dp))
        }

        // Edit FAB
        FloatingActionButton(
            onClick = onEditClick,
            containerColor = AppColors.primaryTeal,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit CV",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(AppColors.primaryTeal.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppColors.primaryTeal,
                modifier = Modifier.size(18.dp)
            )
        }

        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = AppColors.darkGray,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        )
    }

    HorizontalDivider(
        color = Color.LightGray,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            fontWeight = FontWeight.Medium,
            color = AppColors.darkGray
        )

        Text(
            text = value,
            color = Color.Gray
        )
    }

    Spacer(modifier = Modifier.height(8.dp))
}

