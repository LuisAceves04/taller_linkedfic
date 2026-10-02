package com.example.linkedininterno.network

import com.example.linkedininterno.Carrera
import com.example.linkedininterno.RespuestaCarreras
import com.google.gson.JsonObject
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {

    @POST("login")
    fun login(@Body credentials: JsonObject): Call<JsonObject>

    @Multipart
    @POST("alumnos")
    fun registrarAlumno(
        @Part("idRol") idRol: RequestBody,
        @Part("email") email: RequestBody,
        @Part("password") password: RequestBody,
        @Part("telefono") telefono: RequestBody,
        @Part("sobreMi") sobreMi: RequestBody?,
        @Part("nombre") nombre: RequestBody,
        @Part("apellidoPaterno") apellidoPaterno: RequestBody,
        @Part("apellidoMaterno") apellidoMaterno: RequestBody,
        @Part("idCarrera") idCarrera: RequestBody,
        @Part("semestre") semestre: RequestBody,
        @Part avatar: MultipartBody.Part?,
        @Part curriculum: MultipartBody.Part
    ): Call<JsonObject>

    @GET("carreras")
    fun obtenerCarreras(): Call<RespuestaCarreras>
}