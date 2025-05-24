package com.example.smartcv.utils

import android.util.Log
import com.example.smartcv.data.model.CvData
import com.example.smartcv.data.model.Education
import com.example.smartcv.data.model.Experience
import com.example.smartcv.data.model.PersonalInfo
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

object SchemaConverter {
    private val gson = Gson()

    /**
     * Convierte un objeto CvData al formato Schema.org ResumeAction en JSON
     */
    fun resumeActionToSchemaJson(cvData: CvData): String {
        val jsonObject = JSONObject().apply {
            put("@context", "https://schema.org")
            put("@type", "ResumeAction")
            put("name", cvData.name)
            
            // Personal Info
            put("applicant", JSONObject().apply {
                put("@type", "Person")
                put("name", cvData.personalInfo.fullName)
                put("email", cvData.personalInfo.email)
                put("telephone", cvData.personalInfo.phone)
                put("address", cvData.personalInfo.address)
                put("description", cvData.personalInfo.summary)
            })
            
            // Education
            val educationArray = JSONArray()
            cvData.education.forEach { education ->
                educationArray.put(JSONObject().apply {
                    put("@type", "EducationalCredential")
                    put("educationalCredentialAwarded", education.degree)
                    put("educationalProgram", education.fieldOfStudy)
                    put("educationalUse", "Resume")
                    put("provider", JSONObject().apply {
                        put("@type", "Organization")
                        put("name", education.institution)
                    })
                    put("validIn", JSONObject().apply {
                        put("@type", "TimePeriod")
                        put("startDate", education.startDate)
                        put("endDate", education.endDate)
                    })
                    put("description", education.description)
                })
            }
            put("education", educationArray)
            
            // Experience
            val experienceArray = JSONArray()
            cvData.experience.forEach { experience ->
                experienceArray.put(JSONObject().apply {
                    put("@type", "WorkAction")
                    put("agent", JSONObject().apply {
                        put("@type", "Organization")
                        put("name", experience.company)
                    })
                    put("object", JSONObject().apply {
                        put("@type", "Role")
                        put("roleName", experience.position)
                    })
                    put("startTime", experience.startDate)
                    put("endTime", experience.endDate)
                    put("description", experience.description)
                })
            }
            put("workExperience", experienceArray)
            
            // Skills
            val skillsArray = JSONArray()
            cvData.abilities.forEach { skill ->
                skillsArray.put(JSONObject().apply {
                    put("@type", "Skill")
                    put("name", skill)
                })
            }
            put("skills", skillsArray)
        }
        
        return jsonObject.toString()
    }

