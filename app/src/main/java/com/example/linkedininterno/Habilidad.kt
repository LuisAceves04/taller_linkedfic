package com.example.linkedininterno

import com.google.gson.annotations.SerializedName

data class Habilidad(
    @SerializedName("idHabilidad")
    val id: Int?,
    val nombre: String?,
    val pivot: PivotHabilidad?
)

data class PivotHabilidad(
    @SerializedName("id_alumno_habilidad")
    val idAlumnoHabilidad: Int?,

    @SerializedName("idNivel")
    val idNivel: Int?
)