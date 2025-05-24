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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartcv.data.model.CvData
import com.example.smartcv.data.model.Education
import com.example.smartcv.data.model.Experience
import com.example.smartcv.data.model.PersonalInfo
import com.example.smartcv.utils.AppColors

/**
 * Screen for editing CV data extracted from OCR
 */
@Composable
fun CvEditScreen(
    cvData: CvData,
    onSave: (CvData) -> Unit,
    onAddMoreImages: () -> Unit = {},
    onSaveAndView: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    // State for CV data
    var name by remember { mutableStateOf(cvData.name) }
    var personalInfo by remember { mutableStateOf(cvData.personalInfo) }
    val education = remember { mutableStateListOf<Education>().apply { addAll(cvData.education) } }
    val experience = remember { mutableStateListOf<Experience>().apply { addAll(cvData.experience) } }
    val abilities = remember { mutableStateListOf<String>().apply { addAll(cvData.abilities) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Title
        Text(
            text = "Edit CV Information",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.darkGray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // CV Name
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("CV Name") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.primaryTeal,
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = AppColors.primaryTeal,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Personal Information Section
        SectionHeader(
            title = "Personal Information",
            icon = Icons.Default.Person
        )

        PersonalInfoEditor(
            personalInfo = personalInfo,
            onPersonalInfoChange = { personalInfo = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Education Section
        SectionHeader(
            title = "Education",
            icon = Icons.Default.School,
            onAddClick = {
                education.add(Education())
            }
        )

        EducationEditor(
            educationList = education,
            onRemove = { index ->
                if (index in education.indices) {
                    education.removeAt(index)
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Experience Section
        SectionHeader(
            title = "Work Experience",
            icon = Icons.Default.Work,
            onAddClick = {
                experience.add(Experience())
            }
        )

        ExperienceEditor(
            experienceList = experience,
            onRemove = { index ->
                if (index in experience.indices) {
                    experience.removeAt(index)
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Abilities Section
        SectionHeader(
            title = "Skills & Abilities",
            icon = Icons.Default.Star,
            onAddClick = {
                abilities.add("")
            }
        )

        AbilitiesEditor(
            abilitiesList = abilities,
            onRemove = { index ->
                if (index in abilities.indices) {
                    abilities.removeAt(index)
                }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Save Button
        Button(
            onClick = {
                val updatedCvData = cvData.copy(
                    name = name,
                    personalInfo = personalInfo,
                    education = education.toList(),
                    experience = experience.toList(),
                    abilities = abilities.toList(),
                    updatedAt = System.currentTimeMillis()
                )
                onSave(updatedCvData)
                onSaveAndView()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.primaryTeal
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save CV")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SectionHeader(
    title: String,
    icon: ImageVector,
    onAddClick: (() -> Unit)? = null
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

        if (onAddClick != null) {
            IconButton(
                onClick = onAddClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    tint = AppColors.primaryTeal
                )
            }
        }
    }

    Divider(
        color = Color.LightGray,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalInfoEditor(
    personalInfo: PersonalInfo,
    onPersonalInfoChange: (PersonalInfo) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = personalInfo.fullName,
            onValueChange = { onPersonalInfoChange(personalInfo.copy(fullName = it)) },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.primaryTeal,
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = AppColors.primaryTeal,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = personalInfo.email,
            onValueChange = { onPersonalInfoChange(personalInfo.copy(email = it)) },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.primaryTeal,
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = AppColors.primaryTeal,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = personalInfo.phone,
            onValueChange = { onPersonalInfoChange(personalInfo.copy(phone = it)) },
            label = { Text("Phone") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.primaryTeal,
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = AppColors.primaryTeal,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = personalInfo.address,
            onValueChange = { onPersonalInfoChange(personalInfo.copy(address = it)) },
            label = { Text("Address") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.primaryTeal,
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = AppColors.primaryTeal,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = personalInfo.summary,
            onValueChange = { onPersonalInfoChange(personalInfo.copy(summary = it)) },
            label = { Text("Professional Summary") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppColors.primaryTeal,
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = AppColors.primaryTeal,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black
            ),
            minLines = 3,
            maxLines = 5
        )
    }
}

@Composable
fun EducationEditor(
    educationList: SnapshotStateList<Education>,
    onRemove: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        educationList.forEachIndexed { index, education ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Education #${index + 1}",
                            fontWeight = FontWeight.Medium,
                            color = AppColors.darkGray
                        )
                        IconButton(onClick = { onRemove(index) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove Education",
                                tint = Color.Red
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = education.institution,
                        onValueChange = { educationList[index] = education.copy(institution = it) },
                        label = { Text("Institution") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.primaryTeal,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = AppColors.primaryTeal,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = education.degree,
                        onValueChange = { educationList[index] = education.copy(degree = it) },
                        label = { Text("Degree") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.primaryTeal,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = AppColors.primaryTeal,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = education.fieldOfStudy,
                        onValueChange = { educationList[index] = education.copy(fieldOfStudy = it) },
                        label = { Text("Field of Study") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.primaryTeal,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = AppColors.primaryTeal,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = education.startDate,
                            onValueChange = { educationList[index] = education.copy(startDate = it) },
                            label = { Text("Start Date") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppColors.primaryTeal,
                                unfocusedBorderColor = Color.Gray,
                                focusedLabelColor = AppColors.primaryTeal,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            ),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = education.endDate,
                            onValueChange = { educationList[index] = education.copy(endDate = it) },
                            label = { Text("End Date") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppColors.primaryTeal,
                                unfocusedBorderColor = Color.Gray,
                                focusedLabelColor = AppColors.primaryTeal,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            ),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = education.description,
                        onValueChange = { educationList[index] = education.copy(description = it) },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.primaryTeal,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = AppColors.primaryTeal,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        ),
                        minLines = 2,
                        maxLines = 4
                    )
                }
            }
        }
    }
}

@Composable
fun ExperienceEditor(
    experienceList: SnapshotStateList<Experience>,
    onRemove: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        experienceList.forEachIndexed { index, experience ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Experience #${index + 1}",
                            fontWeight = FontWeight.Medium,
                            color = AppColors.darkGray
                        )
                        IconButton(onClick = { onRemove(index) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove Experience",
                                tint = Color.Red
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = experience.company,
                        onValueChange = { experienceList[index] = experience.copy(company = it) },
                        label = { Text("Company") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.primaryTeal,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = AppColors.primaryTeal,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = experience.position,
                        onValueChange = { experienceList[index] = experience.copy(position = it) },
                        label = { Text("Position") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.primaryTeal,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = AppColors.primaryTeal,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = experience.startDate,
                            onValueChange = { experienceList[index] = experience.copy(startDate = it) },
                            label = { Text("Start Date") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppColors.primaryTeal,
                                unfocusedBorderColor = Color.Gray,
                                focusedLabelColor = AppColors.primaryTeal,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            ),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = experience.endDate,
                            onValueChange = { experienceList[index] = experience.copy(endDate = it) },
                            label = { Text("End Date") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppColors.primaryTeal,
                                unfocusedBorderColor = Color.Gray,
                                focusedLabelColor = AppColors.primaryTeal,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            ),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = experience.description,
                        onValueChange = { experienceList[index] = experience.copy(description = it) },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.primaryTeal,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = AppColors.primaryTeal,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        ),
                        minLines = 2,
                        maxLines = 4
                    )
                }
            }
        }
    }
}

@Composable
fun AbilitiesEditor(
    abilitiesList: SnapshotStateList<String>,
    onRemove: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        abilitiesList.forEachIndexed { index, ability ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = ability,
                    onValueChange = { abilitiesList[index] = it },
                    label = { Text("Skill #${index + 1}") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.primaryTeal,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = AppColors.primaryTeal,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black
                    ),
                    singleLine = true
                )

                IconButton(onClick = { onRemove(index) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove Skill",
                        tint = Color.Red
                    )
                }
            }
        }
    }
}

