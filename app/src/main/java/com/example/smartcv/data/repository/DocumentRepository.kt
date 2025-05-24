package com.example.smartcv.data.repository

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.example.smartcv.data.model.DocumentData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * Repository class for handling document-related operations
 * Manages document storage, retrieval, and manipulation
 */
class DocumentRepository(private val context: Context) {

    /**
     * Creates a PDF from a list of image URIs
     *
     * @param imageUris List of image URIs to include in the PDF
     * @param outputFile File where the PDF will be saved
     * @return Boolean indicating success or failure
     */
    suspend fun createPdfFromImages(
        imageUris: List<Uri>,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val pdfDocument = PdfDocument()

            imageUris.forEachIndexed { index, uri ->
                val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input)
                } ?: return@withContext false

                // Create a page with the dimensions of the image
                val pageInfo = PdfDocument.PageInfo.Builder(
                    bitmap.width, bitmap.height, index + 1
                ).create()

                val page = pdfDocument.startPage(pageInfo)
                page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                pdfDocument.finishPage(page)

                // Recycle bitmap to free memory
                bitmap.recycle()
            }

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
     * Copies an image to the app's internal storage
     *
     * @param uri Source URI of the image
     * @param fileName Name to save the image as
     * @return URI of the saved image or null if operation failed
     */
    suspend fun copyImageToInternalStorage(
        uri: Uri,
        fileName: String
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val imagesDir = File(context.filesDir, "images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }

            val file = File(imagesDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }

            return@withContext file.toUri()
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    /**
     * Saves document metadata to internal storage
     *
     * @param documentId Unique identifier for the document
     * @param documentName Display name of the document
     * @param imageUris List of URIs pointing to the document's images
     */
    suspend fun saveDocumentMetadata(
        documentId: String,
        documentName: String,
        imageUris: List<Uri>
    ) = withContext(Dispatchers.IO) {
        try {
            val docsDir = File(context.filesDir, "documents")
            if (!docsDir.exists()) {
                docsDir.mkdirs()
            }

            val metadataFile = File(docsDir, "$documentId.json")
            val jsonObject = JSONObject().apply {
                put("id", documentId)
                put("name", documentName)
                put("createdAt", System.currentTimeMillis())
                put("updatedAt", System.currentTimeMillis())

                // Store image paths relative to app storage
                val imagesArray = JSONArray()
                imageUris.forEach { uri ->
                    val path = uri.toString()
                    imagesArray.put(path)
                }
                put("images", imagesArray)
            }

            FileOutputStream(metadataFile).use { out ->
                out.write(jsonObject.toString().toByteArray())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Loads all documents from internal storage
     *
     * @return List of DocumentData objects
     */
    suspend fun loadAllDocuments(): List<DocumentData> = withContext(Dispatchers.IO) {
        val docsDir = File(context.filesDir, "documents")
        if (!docsDir.exists() || !docsDir.isDirectory) {
            return@withContext emptyList()
        }

        val documents = mutableListOf<DocumentData>()
        docsDir.listFiles()?.filter { it.name.endsWith(".json") }?.forEach { file ->
            try {
                val jsonString = file.readText()
                val jsonObject = JSONObject(jsonString)

                val id = jsonObject.getString("id")
                val name = jsonObject.getString("name")
                val createdAt = jsonObject.getLong("createdAt")
                val updatedAt = jsonObject.getLong("updatedAt")

                val imagesArray = jsonObject.getJSONArray("images")
                val imageUris = mutableListOf<Uri>()
                for (i in 0 until imagesArray.length()) {
                    val uriString = imagesArray.getString(i)
                    imageUris.add(Uri.parse(uriString))
                }

                documents.add(
                    DocumentData(
                        id = id,
                        name = name,
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                        imageUris = imageUris
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Sort by updated time, newest first
        return@withContext documents.sortedByDescending { it.updatedAt }
    }

    /**
     * Loads a specific document from internal storage
     *
     * @param documentId Unique identifier for the document to load
     * @return DocumentData object or null if not found
     */
    suspend fun loadDocument(documentId: String): DocumentData? = withContext(Dispatchers.IO) {
        val metadataFile = File(context.filesDir, "documents/$documentId.json")
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

            val imagesArray = jsonObject.getJSONArray("images")
            val imageUris = mutableListOf<Uri>()
            for (i in 0 until imagesArray.length()) {
                val uriString = imagesArray.getString(i)
                imageUris.add(Uri.parse(uriString))
            }

            return@withContext DocumentData(
                id = id,
                name = name,
                createdAt = createdAt,
                updatedAt = updatedAt,
                imageUris = imageUris
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    /**
     * Updates a document's name
     *
     * @param documentId Unique identifier for the document
     * @param newName New name for the document
     * @return Boolean indicating success or failure
     */
    suspend fun updateDocumentName(
        documentId: String,
        newName: String
    ): Boolean = withContext(Dispatchers.IO) {
        val document = loadDocument(documentId) ?: return@withContext false

        // Update the document with new name
        val updatedDocument = document.copy(
            name = newName,
            updatedAt = System.currentTimeMillis()
        )

        // Save the updated document
        saveDocumentMetadata(
            updatedDocument.id,
            updatedDocument.name,
            updatedDocument.imageUris
        )

        return@withContext true
    }

    /**
     * Deletes a document
     *
     * @param documentId Unique identifier for the document to delete
     * @return Boolean indicating success or failure
     */
    suspend fun deleteDocument(documentId: String): Boolean = withContext(Dispatchers.IO) {
        val metadataFile = File(context.filesDir, "documents/$documentId.json")
        return@withContext metadataFile.delete()
    }

    /**
     * Shares a document as a PDF
     *
     * @param document DocumentData object to share
     */
    suspend fun shareDocument(context: Context, document: DocumentData) {
        shareDocument(context, document.imageUris, document.name)
    }

    /**
     * Shares images as a PDF
     *
     * @param imageUris List of image URIs to include in the PDF
     * @param name Name for the PDF file
     */
    suspend fun shareDocument(context: Context, imageUris: List<Uri>, name: String) {
        // Create a temporary PDF file for sharing
        val pdfDir = File(context.cacheDir, "temp_pdfs")
        if (!pdfDir.exists()) {
            pdfDir.mkdirs()
        }

        val pdfFile = File(pdfDir, "$name.pdf")

        // Create PDF from images
        val success = createPdfFromImages(
            imageUris,
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
            context.startActivity(Intent.createChooser(shareIntent, "Share Document"))
        } else {
            Toast.makeText(
                context,
                "Failed to create PDF for sharing",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Downloads images as a PDF to external storage
     *
     * @param imageUris List of image URIs to include in the PDF
     * @param name Name for the PDF file
     */
    suspend fun downloadDocument(context: Context, imageUris: List<Uri>, name: String) {
        // Create a PDF file for downloading
        val downloadsDir = context.getExternalFilesDir(null) ?: return
        val pdfFile = File(downloadsDir, "$name.pdf")

        // Create PDF from images
        val success = createPdfFromImages(
            imageUris,
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