package com.example.linkedininterno

import com.google.gson.annotations.SerializedName

data class Vacante(
    @SerializedName("id") val idVacante: Int,
    @SerializedName("id_empresa") val idEmpresa: Int,
    @SerializedName("titulo") val titulo: String,
    @SerializedName("descripcion") val descripcion: String,
    @SerializedName("ubicacion") val ubicacion: String?,
    @SerializedName("modalidad") val modalidad: String?,
    @SerializedName("salario") val salario: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("updated_at") val updatedAt: String?,
    @SerializedName("empresa") val empresa: Empresa?,
    @SerializedName(value = "esMia", alternate = ["es_mia"]) val esMia: Boolean? = false
)