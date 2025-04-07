package com.example.smartcv.data

import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * Repository class for handling CV-related operations
 */
class CvRepository(private val context: Context) {

    /**
     * Saves CV data to internal storage
     */
    suspend fun saveCvData(cvData: CvData) = withContext(Dispatchers.IO) {
        try {
            val cvsDir = File(context.filesDir, "cvs")
            if (!cvsDir.exists()) {
                cvsDir.mkdirs()
            }

            val metadataFile = File(cvsDir, "${cvData.id}.json")
            val jsonObject = JSONObject().apply {
                put("id", cvData.id)
                put("name", cvData.name)
                put("createdAt", cvData.createdAt)
                put("updatedAt", System.currentTimeMillis())

                // Personal Info
                val personalInfoJson = JSONObject().apply {
                    put("fullName", cvData.personalInfo.fullName)
                    put("email", cvData.personalInfo.email)
                    put("phone", cvData.personalInfo.phone)
                    put("address", cvData.personalInfo.address)
                    put("summary", cvData.personalInfo.summary)
                }
                put("personalInfo", personalInfoJson)

                // Education
                val educationArray = JSONArray()
                cvData.education.forEach { education ->
                    val educationJson = JSONObject().apply {
                        put("institution", education.institution)
                        put("degree", education.degree)
                        put("fieldOfStudy", education.fieldOfStudy)
                        put("startDate", education.startDate)
                        put("endDate", education.endDate)
                        put("description", education.description)
                    }
                    educationArray.put(educationJson)
                }
                put("education", educationArray)

                // Experience
                val experienceArray = JSONArray()
                cvData.experience.forEach { experience ->
                    val experienceJson = JSONObject().apply {
                        put("company", experience.company)
                        put("position", experience.position)
                        put("startDate", experience.startDate)
                        put("endDate", experience.endDate)
                        put("description", experience.description)
                    }
                    experienceArray.put(experienceJson)
                }
                put("experience", experienceArray)

                // Abilities
                val abilitiesArray = JSONArray()
                cvData.abilities.forEach { ability ->
                    abilitiesArray.put(ability)
                }
                put("abilities", abilitiesArray)

                // Source Images
                val imagesArray = JSONArray()
                cvData.sourceImageUris.forEach { uri ->
                    imagesArray.put(uri.toString())
                }
                put("sourceImages", imagesArray)
            }

            FileOutputStream(metadataFile).use { out ->
                out.write(jsonObject.toString().toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Loads all CVs from internal storage
     */
    suspend fun loadAllCvs(): List<CvData> = withContext(Dispatchers.IO) {
        val cvsDir = File(context.filesDir, "cvs")
        if (!cvsDir.exists() || !cvsDir.isDirectory) {
            return@withContext emptyList()
        }

        val cvs = mutableListOf<CvData>()
        cvsDir.listFiles()?.filter { it.name.endsWith(".json") }?.forEach { file ->
            try {
                val jsonString = file.readText()
                val jsonObject = JSONObject(jsonString)

                val id = jsonObject.getString("id")
                val name = jsonObject.getString("name")
                val createdAt = jsonObject.getLong("createdAt")
                val updatedAt = jsonObject.getLong("updatedAt")

                // Parse Personal Info
                val personalInfoJson = jsonObject.getJSONObject("personalInfo")
                val personalInfo = PersonalInfo(
                    fullName = personalInfoJson.optString("fullName", ""),
                    email = personalInfoJson.optString("email", ""),
                    phone = personalInfoJson.optString("phone", ""),
                    address = personalInfoJson.optString("address", ""),
                    summary = personalInfoJson.optString("summary", "")
                )

                // Parse Education
                val educationArray = jsonObject.getJSONArray("education")
                val education = mutableListOf<Education>()
                for (i in 0 until educationArray.length()) {
                    val educationJson = educationArray.getJSONObject(i)
                    education.add(
                        Education(
                            institution = educationJson.optString("institution", ""),
                            degree = educationJson.optString("degree", ""),
                            fieldOfStudy = educationJson.optString("fieldOfStudy", ""),
                            startDate = educationJson.optString("startDate", ""),
                            endDate = educationJson.optString("endDate", ""),
                            description = educationJson.optString("description", "")
                        )
                    )
                }

                // Parse Experience
                val experienceArray = jsonObject.getJSONArray("experience")
                val experience = mutableListOf<Experience>()
                for (i in 0 until experienceArray.length()) {
                    val experienceJson = experienceArray.getJSONObject(i)
                    experience.add(
                        Experience(
                            company = experienceJson.optString("company", ""),
                            position = experienceJson.optString("position", ""),
                            startDate = experienceJson.optString("startDate", ""),
                            endDate = experienceJson.optString("endDate", ""),
                            description = experienceJson.optString("description", "")
                        )
                    )
                }

                // Parse Abilities
                val abilitiesArray = jsonObject.getJSONArray("abilities")
                val abilities = mutableListOf<String>()
                for (i in 0 until abilitiesArray.length()) {
                    abilities.add(abilitiesArray.getString(i))
                }

                // Parse Source Images
                val imagesArray = jsonObject.getJSONArray("sourceImages")
                val sourceImageUris = mutableListOf<Uri>()
                for (i in 0 until imagesArray.length()) {
                    val uriString = imagesArray.getString(i)
                    sourceImageUris.add(Uri.parse(uriString))
                }

                cvs.add(
                    CvData(
                        id = id,
                        name = name,
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                        personalInfo = personalInfo,
                        education = education,
                        experience = experience,
                        abilities = abilities,
                        sourceImageUris = sourceImageUris
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Sort by updated time, newest first
        return@withContext cvs.sortedByDescending { it.updatedAt }
    }

    /**
     * Loads a specific CV from internal storage
     */
    suspend fun loadCv(cvId: String): CvData? = withContext(Dispatchers.IO) {
        val metadataFile = File(context.filesDir, "cvs/$cvId.json")
        if (!metadataFile.exists()) {
            return@withContext null
        }

        try {
            val jsonString = metadataFile.readText()
            val jsonObject = JSONObject(jsonString)

            val id = jsonObject.getString("id")
            val name = jsonObject.getString("name")
            val createdAt = jsonObject.getLong("createdAt")
            val updatedAt = jsonObject.getLong("updatedAt")

            // Parse Personal Info
            val personalInfoJson = jsonObject.getJSONObject("personalInfo")
            val personalInfo = PersonalInfo(
                fullName = personalInfoJson.optString("fullName", ""),
                email = personalInfoJson.optString("email", ""),
                phone = personalInfoJson.optString("phone", ""),
                address = personalInfoJson.optString("address", ""),
                summary = personalInfoJson.optString("summary", "")
            )

            // Parse Education
            val educationArray = jsonObject.getJSONArray("education")
            val education = mutableListOf<Education>()
            for (i in 0 until educationArray.length()) {
                val educationJson = educationArray.getJSONObject(i)
                education.add(
                    Education(
                        institution = educationJson.optString("institution", ""),
                        degree = educationJson.optString("degree", ""),
                        fieldOfStudy = educationJson.optString("fieldOfStudy", ""),
                        startDate = educationJson.optString("startDate", ""),
                        endDate = educationJson.optString("endDate", ""),
                        description = educationJson.optString("description", "")
                    )
                )
            }

            // Parse Experience
            val experienceArray = jsonObject.getJSONArray("experience")
            val experience = mutableListOf<Experience>()
            for (i in 0 until experienceArray.length()) {
                val experienceJson = experienceArray.getJSONObject(i)
                experience.add(
                    Experience(
                        company = experienceJson.optString("company", ""),
                        position = experienceJson.optString("position", ""),
                        startDate = experienceJson.optString("startDate", ""),
                        endDate = experienceJson.optString("endDate", ""),
                        description = experienceJson.optString("description", "")
                    )
                )
            }

            // Parse Abilities
            val abilitiesArray = jsonObject.getJSONArray("abilities")
            val abilities = mutableListOf<String>()
            for (i in 0 until abilitiesArray.length()) {
                abilities.add(abilitiesArray.getString(i))
            }

            // Parse Source Images
            val imagesArray = jsonObject.getJSONArray("sourceImages")
            val sourceImageUris = mutableListOf<Uri>()
            for (i in 0 until imagesArray.length()) {
                val uriString = imagesArray.getString(i)
                sourceImageUris.add(Uri.parse(uriString))
            }

            return@withContext CvData(
                id = id,
                name = name,
                createdAt = createdAt,
                updatedAt = updatedAt,
                personalInfo = personalInfo,
                education = education,
                experience = experience,
                abilities = abilities,
                sourceImageUris = sourceImageUris
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    /**
     * Deletes a CV
     */
    suspend fun deleteCv(cvId: String): Boolean = withContext(Dispatchers.IO) {
        val metadataFile = File(context.filesDir, "cvs/$cvId.json")
        return@withContext metadataFile.delete()
    }

    /**
     * Creates a PDF from CV data
     */
    suspend fun createPdfFromCv(
        cvData: CvData,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val pdfDocument = PdfDocument()

            // Create a page
            val pageInfo = PdfDocument.PageInfo.Builder(612, 792, 1).create()
            val page = pdfDocument.startPage(pageInfo)

            val canvas = page.canvas
            val paint = android.graphics.Paint()
            paint.color = android.graphics.Color.BLACK
            paint.textSize = 12f

            // Title
            paint.textSize = 18f
            paint.isFakeBoldText = true
            canvas.drawText("CURRICULUM VITAE", 50f, 50f, paint)
            paint.isFakeBoldText = false

            // Personal Info
            paint.textSize = 16f
            paint.isFakeBoldText = true
            canvas.drawText("Personal Information", 50f, 90f, paint)
            paint.isFakeBoldText = false
            paint.textSize = 12f

            var yPosition = 110f

            if (cvData.personalInfo.fullName.isNotEmpty()) {
                canvas.drawText("Name: ${cvData.personalInfo.fullName}", 50f, yPosition, paint)
                yPosition += 20f
            }

            if (cvData.personalInfo.email.isNotEmpty()) {
                canvas.drawText("Email: ${cvData.personalInfo.email}", 50f, yPosition, paint)
                yPosition += 20f
            }

            if (cvData.personalInfo.phone.isNotEmpty()) {
                canvas.drawText("Phone: ${cvData.personalInfo.phone}", 50f, yPosition, paint)
                yPosition += 20f
            }

            if (cvData.personalInfo.address.isNotEmpty()) {
                canvas.drawText("Address: ${cvData.personalInfo.address}", 50f, yPosition, paint)
                yPosition += 20f
            }

            if (cvData.personalInfo.summary.isNotEmpty()) {
                canvas.drawText("Summary:", 50f, yPosition, paint)
                yPosition += 20f

                // Split summary into lines
                val summaryLines = cvData.personalInfo.summary.split("\n")
                for (line in summaryLines) {
                    canvas.drawText(line, 70f, yPosition, paint)
                    yPosition += 20f
                }
            }

            yPosition += 20f

            // Education
            if (cvData.education.isNotEmpty()) {
                paint.textSize = 16f
                paint.isFakeBoldText = true
                canvas.drawText("Education", 50f, yPosition, paint)
                paint.isFakeBoldText = false
                paint.textSize = 12f
                yPosition += 20f

                for (education in cvData.education) {
                    if (education.institution.isNotEmpty()) {
                        paint.isFakeBoldText = true
                        canvas.drawText(education.institution, 50f, yPosition, paint)
                        paint.isFakeBoldText = false
                        yPosition += 20f
                    }

                    if (education.degree.isNotEmpty() || education.fieldOfStudy.isNotEmpty()) {
                        val degreeField = "${education.degree} ${education.fieldOfStudy}".trim()
                        canvas.drawText(degreeField, 70f, yPosition, paint)
                        yPosition += 20f
                    }

                    if (education.startDate.isNotEmpty() || education.endDate.isNotEmpty()) {
                        val dateRange = "${education.startDate} - ${education.endDate}".trim()
                        canvas.drawText(dateRange, 70f, yPosition, paint)
                        yPosition += 20f
                    }

                    if (education.description.isNotEmpty()) {
                        val descLines = education.description.split("\n")
                        for (line in descLines) {
                            canvas.drawText(line, 70f, yPosition, paint)
                            yPosition += 20f
                        }
                    }

                    yPosition += 10f
                }
            }

            yPosition += 20f

            // Experience
            if (cvData.experience.isNotEmpty()) {
                paint.textSize = 16f
                paint.isFakeBoldText = true
                canvas.drawText("Work Experience", 50f, yPosition, paint)
                paint.isFakeBoldText = false
                paint.textSize = 12f
                yPosition += 20f

                for (experience in cvData.experience) {
                    if (experience.company.isNotEmpty()) {
                        paint.isFakeBoldText = true
                        canvas.drawText(experience.company, 50f, yPosition, paint)
                        paint.isFakeBoldText = false
                        yPosition += 20f
                    }

                    if (experience.position.isNotEmpty()) {
                        canvas.drawText(experience.position, 70f, yPosition, paint)
                        yPosition += 20f
                    }

                    if (experience.startDate.isNotEmpty() || experience.endDate.isNotEmpty()) {
                        val dateRange = "${experience.startDate} - ${experience.endDate}".trim()
                        canvas.drawText(dateRange, 70f, yPosition, paint)
                        yPosition += 20f
                    }

                    if (experience.description.isNotEmpty()) {
                        val descLines = experience.description.split("\n")
                        for (line in descLines) {
                            canvas.drawText(line, 70f, yPosition, paint)
                            yPosition += 20f
                        }
                    }

                    yPosition += 10f
                }
            }

            yPosition += 20f

            // Abilities
            if (cvData.abilities.isNotEmpty()) {
                paint.textSize = 16f
                paint.isFakeBoldText = true
                canvas.drawText("Skills & Abilities", 50f, yPosition, paint)
                paint.isFakeBoldText = false
                paint.textSize = 12f
                yPosition += 20f

                for (ability in cvData.abilities) {
                    canvas.drawText("• $ability", 70f, yPosition, paint)
                    yPosition += 20f
                }
            }

            pdfDocument.finishPage(page)

            // Write the PDF to the output file
            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }

            pdfDocument.close()
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    /**
     * Shares a CV as a PDF
     */
    suspend fun shareCvAsPdf(context: Context, cvData: CvData) {
        // Create a temporary PDF file for sharing
        val pdfDir = File(context.cacheDir, "temp_pdfs")
        if (!pdfDir.exists()) {
            pdfDir.mkdirs()
        }

        val pdfFile = File(pdfDir, "${cvData.name}.pdf")

        // Create PDF from CV data
        val success = createPdfFromCv(
            cvData,
            pdfFile
        )

        if (success) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                pdfFile
            )
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "application/pdf"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share CV"))
        } else {
            Toast.makeText(
                context,
                "Failed to create PDF for sharing",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Downloads a CV as a PDF to external storage
     */
    suspend fun downloadCvAsPdf(context: Context, cvData: CvData) {
        // Create a PDF file for downloading
        val downloadsDir = context.getExternalFilesDir(null) ?: return
        val pdfFile = File(downloadsDir, "${cvData.name}.pdf")

        // Create PDF from CV data
        val success = createPdfFromCv(
            cvData,
            pdfFile
        )

        if (success) {
            Toast.makeText(
                context,
                "PDF saved to Downloads folder",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                context,
                "Failed to save PDF",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

