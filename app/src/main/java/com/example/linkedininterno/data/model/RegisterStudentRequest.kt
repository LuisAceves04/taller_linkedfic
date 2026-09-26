package com.example.linkedininterno.data.model

data class RegisterStudentRequest(
    val idRol: Int = 1,
    val email: String,
    val password: String,
    val telefono: String,
    val sobreMi: String,
    val nombre: String,
    val apellidoPaterno: String,
    val apellidoMaterno: String,
    val idCarrera: Int
)
