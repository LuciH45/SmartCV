package com.example.smartcv.utils

import android.content.Context
import android.net.Uri
import com.example.smartcv.data.model.CvData
import com.example.smartcv.data.model.Education
import com.example.smartcv.data.model.Experience
import com.example.smartcv.data.model.PersonalInfo
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.regex.Pattern
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Utility class for OCR and text categorization
 */
object OcrUtils {

    /**
     * Extracts text from images using ML Kit's OCR
     */
    suspend fun extractTextFromImages(context: Context, imageUris: List<Uri>): String = withContext(Dispatchers.IO) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val stringBuilder = StringBuilder()

        for (uri in imageUris) {
            try {
                val image = InputImage.fromFilePath(context, uri)
                val result = suspendCancellableCoroutine<Text> { continuation ->
                    recognizer.process(image)
                        .addOnSuccessListener { text ->
                            continuation.resume(text)
                        }
                        .addOnFailureListener { exception ->
                            continuation.resumeWithException(exception)
                        }
                }
                stringBuilder.append(result.text)
                stringBuilder.append("\n\n")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return@withContext stringBuilder.toString()
    }

    /**
     * Categorizes extracted text into CV sections using AI (OpenAIService)
     */
    suspend fun categorizeTextWithAI(extractedText: String, isSpanish: Boolean = false): CvData = withContext(Dispatchers.IO) {
        return@withContext try {
            if (OpenAIService.isApiKeyConfigured()) {
                OpenAIService.processCvFromTranscript(extractedText, isSpanish)
            } else {
                categorizeText(extractedText) // fallback to keyword-based
            }
        } catch (e: Exception) {
            e.printStackTrace()
            categorizeText(extractedText) // fallback to keyword-based
        }
    }

    /**
     * Categorizes extracted text into CV sections
     */
    fun categorizeText(extractedText: String): CvData {
        val personalInfo = extractPersonalInfo(extractedText)
        val education = extractEducation(extractedText)
        val experience = extractExperience(extractedText)
        val abilities = extractAbilities(extractedText)

        return CvData(
            name = personalInfo.fullName.ifEmpty { "New CV" },
            personalInfo = personalInfo,
            education = education,
            experience = experience,
            abilities = abilities
        )
    }

    /**
     * Extracts personal information from text
     */
    private fun extractPersonalInfo(text: String): PersonalInfo {
        val fullName = extractFullName(text)
        val email = extractEmail(text)
        val phone = extractPhone(text)
        val address = extractAddress(text)
        val summary = extractSummary(text)

        return PersonalInfo(
            fullName = fullName,
            email = email,
            phone = phone,
            address = address,
            summary = summary
        )
    }

    /**
     * Extracts full name from text
     */
    private fun extractFullName(text: String): String {
        // Look for common name patterns at the beginning of the document
        val namePatterns = listOf(
            Pattern.compile("(?i)name\\s*:\\s*([\\w\\s]+)"),
            Pattern.compile("(?i)^([\\w\\s]+)\\s*$", Pattern.MULTILINE),
            Pattern.compile("(?i)^([\\w\\s]+)\\s*curriculum vitae", Pattern.MULTILINE),
            Pattern.compile("(?i)^([\\w\\s]+)\\s*resume", Pattern.MULTILINE)
        )

        for (pattern in namePatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return matcher.group(1)?.trim() ?: ""
            }
        }

        // If no name found, try to extract the first line as name
        val lines = text.split("\n")
        if (lines.isNotEmpty()) {
            val firstLine = lines[0].trim()
            if (firstLine.length < 50 && !firstLine.contains("@") && !firstLine.contains("resume", ignoreCase = true)) {
                return firstLine
            }
        }

        return ""
    }

    /**
     * Extracts email from text
     */
    private fun extractEmail(text: String): String {
        val emailPattern = Pattern.compile("(?i)[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}")
        val matcher = emailPattern.matcher(text)
        return if (matcher.find()) matcher.group() ?: "" else ""
    }

