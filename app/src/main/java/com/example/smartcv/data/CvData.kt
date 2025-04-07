package com.example.smartcv.data

import java.util.UUID

/**
 * Data class representing structured CV information
 */
data class CvData(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val personalInfo: PersonalInfo = PersonalInfo(),
    val education: List<Education> = emptyList(),
    val experience: List<Experience> = emptyList(),
    val abilities: List<String> = emptyList(),
    val sourceImageUris: List<android.net.Uri> = emptyList()
)

/**
 * Data class for personal information section
 */
data class PersonalInfo(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val summary: String = ""
)

/**
 * Data class for education entries
 */
data class Education(
    val institution: String = "",
    val degree: String = "",
    val fieldOfStudy: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val description: String = ""
)

/**
 * Data class for work experience entries
 */
data class Experience(
    val company: String = "",
    val position: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val description: String = ""
)

