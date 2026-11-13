package com.example.linkedininterno

import com.google.gson.annotations.SerializedName

// Modelo para publicaciones y vacantes del Feed
data class FeedItem(

    // --- CAMPOS COMPARTIDOS ---
    @SerializedName("tipo")
    val tipo: String = "",

    @SerializedName("esMia")
    val esMia: Boolean? = false,

    @SerializedName("idUsuario")
    val idUsuario: Int? = null,

    @SerializedName("published_at")
    val publishedAt: String? = null,

    @SerializedName("created_at")
    val createdAt: String? = null,

    @SerializedName("updated_at")
    val updatedAt: String? = null,

    @SerializedName("usuario")
    val usuario: Usuario? = null,

    // --- CAMPOS DE PUBLICACIONES ---
    @SerializedName("idPublicacion")
    val idPublicacion: Int? = null,

    @SerializedName("titulo")
    val titulo: String? = null,

    @SerializedName("contenido")
    val contenido: String? = null,

    @SerializedName("multimedia")
    val multimedia: String? = null,

    // --- CAMPOS DE VACANTES ---
    @SerializedName("idVacante")
    val idVacante: Int? = null,

    @SerializedName("puesto")
    val puesto: String? = null,

    @SerializedName("idModalidad")
    val idModalidad: Int? = null,

    @SerializedName(value = "modalidad", alternate = ["modalidadObj"])
    val modalidadObj: Modalidad? = null,

    @SerializedName(value = "modalidad_texto", alternate = ["modalidadTexto"])
    val modalidadTexto: String? = null,

    @SerializedName("sueldo_min")
    val sueldoMin: String? = null,

    @SerializedName("sueldo_max")
    val sueldoMax: String? = null,

    @SerializedName("descripcion")
    val descripcion: String? = null,

    @SerializedName("salario")
    val salario: String? = null
)

// Respuesta general del Feed
data class RespuestaFeed(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("feed")
    val feed: FeedPagina?
)

// Paginación del Feed
data class FeedPagina(
    @SerializedName("data")
    val data: List<FeedItem>
)