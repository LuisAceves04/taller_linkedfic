package com.example.linkedininterno.network

import com.example.linkedininterno.Habilidad
import com.example.linkedininterno.Industria
import com.example.linkedininterno.RespuestaCarreras
import com.example.linkedininterno.RespuestaPerfil
import com.example.linkedininterno.Publicacion
import com.example.linkedininterno.RespuestaHabilidades
import com.example.linkedininterno.RespuestaIndustrias
import com.example.linkedininterno.RespuestaPublicaciones
import com.google.gson.JsonObject
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

data class EditPublicacionRequest(
    val titulo: String,
    val contenido: String
)

interface ApiService {

    @POST("login")
    fun login(
        @Body request: JsonObject
    ): Call<JsonObject>

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

    @GET("perfil")
    fun obtenerPerfil(
        @Header("Authorization") token: String
    ): Call<RespuestaPerfil>

    @GET("perfil")
    fun obtenerPerfilJson(@Header("Authorization") token: String): Call<JsonObject>

    @Multipart
    @POST("perfil")
    fun updatePerfil(
        @Header("Authorization") token: String,
        @Part("_method") method: RequestBody,
        @Part("nombre") nombre: RequestBody?,
        @Part("apellidoPaterno") apellidoPaterno: RequestBody?,
        @Part("apellidoMaterno") apellidoMaterno: RequestBody?,
        @Part("telefono") telefono: RequestBody?,
        @Part("semestre") semestre: RequestBody?,
        @Part("sobreMi") sobreMi: RequestBody?,
        @Part("idCarrera") idCarrera: RequestBody?,
        @Part avatar: MultipartBody.Part?,
        @Part curriculum: MultipartBody.Part?
    ): Call<JsonObject>

    @GET("publicaciones")
    fun obtenerPublicaciones(
        @Header("Authorization") token: String
    ): Call<RespuestaPublicaciones>

    @GET("publicaciones")
    fun obtenerPublicacionesRaw(@Header("Authorization") token: String): Call<com.google.gson.JsonElement>

    @Multipart
    @POST("publicaciones")
    fun crearPublicacionConImagen(
        @Header("Authorization") token: String,
        @Part("idUsuario") idUsuario: RequestBody,
        @Part("titulo") titulo: RequestBody,
        @Part("contenido") contenido: RequestBody,
        @Part multimedia: MultipartBody.Part?,
        @Part("published_at") publishedAt: RequestBody? = null
    ): Call<JsonObject>

    @Multipart
    @POST("publicaciones/{idPublicacion}")
    fun editarPublicacion(
        @Header("Authorization") token: String,
        @Path("idPublicacion") idPublicacion: Int,
        @Part("_method") method: RequestBody,
        @Part("titulo") titulo: RequestBody,
        @Part("contenido") contenido: RequestBody,
        @Part multimedia: MultipartBody.Part?
    ): Call<JsonObject>

    @DELETE("publicaciones/{id}")
    fun eliminarPublicacion(
        @Header("Authorization") token: String,
        @Path("id") idPublicacion: Int
    ): Call<JsonObject>

    @GET("habilidades")
    fun obtenerHabilidades(): Call<RespuestaHabilidades>

    @DELETE("alumnos/habilidades/{id_alumno_habilidad}")
    fun eliminarHabilidad(
        @Header("Authorization") token: String,
        @Path("id_alumno_habilidad") idAlumnoHabilidad: Int
    ): Call<JsonObject>

    @FormUrlEncoded
    @POST("alumnos/habilidades")
    fun agregarHabilidad(
        @Header("Authorization") token: String,
        @Field("idHabilidad") idHabilidad: Int,
        @Field("idNivel") idNivel: Int
    ): Call<JsonObject>

    @GET("alumnos/curriculum")
    fun descargarCurriculum(
        @Header("Authorization") token: String
    ): Call<ResponseBody>

    @Multipart
    @POST("empresas")
    fun registrarEmpresa(
        @Part("idRol") idRol: RequestBody,
        @Part("nombreEmpresa") nombreEmpresa: RequestBody,
        @Part("email") email: RequestBody,
        @Part("password") password: RequestBody,
        @Part("telefono") telefono: RequestBody?,
        @Part("paginaWeb") paginaWeb: RequestBody?,
        @Part("sobreMi") sobreMi: RequestBody?,
        @Part("idIndustria") idIndustria: RequestBody,
        @Part avatar: MultipartBody.Part? = null
    ): Call<JsonObject>

    @GET("industrias")
    fun obtenerIndustrias(): Call<RespuestaIndustrias>

    @Multipart
    @POST("perfil")
    fun updatePerfilEmpresa(
        @Header("Authorization") token: String,
        @Part("_method") method: RequestBody,
        @Part("nombreEmpresa") nombreEmpresa: RequestBody?,
        @Part("telefono") telefono: RequestBody?,
        @Part("paginaWeb") paginaWeb: RequestBody?,
        @Part("sobreMi") sobreMi: RequestBody?,
        @Part("idIndustria") idIndustria: RequestBody?,
        @Part avatar: MultipartBody.Part?
    ): Call<JsonObject>

    @GET("usuarios/{id}/perfil")
    fun obtenerPerfilPorId(
        @Header("Authorization") token: String,
        @Path("id") idUsuario: Int
    ): Call<RespuestaPerfil>
}