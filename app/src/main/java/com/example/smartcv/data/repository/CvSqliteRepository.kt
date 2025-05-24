package com.example.smartcv.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.example.smartcv.data.db.CvDatabaseHelper
import com.example.smartcv.data.model.CvData
import com.example.smartcv.data.model.PersonalInfo
import com.example.smartcv.data.model.Education
import com.example.smartcv.data.model.Experience
import com.example.smartcv.utils.SchemaConverter
import com.google.gson.Gson
import org.json.JSONObject
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class CvSqliteRepository(private val context: Context) {
    private val dbHelper = CvDatabaseHelper(context)
    private val gson = Gson()
    private val TAG = "CvSqliteRepository"

    /**
     * Guarda un CV en la base de datos SQLite
     */
    fun saveCv(cvData: CvData) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put("id", cvData.id)
                put("name", cvData.name)
                put("created_at", cvData.createdAt)
                put("updated_at", cvData.updatedAt)
                put("personal_info", JSONObject().apply {
                    put("fullName", cvData.personalInfo.fullName)
                    put("email", cvData.personalInfo.email)
                    put("phone", cvData.personalInfo.phone)
                    put("address", cvData.personalInfo.address)
                    put("summary", cvData.personalInfo.summary)
                }.toString())
                put("education", SchemaConverter.educationListToJson(cvData.education).toString())
                put("experience", SchemaConverter.experienceListToJson(cvData.experience).toString())
                put("abilities", cvData.abilities.joinToString(","))
                put("source_images", cvData.sourceImageUris.joinToString(",") { it.toString() })
            }

            // Check if CV already exists
            val cursor = db.query("cvs", arrayOf("id"), "id = ?", arrayOf(cvData.id), null, null, null)
            val exists = cursor.moveToFirst()
            cursor.close()

            if (exists) {
                // Update existing CV
                val result = db.update("cvs", values, "id = ?", arrayOf(cvData.id))
                if (result <= 0) {
                    throw Exception("Error updating CV in SQLite: ${cvData.id}")
                }
                Log.d(TAG, "CV updated in SQLite with ID: ${cvData.id}")
            } else {
                // Insert new CV
                val result = db.insert("cvs", null, values)
                if (result == -1L) {
                    throw Exception("Error inserting CV into SQLite: ${cvData.id}")
                }
                Log.d(TAG, "CV inserted into SQLite with ID: ${cvData.id}")
            }

            // Verify the save/update was successful within the same transaction
            val verifyCursor = db.query(
                "cvs",
                arrayOf("id"),
                "id = ?",
                arrayOf(cvData.id),
                null, null, null
            )
            val saved = verifyCursor.moveToFirst()
            verifyCursor.close()

            if (!saved) {
                throw Exception("CV not found after save/update: ${cvData.id}")
            }

            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving CV to SQLite", e)
            throw e
        } finally {
            db.endTransaction()
            db.close()
        }
    }

    /**
     * Obtiene todos los CVs de la base de datos
     */
    fun getAllCvs(): List<Pair<String, CvData>> {
        val cvs = mutableListOf<Pair<String, CvData>>()
        val db = dbHelper.readableDatabase

        try {
            val cursor = db.query(true, "cvs", null, null, null, "id", null, "updated_at DESC", null)
            
            while (cursor.moveToNext()) {
                val id = cursor.getString(cursor.getColumnIndexOrThrow("id"))
                val cvData = cursorToCvData(cursor)
                cvs.add(Pair(id, cvData))
            }
            
            cursor.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading CVs from SQLite", e)
        } finally {
            db.close()
        }
        
        return cvs
    }

    /**
     * Obtiene un CV por su ID
     */
    fun getCvById(id: String): CvData? {
        val db = dbHelper.readableDatabase
        var cursor: Cursor? = null

        try {
            cursor = db.query(
                "cvs",
                null,
                "id = ?",
                arrayOf(id),
                null, null, null
            )

            if (cursor.moveToFirst()) {
                return cursorToCvData(cursor)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener CV de SQLite", e)
        } finally {
            cursor?.close()
            db.close()
        }

        return null
    }

    /**
     * Actualiza un CV existente
     */
    fun updateCv(id: String, cvData: CvData) {
        val db = dbHelper.writableDatabase
        try {
            val values = ContentValues().apply {
                put("name", cvData.name)
                put("updated_at", cvData.updatedAt)
                put("personal_info", JSONObject().apply {
                    put("fullName", cvData.personalInfo.fullName)
                    put("email", cvData.personalInfo.email)
                    put("phone", cvData.personalInfo.phone)
                    put("address", cvData.personalInfo.address)
                    put("summary", cvData.personalInfo.summary)
                }.toString())
                put("education", SchemaConverter.educationListToJson(cvData.education).toString())
                put("experience", SchemaConverter.experienceListToJson(cvData.experience).toString())
                put("abilities", cvData.abilities.joinToString(","))
                put("source_images", cvData.sourceImageUris.joinToString(",") { it.toString() })
            }

            db.update("cvs", values, "id = ?", arrayOf(id))
        } catch (e: Exception) {
            Log.e(TAG, "Error updating CV in SQLite", e)
            throw e
        } finally {
            db.close()
        }
    }

    /**
     * Elimina un CV por su ID
     */
    fun deleteCv(id: String): Boolean {
        val db = dbHelper.writableDatabase

        try {
            val rowsAffected = db.delete(
                "cvs",
                "id = ?",
                arrayOf(id)
            )

            return rowsAffected > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar CV de SQLite", e)
            return false
        } finally {
            db.close()
        }
    }

    private fun cursorToCvData(cursor: Cursor): CvData {
        val personalInfoJson = JSONObject(cursor.getString(cursor.getColumnIndexOrThrow("personal_info")))
        val personalInfo = PersonalInfo(
            fullName = personalInfoJson.optString("fullName", ""),
            email = personalInfoJson.optString("email", ""),
            phone = personalInfoJson.optString("phone", ""),
            address = personalInfoJson.optString("address", ""),
            summary = personalInfoJson.optString("summary", "")
        )

        // Parse education as JSONArray
        val educationJson = cursor.getString(cursor.getColumnIndexOrThrow("education"))
        val educationArray = JSONArray(educationJson)
        val education = mutableListOf<Education>()
        for (i in 0 until educationArray.length()) {
            val eduJson = educationArray.getJSONObject(i)
            education.add(
                Education(
                    institution = eduJson.optString("institution", ""),
                    degree = eduJson.optString("degree", ""),
                    fieldOfStudy = eduJson.optString("fieldOfStudy", ""),
                    startDate = eduJson.optString("startDate", ""),
                    endDate = eduJson.optString("endDate", ""),
                    description = eduJson.optString("description", "")
                )
            )
        }

        // Parse experience as JSONArray
        val experienceJson = cursor.getString(cursor.getColumnIndexOrThrow("experience"))
        val experienceArray = JSONArray(experienceJson)
        val experience = mutableListOf<Experience>()
        for (i in 0 until experienceArray.length()) {
            val expJson = experienceArray.getJSONObject(i)
            experience.add(
                Experience(
                    company = expJson.optString("company", ""),
                    position = expJson.optString("position", ""),
                    startDate = expJson.optString("startDate", ""),
                    endDate = expJson.optString("endDate", ""),
                    description = expJson.optString("description", "")
                )
            )
        }

        return CvData(
            id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
            name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
            updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at")),
            personalInfo = personalInfo,
            education = education,
            experience = experience,
            abilities = cursor.getString(cursor.getColumnIndexOrThrow("abilities")).split(",").filter { it.isNotEmpty() },
            sourceImageUris = cursor.getString(cursor.getColumnIndexOrThrow("source_images"))
                .split(",")
                .filter { it.isNotEmpty() }
                .map { android.net.Uri.parse(it) }
        )
    }

    /**
     * Obtiene la fecha y hora actual en formato ISO
     */
    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        return sdf.format(Date())
    }
}
