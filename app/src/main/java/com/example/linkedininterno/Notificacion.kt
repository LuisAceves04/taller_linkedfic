package com.example.linkedininterno

data class Notificacion(
    val id: String?,
    val mensaje: String?,
    val empresa: EmpresaInfo?,
    val created_at: String?
)

data class EmpresaInfo(
    val id: String?,
    val nombreEmpresa: String?,
    val avatar: String?
)