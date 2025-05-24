package com.example.smartcv.utils

import android.util.Log
import com.example.smartcv.data.model.CvData
import com.example.smartcv.data.model.Education
import com.example.smartcv.data.model.Experience
import com.example.smartcv.data.model.PersonalInfo
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Service for interacting with OpenAI API to process CV data
 */
object OpenAIService {
    private val API_KEY = "tu-clave-api-openai-aqui"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Verifica si la API key está configurada
     */
    fun isApiKeyConfigured(): Boolean {
        return API_KEY.isNotEmpty() && !API_KEY.startsWith("tu-clave") && API_KEY != "tu-clave-api-openai-aqui"
    }

    /**
     * Process a transcript to extract structured CV information
     * @param transcript The transcribed text from speech recognition
     * @param isSpanish Whether the transcript is in Spanish (default: false)
     */
    fun processCvFromTranscript(transcript: String, isSpanish: Boolean = false): CvData {
        if (!isApiKeyConfigured()) {
            Log.w("OpenAIService", "API Key not configured.")
            return createBasicCvFromTranscript(transcript, isSpanish)
        }

        try {
            // Create JSON request body for OpenAI
            val prompt = if (isSpanish) {
                """
                Analiza cuidadosamente el contenido de la siguiente transcripción en español y extrae toda la información útil para construir una hoja de vida (currículum vitae) de forma estructurada. Usa el entendimiento del significado y contexto de lo que se dice, no solo palabras clave.

                Devuelve ÚNICAMENTE un objeto JSON con la siguiente estructura exacta, sin texto adicional ni explicaciones:
                
                {
                  "personalInfo": {
                    "fullName": "",
                    "email": "",
                    "phone": "",
                    "address": "",
                    "summary": ""
                  },
                  "education": [
                    {
                      "institution": "",
                      "degree": "",
                      "fieldOfStudy": "",
                      "startDate": "",
                      "endDate": "",
                      "description": ""
                    }
                  ],
                  "experience": [
                    {
                      "company": "",
                      "position": "",
                      "startDate": "",
                      "endDate": "",
                      "description": ""
                    }
                  ],
                  "abilities": ["", "", ""]
                }
                
                Instrucciones:
                
                - Analiza el contenido completo para entender el perfil de la persona, sus estudios, experiencia laboral y habilidades.
                - Extrae nombres, fechas, instituciones, empresas y habilidades mencionadas.
                - Si no se menciona algún campo, déjalo vacío.
                - Asegúrate de que el JSON sea válido y siga exactamente la estructura especificada.
                - No incluyas ningún texto adicional fuera del JSON.
                
                Transcripción:
                $transcript
                """
            } else {
                """
                Carefully analyze the following transcript and extract all useful information to build a structured CV. Use understanding of meaning and context, not just keywords.

                Return ONLY a JSON object with the following exact structure, without any additional text or explanations:
                
                {
                  "personalInfo": {
                    "fullName": "",
                    "email": "",
                    "phone": "",
                    "address": "",
                    "summary": ""
                  },
                  "education": [
                    {
                      "institution": "",
                      "degree": "",
                      "fieldOfStudy": "",
                      "startDate": "",
                      "endDate": "",
                      "description": ""
                    }
                  ],
                  "experience": [
                    {
                      "company": "",
                      "position": "",
                      "startDate": "",
                      "endDate": "",
                      "description": ""
                    }
                  ],
                  "abilities": ["", "", ""]
                }
                
                Instructions:
                
                - Analyze the complete content to understand the person's profile, studies, work experience, and skills.
                - Extract names, dates, institutions, companies, and mentioned skills.
                - If a field is not mentioned, leave it empty.
                - Ensure the JSON is valid and follows exactly the specified structure.
                - Do not include any additional text outside the JSON.
                
                Transcript:
                $transcript
                """
            }

            val requestBody = JSONObject().apply {
                put("model", "gpt-3.5-turbo")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", "You are a CV data extraction assistant. Extract structured CV information from the provided transcript and return it in the specified JSON format.")
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
                put("temperature", 0.3)
                put("max_tokens", 2000)
            }.toString().toRequestBody("application/json".toMediaTypeOrNull())

            val request = Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .addHeader("Authorization", "Bearer $API_KEY")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val jsonResponse = JSONObject(responseBody)
                val choices = jsonResponse.getJSONArray("choices")
                if (choices.length() > 0) {
                    val message = choices.getJSONObject(0).getJSONObject("message")
                    val content = message.getString("content")

                    // Extract JSON from the response
                    val jsonStart = content.indexOf('{')
                    val jsonEnd = content.lastIndexOf('}') + 1
                    if (jsonStart >= 0 && jsonEnd > jsonStart) {
                        val jsonStr = content.substring(jsonStart, jsonEnd)
                        val json = JSONObject(jsonStr)

                        // Parse personal info
                        val personalInfoJson = json.getJSONObject("personalInfo")
                        val personalInfo = PersonalInfo(
                            fullName = personalInfoJson.optString("fullName", ""),
                            email = personalInfoJson.optString("email", ""),
                            phone = personalInfoJson.optString("phone", ""),
                            address = personalInfoJson.optString("address", ""),
                            summary = personalInfoJson.optString("summary", "")
                        )

                        // Parse education
                        val educationList = mutableListOf<Education>()
                        val educationArray = json.optJSONArray("education")
                        if (educationArray != null) {
                            for (i in 0 until educationArray.length()) {
                                val eduJson = educationArray.getJSONObject(i)
                                educationList.add(
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
                        }

                        // Parse experience
                        val experienceList = mutableListOf<Experience>()
                        val experienceArray = json.optJSONArray("experience")
                        if (experienceArray != null) {
                            for (i in 0 until experienceArray.length()) {
                                val expJson = experienceArray.getJSONObject(i)
                                experienceList.add(
                                    Experience(
                                        company = expJson.optString("company", ""),
                                        position = expJson.optString("position", ""),
                                        startDate = expJson.optString("startDate", ""),
                                        endDate = expJson.optString("endDate", ""),
                                        description = expJson.optString("description", "")
                                    )
                                )
                            }
                        }

                        // Parse abilities
                        val abilitiesList = mutableListOf<String>()
                        val abilitiesArray = json.optJSONArray("abilities")
                        if (abilitiesArray != null) {
                            for (i in 0 until abilitiesArray.length()) {
                                abilitiesList.add(abilitiesArray.getString(i))
                            }
                        }

                        // Create CV name based on personal info
                        val cvName = if (personalInfo.fullName.isNotEmpty()) {
                            "${personalInfo.fullName}'s CV"
                        } else {
                            "Voice CV ${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}"
                        }

                        return CvData(
                            name = cvName,
                            personalInfo = personalInfo,
                            education = educationList,
                            experience = experienceList,
                            abilities = abilitiesList
                        )
                    }
                }
            }
            throw Exception("Failed to process CV data from transcript")
        } catch (e: Exception) {
            Log.e("OpenAIService", "Error processing CV from transcript: ${e.message}", e)
            return createBasicCvFromTranscript(transcript, isSpanish)
        }
    }

    /**
     * Create a basic CV from transcript when API processing fails
     */
    private fun createBasicCvFromTranscript(transcript: String, isSpanish: Boolean = false): CvData {
        // Simple fallback that just puts the transcript in the summary
        val cvName = "Voice CV ${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}"

        return CvData(
            name = cvName,
            personalInfo = PersonalInfo(summary = transcript)
        )
    }
}

