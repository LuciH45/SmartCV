package com.example.smartcv.utils

import android.util.Log
import com.example.smartcv.data.CvData
import com.example.smartcv.data.Education
import com.example.smartcv.data.Experience
import com.example.smartcv.data.PersonalInfo
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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
                - Usa criterio semántico para determinar a qué sección pertenece cada fragmento de la transcripción.
                - Si hay información relevante pero no completa (por ejemplo, sin fechas), inclúyela igualmente en el campo correspondiente y deja vacíos los datos faltantes.
                - Si algún campo no tiene información en la transcripción, déjalo como una cadena vacía "".
                - No incluyas explicaciones ni comentarios, solo el JSON.
                
                Transcripción:
                $transcript
                """
            } else {
                """
                Carefully analyze the following transcript in Spanish and extract all relevant information to build a structured résumé (CV). Use your understanding of meaning and context, not keyword matching.

                Return ONLY a JSON object in the exact structure below, with no extra text or explanations:
                
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
                - Analyze the entire transcript using semantic reasoning to determine the appropriate section for each piece of information.
                - If relevant information is present but incomplete (e.g., missing dates), include it and leave the missing fields blank.
                - Leave fields as empty strings ("") if the information is not present.
                - Return strictly and only the JSON object.
                
                Transcript:
                $transcript
                """
            }

            val systemPrompt = if (isSpanish) {
                "Eres un asistente especializado en extraer información estructurada para CVs a partir de transcripciones en español. Tu tarea es analizar el texto y extraer datos relevantes como información personal, educación, experiencia laboral y habilidades."
            } else {
                "You are a CV parsing assistant that extracts structured information from transcripts."
            }

            // Replace your current jsonBody construction with this:
            val escapedPrompt = prompt.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")

            val escapedSystemPrompt = systemPrompt.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")

            val jsonBody = """
                {
                  "model": "gpt-3.5-turbo",
                  "messages": [
                    {"role": "system", "content": "$escapedSystemPrompt"},
                    {"role": "user", "content": "$escapedPrompt"}
                  ],
                  "temperature": 0.2
                }
            """.trimIndent()

            val requestBody = jsonBody.toRequestBody("application/json".toMediaTypeOrNull())

            val request = Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .post(requestBody)
                .addHeader("Authorization", "Bearer $API_KEY")
                .addHeader("Content-Type", "application/json")
                .build()

            Log.d("OpenAIService", "Enviando solicitud a OpenAI API")

            // Execute the request
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Log.e("OpenAIService", "API Error: $errorBody")
                    throw IOException("API Error: ${response.code} - ${response.message}")
                }

                val responseBody = response.body?.string()
                if (responseBody != null) {
                    Log.d("OpenAIService", "Respuesta recibida de OpenAI")

                    try {
                        val jsonResponse = JSONObject(responseBody)
                        val content = jsonResponse
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")

                        Log.d("OpenAIService", "Contenido de respuesta: $content")

                        // Parse the JSON content
                        return parseCvJson(content, transcript, isSpanish)
                    } catch (e: Exception) {
                        Log.e("OpenAIService", "JSON parsing error: ${e.message}", e)
                        Log.e("OpenAIService", "Response body: $responseBody")
                        throw IOException("Error parsing API response: ${e.message}")
                    }
                } else {
                    throw IOException("Empty response from API")
                }
            }
        } catch (e: Exception) {
            Log.e("OpenAIService", "Error in processCvFromTranscript: ${e.message}", e)
            e.printStackTrace()
            // Fallback to basic processing
            return createBasicCvFromTranscript(transcript, isSpanish)
        }
    }

    /**
     * Parse JSON response from OpenAI into CvData
     */
    private fun parseCvJson(jsonContent: String, fallbackTranscript: String, isSpanish: Boolean = false): CvData {
        try {
            // Extract the JSON object from the content (it might be wrapped in markdown code blocks)
            val jsonString = jsonContent.trim()
                .replace("```json", "")
                .replace("```", "")
                .trim()

            Log.d("OpenAIService", "Parsing JSON: $jsonString")

            // Try to find a valid JSON object in the response
            val jsonStartIndex = jsonString.indexOf("{")
            val jsonEndIndex = jsonString.lastIndexOf("}")

            if (jsonStartIndex >= 0 && jsonEndIndex > jsonStartIndex) {
                val cleanedJsonString = jsonString.substring(jsonStartIndex, jsonEndIndex + 1)
                Log.d("OpenAIService", "Cleaned JSON: $cleanedJsonString")

                try {
                    val json = JSONObject(cleanedJsonString)

                    // Parse personal info
                    val personalInfoJson = json.optJSONObject("personalInfo") ?: JSONObject()
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
                        // Always use English for UI regardless of input language
                        "${personalInfo.fullName}'s CV"
                    } else {
                        // Always use English for UI regardless of input language
                        "Voice CV ${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}"
                    }

                    return CvData(
                        name = cvName,
                        personalInfo = personalInfo,
                        education = educationList,
                        experience = experienceList,
                        abilities = abilitiesList
                    )
                } catch (e: Exception) {
                    Log.e("OpenAIService", "Error parsing cleaned JSON: ${e.message}", e)
                    throw e
                }
            } else {
                Log.e("OpenAIService", "Could not find valid JSON object in response")
                throw Exception("Invalid JSON format in API response")
            }
        } catch (e: Exception) {
            Log.e("OpenAIService", "Error parsing CV JSON: ${e.message}", e)
            e.printStackTrace()
            // Fallback to basic processing
            return createBasicCvFromTranscript(fallbackTranscript, isSpanish)
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

