package com.example.smartcv.data.model

import android.net.Uri

/**
 * Data class representing a document in the application
 *
 * @property id Unique identifier for the document
 * @property name Display name of the document
 * @property createdAt Timestamp when the document was created
 * @property updatedAt Timestamp when the document was last updated
 * @property imageUris List of URIs pointing to the document's images
 */
data class DocumentData(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val imageUris: List<Uri>
)