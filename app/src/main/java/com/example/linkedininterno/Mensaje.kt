package com.example.linkedininterno

import com.google.gson.annotations.SerializedName

data class Mensaje(
    @SerializedName("_id") val id: String? = null,
    @SerializedName("remitente_id") val remitenteId: String? = null,
    @SerializedName("destinatario_id") val destinatarioId: String? = null,
    @SerializedName("mensaje") val mensaje: String? = null,
    @SerializedName("leido") val leido: Boolean? = false,
    @SerializedName("created_at") val createdAt: String? = null
)

data class RespuestaMensajes(
    @SerializedName("success") val success: Boolean,
    @SerializedName("mensajes") val mensajes: List<Mensaje>?
)

data class RespuestaEnviarMensaje(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String? = null,
    @SerializedName("mensaje") val mensaje: Mensaje? = null
)

data class PeticionEnviarMensaje(
    @SerializedName("destinatario_id") val destinatarioId: String,
    @SerializedName("mensaje") val mensaje: String
)