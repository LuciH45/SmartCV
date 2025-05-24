package com.example.smartcv.data.model

import android.net.Uri

data class CvData(
    val id: String = "",
    val name: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val personalInfo: PersonalInfo = PersonalInfo(),
    val education: List<Education> = emptyList(),
    val experience: List<Experience> = emptyList(),
    val abilities: List<String> = emptyList(),
    val sourceImageUris: List<Uri> = emptyList()
)

data class PersonalInfo(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val summary: String = ""
)

data class Education(
    val institution: String = "",
    val degree: String = "",
    val fieldOfStudy: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val description: String = ""
)

data class Experience(
    val company: String = "",
    val position: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val description: String = ""
) 