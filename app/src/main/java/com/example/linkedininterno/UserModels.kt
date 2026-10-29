package com.example.linkedininterno

import com.google.gson.annotations.SerializedName

data class Usuario(
    @SerializedName(value = "idUsuario", alternate = ["id"]) val idUsuario: Int?,
    @SerializedName(value = "email") val email: String?,
    @SerializedName(value = "telefono") val telefono: String?,
    @SerializedName(value = "sobreMi", alternate = ["sobre_mi"]) val sobreMi: String?,
    @SerializedName(value = "avatar") val avatar: String?,
    @SerializedName(value = "alumno") val alumno: Alumno?,
    @SerializedName(value = "empresa") val empresa: Empresa? = null
)

data class Empresa(
    @SerializedName(value = "idEmpresa", alternate = ["id"]) val idEmpresa: Int? = null,
    @SerializedName(value = "nombreEmpresa", alternate = ["nombre_empresa"]) val nombreEmpresa: String? = null,
    @SerializedName(value = "paginaWeb", alternate = ["pagina_web"]) val paginaWeb: String? = null,
    @SerializedName(value = "industria") val industria: Industria? = null
)

data class Alumno(
    @SerializedName(value = "idAlumno", alternate = ["id", "id_alumno"]) val idAlumno: Int?,
    @SerializedName(value = "nombre") val nombre: String?,
    @SerializedName(value = "apellidoPaterno", alternate = ["apellido_paterno"]) val apellidoPaterno: String?,
    @SerializedName(value = "apellidoMaterno", alternate = ["apellido_materno"]) val apellidoMaterno: String?,
    @SerializedName(value = "semestre") val semestre: Int?,
    @SerializedName(value = "curriculum") val curriculum: String?,
    @SerializedName(value = "carrera") val carrera: Carrera?,
    @SerializedName(value = "habilidades") val habilidades: List<Habilidad>?
)

data class RespuestaHabilidades(
    @SerializedName(value = "data") val data: List<Habilidad>?,
    @SerializedName(value = "habilidades") val habilidades: List<Habilidad>?
)