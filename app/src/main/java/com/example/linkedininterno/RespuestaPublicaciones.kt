package com.example.linkedininterno

import com.google.gson.annotations.SerializedName

data class RespuestaPublicaciones(
    @SerializedName("success") val success: Boolean?,
    @SerializedName("message") val message: String?,
    @SerializedName("publicaciones") val publicacionesPagina: PaginacionPublicaciones?
)

data class PaginacionPublicaciones(
    @SerializedName("current_page") val currentPage: Int?,
    @SerializedName("data") val data: List<Publicacion>?
)