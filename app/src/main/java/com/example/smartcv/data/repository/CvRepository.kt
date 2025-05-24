package com.example.smartcv.data.repository

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.smartcv.api.ApiResponse
import com.example.smartcv.api.MagnetoApiService
import com.example.smartcv.api.MagnetoSyncResponse
import com.example.smartcv.data.model.CvData
import com.example.smartcv.data.model.Education
import com.example.smartcv.data.model.Experience
import com.example.smartcv.data.model.PersonalInfo
import com.example.smartcv.utils.SchemaConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Repository class for handling CV-related operations
 */
class CvRepository(private val context: Context) {
    private val sqliteRepository = CvSqliteRepository(context)
    private val TAG = "CvRepository"
    private val magnetoApiService = MagnetoApiService()

    /**
     * Envía un CV a Magneto en formato JSON schema.org
     */
    suspend fun sendCvToMagneto(cvId: String, simulateError: Boolean = false): ApiResponse<MagnetoSyncResponse> {
        val cv = loadCv(cvId) ?: return ApiResponse.Error("CV no encontrado")

        try {
            // Convertir a formato Schema.org JSON
            val jsonData = SchemaConverter.resumeActionToSchemaJson(cv)

            // Enviar a Magneto
            return magnetoApiService.sendCvToMagneto(cvId, jsonData, simulateError)
        } catch (e: Exception) {
            Log.e(TAG, "Error al preparar CV para envío a Magneto", e)
            return ApiResponse.Error(e.message ?: "Error desconocido al preparar CV para envío")
        }
    }

    /**
     * Verifica el estado de sincronización de un CV con Magneto
     */
    suspend fun checkMagnetoSyncStatus(cvId: String): ApiResponse<MagnetoSyncResponse> {
        return magnetoApiService.checkSyncStatus(cvId)
    }

    /**
     * Guarda el estado de sincronización de un CV con Magneto
     */
    suspend fun saveMagnetoSyncStatus(cvId: String, syncResponse: MagnetoSyncResponse) {
        try {
            // En una implementación real, guardarías esto en la base de datos
            // Por ahora, solo lo registramos
            Log.d(TAG, "Estado de sincronización guardado para CV $cvId: ${syncResponse.success}")

            // Aquí podrías guardar el estado en SQLite si quieres
            // Por ejemplo, agregando una columna "magneto_synced" a tu tabla de CVs
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar estado de sincronización", e)
        }
    }

