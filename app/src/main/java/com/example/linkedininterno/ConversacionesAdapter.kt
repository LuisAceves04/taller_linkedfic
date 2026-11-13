package com.example.linkedininterno

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.linkedininterno.network.RetrofitClient
import com.google.android.material.card.MaterialCardView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ConversacionesAdapter(
    private var conversaciones: List<Conversacion>,
    private val token: String,
    private val onChatClick: (Conversacion) -> Unit
) : RecyclerView.Adapter<ConversacionesAdapter.ConversacionViewHolder>() {

    class ConversacionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAvatarInitials: TextView = view.findViewById(R.id.tvAvatarInitials)
        val ivAvatarChat: ImageView = view.findViewById(R.id.ivAvatarChat)
        val tvConversationName: TextView = view.findViewById(R.id.tvConversationName)
        val tvConversationPreview: TextView = view.findViewById(R.id.tvConversationPreview)
        val tvConversationTime: TextView = view.findViewById(R.id.tvConversationTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversacionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversacion, parent, false)
        return ConversacionViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConversacionViewHolder, position: Int) {
        val conversacion = conversaciones[position]
        val idInterlocutor = conversacion.interlocutorId

        holder.tvConversationName.text = "Cargando..."
        holder.tvConversationPreview.text = conversacion.ultimoMensaje ?: "Sin mensaje"
        holder.tvConversationTime.text = conversacion.fecha?.takeIf { it.length >= 16 }?.substring(11, 16) ?: ""
        holder.tvAvatarInitials.visibility = View.VISIBLE
        holder.ivAvatarChat.visibility = View.GONE
        holder.tvAvatarInitials.text = "..."

        RetrofitClient.instance.obtenerPerfilPorId("Bearer $token", idInterlocutor.toInt()).enqueue(object : Callback<RespuestaPerfil> {
            override fun onResponse(call: Call<RespuestaPerfil>, response: Response<RespuestaPerfil>) {
                if (response.isSuccessful && response.body() != null) {
                    val usuario = response.body()!!.usuario ?: return

                    val nombreReal = when {
                        usuario.alumno != null -> "${usuario.alumno.nombre ?: ""} ${usuario.alumno.apellidoPaterno ?: ""}".trim()
                        usuario.empresa != null -> usuario.empresa.nombreEmpresa ?: "Empresa"
                        else -> "Usuario $idInterlocutor"
                    }

                    holder.tvConversationName.text = if (nombreReal.isBlank()) "Usuario $idInterlocutor" else nombreReal

                    val avatarUrl = usuario.avatar
                    if (!avatarUrl.isNullOrEmpty()) {
                        holder.tvAvatarInitials.visibility = View.GONE
                        holder.ivAvatarChat.visibility = View.VISIBLE
                        val urlCompleta = if (avatarUrl.startsWith("http")) avatarUrl else "http://10.0.2.2:8000/storage/$avatarUrl"

                        Glide.with(holder.itemView.context)
                            .load(urlCompleta)
                            .centerCrop()
                            .into(holder.ivAvatarChat)
                    } else {
                        holder.tvAvatarInitials.visibility = View.VISIBLE
                        holder.ivAvatarChat.visibility = View.GONE
                        holder.tvAvatarInitials.text = (holder.tvConversationName.text.firstOrNull() ?: 'U').toString().uppercase()
                    }
                } else {
                    holder.tvConversationName.text = "Usuario $idInterlocutor"
                    holder.tvAvatarInitials.text = "U"
                }
            }

            override fun onFailure(call: Call<RespuestaPerfil>, t: Throwable) {
                holder.tvConversationName.text = "Usuario $idInterlocutor"
                holder.tvAvatarInitials.text = "U"
            }
        })

        holder.itemView.setOnClickListener {
            onChatClick(conversacion)
        }
    }

    override fun getItemCount(): Int = conversaciones.size

    fun actualizarDatos(nuevasConversaciones: List<Conversacion>) {
        this.conversaciones = nuevasConversaciones
        notifyDataSetChanged()
    }
}