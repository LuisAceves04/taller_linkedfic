package com.example.linkedininterno

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class NotificacionesAdapter(private val listaNotificaciones: List<Notificacion>) :
    RecyclerView.Adapter<NotificacionesAdapter.NotificacionViewHolder>() {

    class NotificacionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivEmpresaAvatar: ImageView = itemView.findViewById(R.id.ivEmpresaAvatar)
        val tvMensajeNotif: TextView = itemView.findViewById(R.id.tvMensajeNotif)
        val tvFechaNotif: TextView = itemView.findViewById(R.id.tvFechaNotif)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificacionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notificacion, parent, false)
        return NotificacionViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificacionViewHolder, position: Int) {
        val notif = listaNotificaciones[position]

        holder.tvMensajeNotif.text = notif.mensaje ?: "Tienes una nueva notificación"
        holder.tvFechaNotif.text = notif.created_at ?: ""

        val avatarUrl = notif.empresa?.avatar
        if (!avatarUrl.isNullOrEmpty()) {
            val urlCompleta = if (avatarUrl.startsWith("http")) avatarUrl else "http://10.0.2.2:8000/storage/$avatarUrl"
            Glide.with(holder.itemView.context)
                .load(urlCompleta)
                .centerCrop()
                .into(holder.ivEmpresaAvatar)
        } else {
            holder.ivEmpresaAvatar.setImageResource(android.R.drawable.ic_menu_myplaces)
        }
    }

    override fun getItemCount(): Int = listaNotificaciones.size
}