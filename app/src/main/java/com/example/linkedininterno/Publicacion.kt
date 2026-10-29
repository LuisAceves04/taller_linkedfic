package com.example.linkedininterno

import com.google.gson.annotations.SerializedName

data class Publicacion(
    @SerializedName("idPublicacion") val idPublicacion: Int,
    @SerializedName("idUsuario") val idUsuario: Int,
    @SerializedName("titulo") val titulo: String,
    @SerializedName("contenido") val contenido: String,
    @SerializedName("multimedia") val multimedia: String?,
    @SerializedName("published_at") val publishedAt: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("usuario") val usuario: Usuario?,
    @SerializedName("updated_at") val updatedAt: String?,
    @SerializedName(value = "esMia", alternate = ["es_mia"]) val esMia: Boolean? = false
)