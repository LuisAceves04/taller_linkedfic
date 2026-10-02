package com.example.linkedininterno

data class Usuario(
    val id: Int,
    val email: String,
    val telefono: String?,
    val sobreMi: String?,
    val avatar: String?,
    val alumno: Alumno?
)

data class Alumno(
    val idAlumno: Int,
    val nombre: String,
    val apellidoPaterno: String,
    val apellidoMaterno: String,
    val semestre: Int,
    val curriculum: String?,
    val carrera: Carrera?
)

data class RespuestaPerfil(
    val success: Boolean,
    val message: String,
    val usuario: Usuario
)