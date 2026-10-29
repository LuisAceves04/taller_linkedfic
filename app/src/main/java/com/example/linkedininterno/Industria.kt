package com.example.linkedininterno

import com.google.gson.annotations.SerializedName

data class Industria(
    @SerializedName("idIndustria")
    val idIndustria: Int,
    @SerializedName("industria")
    val industria: String
)

data class RespuestaIndustrias(
    @SerializedName("industrias")
    val industrias: List<Industria>
)