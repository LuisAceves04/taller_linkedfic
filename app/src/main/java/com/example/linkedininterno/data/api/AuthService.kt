package com.example.linkedininterno.data.api

import com.example.linkedininterno.data.model.AuthResponse
import com.example.linkedininterno.data.model.LoginRequest
import com.example.linkedininterno.data.model.RegisterCompanyRequest
import com.example.linkedininterno.data.model.RegisterStudentRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface AuthService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @Multipart
    @POST("auth/register/student")
    suspend fun registerStudentMultipart(
        @Part("idRol") idRol: RequestBody,
        @Part("email") email: RequestBody,
        @Part("password") password: RequestBody,
        @Part("telefono") telefono: RequestBody,
        @Part("sobreMi") sobreMi: RequestBody,
        @Part("nombre") nombre: RequestBody,
        @Part("apellidoPaterno") apellidoPaterno: RequestBody,
        @Part("apellidoMaterno") apellidoMaterno: RequestBody,
        @Part("idCarrera") idCarrera: RequestBody,
        @Part curriculum: MultipartBody.Part? = null
    ): Response<AuthResponse>

    @POST("auth/register/student")
    suspend fun registerStudentJson(
        @Body request: RegisterStudentRequest
    ): Response<AuthResponse>

    @POST("auth/register/company")
    suspend fun registerCompany(@Body request: RegisterCompanyRequest): Response<AuthResponse>
}
