package com.example.linkedininterno

import com.google.gson.annotations.SerializedName

data class Conversacion(
    @SerializedName("interlocutor_id") val interlocutorId: String,
    @SerializedName("ultimo_mensaje") val ultimoMensaje: String? = null,
    @SerializedName("fecha") val fecha: String? = null,
    @SerializedName("no_leidos") val noLeidos: Int? = 0,
    @SerializedName("usuario") val usuario: Usuario? = null
)

data class RespuestaConversaciones(
    @SerializedName("success") val success: Boolean,
    @SerializedName("conversaciones") val conversaciones: List<Conversacion>?
)