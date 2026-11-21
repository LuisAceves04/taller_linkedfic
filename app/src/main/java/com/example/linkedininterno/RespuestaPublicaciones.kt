package com.example.linkedininterno

import com.google.gson.annotations.SerializedName

data class RespuestaPublicaciones(
    @SerializedName(value = "success") val success: Boolean?,
    @SerializedName(value = "message") val message: String?,
    @SerializedName(value = "publicaciones") val publicacionesPagina: PaginacionPublicaciones?
)

data class PaginacionPublicaciones(
    @SerializedName(value = "current_page") val currentPage: Int?,
    @SerializedName(value = "data") val data: List<FeedItem?>?
)