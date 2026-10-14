package com.example.linkedininterno

import com.google.gson.annotations.SerializedName

data class RespuestaPerfil(
    @SerializedName("success") val success: Boolean?,
    @SerializedName("message") val message: String?,
    @SerializedName("usuario") val usuario: Usuario?
)

data class Usuario(
    @SerializedName("idUsuario", alternate = ["id"]) val idUsuario: Int?,
    @SerializedName("email") val email: String?,
    @SerializedName("telefono") val telefono: String?,
    @SerializedName("sobreMi", alternate = ["sobre_mi"]) val sobreMi: String?,
    @SerializedName("avatar") val avatar: String?,
    @SerializedName("alumno") val alumno: Alumno?
)

data class Alumno(
    @SerializedName("idAlumno", alternate = ["id", "id_alumno"]) val idAlumno: Int?,
    @SerializedName("nombre") val nombre: String?,
    @SerializedName("apellidoPaterno", alternate = ["apellido_paterno"]) val apellidoPaterno: String?,
    @SerializedName("apellidoMaterno", alternate = ["apellido_materno"]) val apellidoMaterno: String?,
    @SerializedName("semestre") val semestre: Int?,
    @SerializedName("curriculum") val curriculum: String?,
    @SerializedName("carrera") val carrera: Carrera?,
    @SerializedName("habilidades") val habilidades: List<Habilidad>?
)

data class RespuestaHabilidades(
    @SerializedName("data") val data: List<Habilidad>?,
    @SerializedName("habilidades") val habilidades: List<Habilidad>?
)