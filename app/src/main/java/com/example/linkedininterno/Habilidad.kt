package com.example.linkedininterno

import com.google.gson.annotations.SerializedName

data class Habilidad(
    @SerializedName("idHabilidad")
    val id: Int?,
    val nombre: String?
)