    /**
     * Convierte un JSON en formato Schema.org a CvData
     */
    fun jsonToCvData(jsonString: String): CvData {
        try {
            val jsonObject = JSONObject(jsonString)

            // Información personal
            var personalInfo = PersonalInfo()

            // Nombre
            if (jsonObject.has("name")) {
                personalInfo = personalInfo.copy(fullName = jsonObject.getString("name"))
            }

            // Descripción/resumen
            if (jsonObject.has("description")) {
                personalInfo = personalInfo.copy(summary = jsonObject.getString("description"))
            }

            // Información de contacto desde "about"
            if (jsonObject.has("about")) {
                val aboutObj = jsonObject.getJSONObject("about")

                if (aboutObj.has("email")) {
                    personalInfo = personalInfo.copy(email = aboutObj.getString("email"))
                }

                if (aboutObj.has("telephone")) {
                    personalInfo = personalInfo.copy(phone = aboutObj.getString("telephone"))
                }

                // Dirección
                if (aboutObj.has("address")) {
                    val addressObj = aboutObj.getJSONObject("address")
                    if (addressObj.has("streetAddress")) {
                        personalInfo = personalInfo.copy(address = addressObj.getString("streetAddress"))
                    }
                }
            }

            // Educación
            val educationList = mutableListOf<Education>()
            if (jsonObject.has("education")) {
                val educationArray = jsonObject.getJSONArray("education")
                for (i in 0 until educationArray.length()) {
                    val eduObj = educationArray.getJSONObject(i)
                    var education = Education()

                    if (eduObj.has("name")) {
                        education = education.copy(institution = eduObj.getString("name"))
                    }

                    if (eduObj.has("degree")) {
                        education = education.copy(degree = eduObj.getString("degree"))
                    }

                    if (eduObj.has("fieldOfStudy")) {
                        education = education.copy(fieldOfStudy = eduObj.getString("fieldOfStudy"))
                    }

                    if (eduObj.has("startDate")) {
                        education = education.copy(startDate = eduObj.getString("startDate"))
                    }

                    if (eduObj.has("endDate")) {
                        education = education.copy(endDate = eduObj.getString("endDate"))
                    }

                    if (eduObj.has("description")) {
                        education = education.copy(description = eduObj.getString("description"))
                    }

                    educationList.add(education)
                }
            }

            // Experiencia laboral
            val experienceList = mutableListOf<Experience>()
            if (jsonObject.has("workExperience")) {
                val workArray = jsonObject.getJSONArray("workExperience")
                for (i in 0 until workArray.length()) {
                    val workObj = workArray.getJSONObject(i)
                    var experience = Experience()

                    if (workObj.has("name")) {
                        experience = experience.copy(company = workObj.getString("name"))
                    }

                    if (workObj.has("jobTitle")) {
                        experience = experience.copy(position = workObj.getString("jobTitle"))
                    }

                    if (workObj.has("startDate")) {
                        experience = experience.copy(startDate = workObj.getString("startDate"))
                    }

                    if (workObj.has("endDate")) {
                        experience = experience.copy(endDate = workObj.getString("endDate"))
                    }

                    if (workObj.has("description")) {
                        experience = experience.copy(description = workObj.getString("description"))
                    }

                    experienceList.add(experience)
                }
            }

            // Habilidades
            val abilitiesList = mutableListOf<String>()
            if (jsonObject.has("skills")) {
                val skillsArray = jsonObject.getJSONArray("skills")
                for (i in 0 until skillsArray.length()) {
                    abilitiesList.add(skillsArray.getString(i))
                }
            }

            // Crear y devolver el objeto CvData
            return CvData(
                personalInfo = personalInfo,
                education = educationList,
                experience = experienceList,
                abilities = abilitiesList
            )
        } catch (e: Exception) {
            Log.e("SchemaConverter", "Error al convertir JSON a CvData", e)
            // Devolver un CvData vacío en caso de error
            return CvData(
                personalInfo = PersonalInfo(),
                education = emptyList(),
                experience = emptyList(),
                abilities = emptyList()
            )
        }
    }

    fun educationListToJson(educationList: List<Education>): JSONArray {
        return JSONArray().apply {
            educationList.forEach { education ->
                put(JSONObject().apply {
                    put("institution", education.institution)
                    put("degree", education.degree)
                    put("fieldOfStudy", education.fieldOfStudy)
                    put("startDate", education.startDate)
                    put("endDate", education.endDate)
                    put("description", education.description)
                })
            }
        }
    }

    fun experienceListToJson(experienceList: List<Experience>): JSONArray {
        return JSONArray().apply {
            experienceList.forEach { experience ->
                put(JSONObject().apply {
                    put("company", experience.company)
                    put("position", experience.position)
                    put("startDate", experience.startDate)
                    put("endDate", experience.endDate)
                    put("description", experience.description)
                })
            }
        }
    }

    fun jsonToEducationList(jsonArray: JSONObject): List<Education> {
        val educationList = mutableListOf<Education>()
        val array = jsonArray.getJSONArray("education")
        
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            educationList.add(Education(
                institution = obj.optString("institution", ""),
                degree = obj.optString("degree", ""),
                fieldOfStudy = obj.optString("fieldOfStudy", ""),
                startDate = obj.optString("startDate", ""),
                endDate = obj.optString("endDate", ""),
                description = obj.optString("description", "")
            ))
        }
        
        return educationList
    }

    fun jsonToExperienceList(jsonArray: JSONObject): List<Experience> {
        val experienceList = mutableListOf<Experience>()
        val array = jsonArray.getJSONArray("experience")
        
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            experienceList.add(Experience(
                company = obj.optString("company", ""),
                position = obj.optString("position", ""),
                startDate = obj.optString("startDate", ""),
                endDate = obj.optString("endDate", ""),
                description = obj.optString("description", "")
            ))
        }
        
        return experienceList
    }
}
