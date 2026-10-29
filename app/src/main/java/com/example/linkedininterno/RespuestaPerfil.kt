package com.example.linkedininterno

import com.google.gson.annotations.SerializedName

data class RespuestaPerfil(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("usuario") val usuario: UsuarioPerfil
)

data class UsuarioPerfil(
    @SerializedName("idUsuario") val idUsuario: Int,
    @SerializedName("idRol") val idRol: Int,
    @SerializedName("email") val email: String,
    @SerializedName("telefono") val telefono: String?,
    @SerializedName("sobreMi") val sobreMi: String?,
    @SerializedName("avatar") val avatar: String?,
    @SerializedName("alumno") val alumno: AlumnoPerfil?,
    @SerializedName("empresa") val empresa: EmpresaPerfil?,
    @SerializedName("rol") val rol: RolPerfil?
)

data class EmpresaPerfil(
    @SerializedName("idEmpresa") val idEmpresa: Int,
    @SerializedName("nombreEmpresa") val nombreEmpresa: String,
    @SerializedName("paginaWeb") val paginaWeb: String?,
    @SerializedName("industria") val industria: Industria?
)

data class AlumnoPerfil(
    @SerializedName("idAlumno") val idAlumno: Int,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("apellidoPaterno") val apellidoPaterno: String,
    @SerializedName("apellidoMaterno") val apellidoMaterno: String,
    @SerializedName("semestre") val semestre: Int,
    @SerializedName("curriculum") val curriculum: String?,
    @SerializedName("carrera") val carrera: CarreraPerfil?,
    @SerializedName("habilidades") val habilidades: List<Habilidad>?
)

data class CarreraPerfil(
    @SerializedName("idCarrera") val idCarrera: Int,
    @SerializedName("carrera") val carrera: String
)

data class RolPerfil(
    @SerializedName("idRol") val idRol: Int,
    @SerializedName("rol") val rol: String
)