    /**
     * Extracts phone number from text
     */
    private fun extractPhone(text: String): String {
        val phonePatterns = listOf(
            Pattern.compile("(?i)phone\\s*:\\s*([\\d\\s\\+\\-\$$\$$]+)"),
            Pattern.compile("(?i)tel\\s*:\\s*([\\d\\s\\+\\-\$$\$$]+)"),
            Pattern.compile("(?i)mobile\\s*:\\s*([\\d\\s\\+\\-\$$\$$]+)"),
            Pattern.compile("(?i)(?<!\\w)(\\+?\\d{1,3}[\\s-]?)?\$$?\\d{3}\$$?[\\s-]?\\d{3}[\\s-]?\\d{4}(?!\\w)")
        )

        for (pattern in phonePatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return if (pattern == phonePatterns.last()) {
                    matcher.group() ?: ""
                } else {
                    matcher.group(1)?.trim() ?: ""
                }
            }
        }

        return ""
    }

    /**
     * Extracts address from text
     */
    private fun extractAddress(text: String): String {
        val addressPatterns = listOf(
            Pattern.compile("(?i)address\\s*:\\s*([^\\n]+)"),
            Pattern.compile("(?i)location\\s*:\\s*([^\\n]+)")
        )

        for (pattern in addressPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return matcher.group(1)?.trim() ?: ""
            }
        }

        return ""
    }

    /**
     * Extracts summary/objective from text
     */
    private fun extractSummary(text: String): String {
        val summaryPatterns = listOf(
            Pattern.compile("(?i)summary\\s*:?\\s*([^\\n]+(?:\\n(?!education|experience|skills|abilities)[^\\n]+)*)"),
            Pattern.compile("(?i)objective\\s*:?\\s*([^\\n]+(?:\\n(?!education|experience|skills|abilities)[^\\n]+)*)"),
            Pattern.compile("(?i)profile\\s*:?\\s*([^\\n]+(?:\\n(?!education|experience|skills|abilities)[^\\n]+)*)")
        )

        for (pattern in summaryPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return matcher.group(1)?.trim() ?: ""
            }
        }

        return ""
    }

    /**
     * Extracts education information from text
     */
    private fun extractEducation(text: String): List<Education> {
        val educationList = mutableListOf<Education>()

        // Find education section
        val educationSectionPattern = Pattern.compile(
            "(?i)education\\s*:?\\s*([^\\n]+(?:\\n(?!experience|skills|abilities)[^\\n]+)*)",
            Pattern.DOTALL
        )

        val matcher = educationSectionPattern.matcher(text)
        if (matcher.find()) {
            val educationSection = matcher.group(0) ?: return educationList

            // Split by years or degrees to identify different education entries
            val educationEntries = educationSection.split(
                Pattern.compile("(?i)(?:\\n|^)(?:\\d{4}|university|college|school|bachelor|master|phd|diploma)")
            ).filter { it.isNotEmpty() }

            for (entry in educationEntries) {
                if (entry.contains("education", ignoreCase = true) && entry.length < 15) {
                    continue // Skip the header
                }

                val institution = extractInstitution(entry)
                val degree = extractDegree(entry)
                val fieldOfStudy = extractFieldOfStudy(entry)
                val dates = extractDates(entry)

                if (institution.isNotEmpty() || degree.isNotEmpty() || fieldOfStudy.isNotEmpty()) {
                    educationList.add(
                        Education(
                            institution = institution,
                            degree = degree,
                            fieldOfStudy = fieldOfStudy,
                            startDate = dates.first,
                            endDate = dates.second,
                            description = extractDescription(entry)
                        )
                    )
                }
            }
        }

        return educationList
    }

    /**
     * Extracts institution name from education entry
     */
    private fun extractInstitution(text: String): String {
        val institutionPatterns = listOf(
            Pattern.compile("(?i)(?:university|college|school|institute)\\s+of\\s+([^,\\n]+)"),
            Pattern.compile("(?i)([^,\\n]+)\\s+(?:university|college|school|institute)")
        )

        for (pattern in institutionPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return matcher.group().trim()
            }
        }

        return ""
    }

    /**
     * Extracts degree from education entry
     */
    private fun extractDegree(text: String): String {
        val degreePatterns = listOf(
            Pattern.compile("(?i)(?:bachelor|master|phd|doctorate|diploma|certificate|bs|ba|ms|ma|mba)\\s+(?:of|in)?\\s+[^,\\n]+"),
            Pattern.compile("(?i)(?:b\\.?s\\.?|b\\.?a\\.?|m\\.?s\\.?|m\\.?a\\.?|m\\.?b\\.?a\\.?|ph\\.?d\\.?)")
        )

        for (pattern in degreePatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return matcher.group().trim()
            }
        }

        return ""
    }

    /**
     * Extracts field of study from education entry
     */
    private fun extractFieldOfStudy(text: String): String {
        val fieldPattern = Pattern.compile("(?i)(?:in|of)\\s+([^,\\n]+)")
        val matcher = fieldPattern.matcher(text)
        return if (matcher.find()) matcher.group(1)?.trim() ?: "" else ""
    }

    /**
     * Extracts work experience information from text
     */
    private fun extractExperience(text: String): List<Experience> {
        val experienceList = mutableListOf<Experience>()

        // Find experience section
        val experienceSectionPattern = Pattern.compile(
            "(?i)(?:experience|employment|work history)\\s*:?\\s*([^\\n]+(?:\\n(?!education|skills|abilities)[^\\n]+)*)",
            Pattern.DOTALL
        )

        val matcher = experienceSectionPattern.matcher(text)
        if (matcher.find()) {
            val experienceSection = matcher.group(0) ?: return experienceList

            // Split by years or companies to identify different experience entries
            val experienceEntries = experienceSection.split(
                Pattern.compile("(?i)(?:\\n|^)(?:\\d{4}|company|position|job title)")
            ).filter { it.isNotEmpty() }

            for (entry in experienceEntries) {
                if (entry.contains("experience", ignoreCase = true) && entry.length < 15) {
                    continue // Skip the header
                }

                val company = extractCompany(entry)
                val position = extractPosition(entry)
                val dates = extractDates(entry)

                if (company.isNotEmpty() || position.isNotEmpty()) {
                    experienceList.add(
                        Experience(
                            company = company,
                            position = position,
                            startDate = dates.first,
                            endDate = dates.second,
                            description = extractDescription(entry)
                        )
                    )
                }
            }
        }

        return experienceList
    }

    /**
     * Extracts company name from experience entry
     */
    private fun extractCompany(text: String): String {
        val companyPatterns = listOf(
            Pattern.compile("(?i)company\\s*:\\s*([^,\\n]+)"),
            Pattern.compile("(?i)employer\\s*:\\s*([^,\\n]+)"),
            Pattern.compile("(?i)(?:at|for)\\s+([^,\\n]+)")
        )

        for (pattern in companyPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return if (pattern == companyPatterns.last()) {
                    matcher.group(1)?.trim() ?: ""
                } else {
                    matcher.group(1)?.trim() ?: ""
                }
            }
        }

        // If no company found, try to extract the first line as company
        val lines = text.split("\n")
        if (lines.isNotEmpty()) {
            val firstLine = lines[0].trim()
            if (firstLine.length < 50 && !firstLine.contains("position", ignoreCase = true)) {
                return firstLine
            }
        }

        return ""
    }

    /**
     * Extracts position/title from experience entry
     */
    private fun extractPosition(text: String): String {
        val positionPatterns = listOf(
            Pattern.compile("(?i)position\\s*:\\s*([^,\\n]+)"),
            Pattern.compile("(?i)title\\s*:\\s*([^,\\n]+)"),
            Pattern.compile("(?i)job title\\s*:\\s*([^,\\n]+)")
        )

        for (pattern in positionPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return matcher.group(1)?.trim() ?: ""
            }
        }

        return ""
    }

    /**
     * Extracts dates from an entry
     */
    private fun extractDates(text: String): Pair<String, String> {
        var startDate = ""
        var endDate = ""

        // Look for date ranges
        val dateRangePatterns = listOf(
            Pattern.compile("(?i)(\\d{4})\\s*-\\s*(\\d{4}|present|current|now)"),
            Pattern.compile("(?i)(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\\s+\\d{4}\\s*-\\s*(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\\s+\\d{4}"),
            Pattern.compile("(?i)(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\\s+\\d{4}\\s*-\\s*(present|current|now)")
        )

        for (pattern in dateRangePatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                startDate = matcher.group(1) ?: ""
                endDate = matcher.group(2) ?: ""
                break
            }
        }

        // If no range found, look for individual dates
        if (startDate.isEmpty()) {
            val datePattern = Pattern.compile("(?i)(\\d{4}|jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)")
            val matcher = datePattern.matcher(text)
            if (matcher.find()) {
                startDate = matcher.group(1) ?: ""
            }
        }

        return Pair(startDate, endDate)
    }

    /**
     * Extracts description from an entry
     */
    private fun extractDescription(text: String): String {
        // Remove known headers and dates
        var cleanedText = text

        // Remove headers like "company:", "position:", etc.
        val headerPattern = Pattern.compile("(?i)(?:company|position|title|institution|degree|field|dates?)\\s*:\\s*[^\\n]+")
        val headerMatcher = headerPattern.matcher(cleanedText)
        cleanedText = headerMatcher.replaceAll("")

        // Remove date ranges
        val datePattern = Pattern.compile("(?i)\\d{4}\\s*-\\s*(?:\\d{4}|present|current|now)")
        val dateMatcher = datePattern.matcher(cleanedText)
        cleanedText = dateMatcher.replaceAll("")

        // Clean up and return
        return cleanedText.trim()
    }

    /**
     * Extracts abilities/skills from text
     */
    private fun extractAbilities(text: String): List<String> {
        val abilitiesList = mutableListOf<String>()

        // Find skills/abilities section
        val abilitiesSectionPattern = Pattern.compile(
            "(?i)(?:skills|abilities|competencies|proficiencies)\\s*:?\\s*([^\\n]+(?:\\n(?!education|experience)[^\\n]+)*)",
            Pattern.DOTALL
        )

        val matcher = abilitiesSectionPattern.matcher(text)
        if (matcher.find()) {
            val abilitiesSection = matcher.group(0) ?: return abilitiesList

            // Look for bullet points or commas
            val bulletPointPattern = Pattern.compile("(?i)(?:•|\\*|-)\\s*([^•\\*\\-\\n]+)")
            val bulletMatcher = bulletPointPattern.matcher(abilitiesSection)

            if (bulletMatcher.find()) {
                // If bullet points found, extract each one
                bulletMatcher.reset()
                while (bulletMatcher.find()) {
                    val ability = bulletMatcher.group(1)?.trim() ?: ""
                    if (ability.isNotEmpty()) {
                        abilitiesList.add(ability)
                    }
                }
            } else {
                // If no bullet points, try comma-separated list
                val commaListPattern = Pattern.compile("(?i)(?:skills|abilities)\\s*:?\\s*(.+)")
                val commaMatcher = commaListPattern.matcher(abilitiesSection)

                if (commaMatcher.find()) {
                    val skillsList = commaMatcher.group(1)?.trim() ?: ""
                    abilitiesList.addAll(skillsList.split(",", ";").map { it.trim() }.filter { it.isNotEmpty() })
                }
            }
        }

        return abilitiesList
    }
}