    /**
     * Saves CV data to SQLite database
     */
    suspend fun saveCvData(cvData: CvData) = withContext(Dispatchers.IO) {
        try {
            // Actualizar la fecha de modificación
            val updatedCv = cvData.copy(updatedAt = System.currentTimeMillis())

            // Guardar en SQLite
            if (cvData.id.isEmpty()) {
                // Nuevo CV
                val newId = UUID.randomUUID().toString()
                val newCv = updatedCv.copy(id = newId)
                sqliteRepository.saveCv(newCv)
                Log.d(TAG, "Nuevo CV guardado con ID: $newId")
                return@withContext newId
            } else {
                // Actualizar CV existente
                sqliteRepository.saveCv(updatedCv)
                Log.d(TAG, "CV actualizado con ID: ${cvData.id}")
                return@withContext cvData.id
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar CV en SQLite", e)
            throw e
        }
    }

    /**
     * Saves multiple CVs to SQLite database
     */
    suspend fun saveCvs(cvs: List<CvData>) = withContext(Dispatchers.IO) {
        cvs.forEach { cvData ->
            saveCvData(cvData)
        }
    }

    /**
     * Loads all CVs from SQLite database
     */
    suspend fun loadAllCvs(): List<CvData> = withContext(Dispatchers.IO) {
        try {
            // Intentar cargar desde SQLite
            val sqliteCvs = sqliteRepository.getAllCvs().map { it.second }
            Log.d(TAG, "Cargados ${sqliteCvs.size} CVs desde SQLite")
            return@withContext sqliteCvs.sortedByDescending { it.updatedAt }
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar CVs desde SQLite", e)
            return@withContext emptyList()
        }
    }

    /**
     * Loads a specific CV from SQLite database
     */
    suspend fun loadCv(cvId: String): CvData? = withContext(Dispatchers.IO) {
        try {
            // Intentar cargar desde SQLite
            val cv = sqliteRepository.getCvById(cvId)
            if (cv != null) {
                Log.d(TAG, "CV cargado desde SQLite: $cvId")
                return@withContext cv
            }

            // Si no está en SQLite, intentar cargar desde archivo
            Log.d(TAG, "CV no encontrado en SQLite, intentando cargar desde archivo: $cvId")
            val fileCv = loadFromFile(cvId)
            if (fileCv != null) {
                // Si se encontró en archivo, intentar migrar a SQLite
                try {
                    sqliteRepository.saveCv(fileCv)
                    Log.d(TAG, "CV migrado desde archivo a SQLite: $cvId")
                } catch (e: Exception) {
                    Log.e(TAG, "Error al migrar CV desde archivo a SQLite", e)
                }
            }
            return@withContext fileCv
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar CV desde SQLite", e)
            return@withContext null
        }
    }

    /**
     * Método de respaldo para cargar un CV desde archivo si SQLite falla
     */
    private suspend fun loadFromFile(cvId: String): CvData? = withContext(Dispatchers.IO) {
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

            val cv = CvData(
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

            // Migrar a SQLite mientras cargamos
            saveCvData(cv)

            Log.d(TAG, "CV cargado desde archivo: $cvId")
            return@withContext cv
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar CV desde archivo", e)
            return@withContext null
        }
    }

    /**
     * Deletes a CV from SQLite database and file system
     */
    suspend fun deleteCv(cvId: String): Boolean = withContext(Dispatchers.IO) {
        var success = false

        // Eliminar de SQLite
        try {
            success = sqliteRepository.deleteCv(cvId)
            Log.d(TAG, "CV eliminado de SQLite: $cvId, resultado: $success")
        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar CV de SQLite", e)
        }

        // También eliminar el archivo por si acaso
        try {
            val metadataFile = File(context.filesDir, "cvs/$cvId.json")
            val fileDeleted = metadataFile.delete()
            Log.d(TAG, "CV eliminado de archivo: $cvId, resultado: $fileDeleted")

            // Si al menos uno de los dos métodos tuvo éxito, consideramos exitosa la operación
            success = success || fileDeleted
        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar archivo de CV", e)
        }

        return@withContext success
    }

    /**
     * Exports a CV to JSON in Schema.org format
     */
    suspend fun exportCvToSchemaJson(cvId: String, outputFile: File): Boolean =
        withContext(Dispatchers.IO) {
            val cv = loadCv(cvId) ?: return@withContext false

            try {
                // Convertir a formato Schema.org
                val jsonData = SchemaConverter.resumeActionToSchemaJson(cv)

                // Escribir a archivo
                outputFile.writeText(jsonData)
                Log.d(TAG, "CV exportado a JSON Schema.org: $cvId")
                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "Error al exportar CV a JSON Schema.org", e)
                return@withContext false
            }
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
            val paint = Paint()
            paint.color = Color.BLACK
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

    /**
     * Shares a CV as JSON in Schema.org format
     */
    suspend fun shareCvAsSchemaJson(context: Context, cvData: CvData) {
        // Create a temporary JSON file for sharing
        val jsonDir = File(context.cacheDir, "temp_json")
        if (!jsonDir.exists()) {
            jsonDir.mkdirs()
        }

        val jsonFile = File(jsonDir, "${cvData.name}.json")

        // Export CV to Schema.org JSON
        val success = exportCvToSchemaJson(cvData.id, jsonFile)

        if (success) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                jsonFile
            )
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "application/json"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share CV as JSON"))
        } else {
            Toast.makeText(
                context,
                "Failed to create JSON for sharing",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}