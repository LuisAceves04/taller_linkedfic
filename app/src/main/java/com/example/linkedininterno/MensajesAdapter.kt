package com.example.linkedininterno

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MensajesAdapter(
    private var mensajes: List<Mensaje>,
    private val miIdUsuario: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TIPO_ENVIADO = 1
    private val TIPO_RECIBIDO = 2

    // ViewHolder para mis mensajes (Burbuja Azul)
    class MensajeEnviadoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMensaje: TextView = view.findViewById(R.id.tvMensajeEnviado)
        val tvHora: TextView = view.findViewById(R.id.tvHoraEnviado)
    }

    // ViewHolder para mensajes de la otra persona (Burbuja Gris)
    class MensajeRecibidoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMensaje: TextView = view.findViewById(R.id.tvMensajeRecibido)
        val tvHora: TextView = view.findViewById(R.id.tvHoraRecibido)
    }

    // Esta función decide qué tipo de burbuja usar comparando los IDs
    override fun getItemViewType(position: Int): Int {
        val mensaje = mensajes[position]
        return if (mensaje.remitenteId == miIdUsuario) {
            TIPO_ENVIADO
        } else {
            TIPO_RECIBIDO
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TIPO_ENVIADO) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_mensaje_enviado, parent, false)
            MensajeEnviadoViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_mensaje_recibido, parent, false)
            MensajeRecibidoViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mensaje = mensajes[position]
        val hora = mensaje.createdAt?.substring(11, 16) ?: ""

        if (holder is MensajeEnviadoViewHolder) {
            holder.tvMensaje.text = mensaje.mensaje
            holder.tvHora.text = hora
        } else if (holder is MensajeRecibidoViewHolder) {
            holder.tvMensaje.text = mensaje.mensaje
            holder.tvHora.text = hora
        }
    }

    override fun getItemCount(): Int = mensajes.size

    fun actualizarMensajes(nuevosMensajes: List<Mensaje>) {
        this.mensajes = nuevosMensajes
        notifyDataSetChanged()
    }
}