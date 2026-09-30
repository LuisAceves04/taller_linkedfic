package com.example.linkedininterno.network

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("api/login")
    fun login(@Body credentials: JsonObject): Call<JsonObject>

    @POST("api/alumnos")
    fun registrarAlumno(@Body alumnoData: JsonObject): Call<JsonObject>
}