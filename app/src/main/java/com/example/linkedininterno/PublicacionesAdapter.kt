package com.example.linkedininterno

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.VideoView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class PublicacionesAdapter(
    private val listaPublicaciones: List<Publicacion>,
    private val onEditarClick: (Publicacion) -> Unit,
    private val onEliminarClick: (Publicacion) -> Unit,
    private val onPerfilClick: (Int) -> Unit
) : RecyclerView.Adapter<PublicacionesAdapter.PublicacionViewHolder>() {

    class PublicacionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivAvatarAutor: ImageView = itemView.findViewById(R.id.ivAvatarAutor)
        val tvNombreAutor: TextView = itemView.findViewById(R.id.tvNombreAutor)
        val tvTiempoPost: TextView = itemView.findViewById(R.id.tvTiempoPost)
        val tvTitulo: TextView = itemView.findViewById(R.id.tvTituloPost)
        val tvContenido: TextView = itemView.findViewById(R.id.tvContenidoPost)
        val ivImagenPost: ImageView = itemView.findViewById(R.id.ivImagenPost)
        val vvVideoPost: VideoView = itemView.findViewById(R.id.vvVideoPost)
        val btnEditar: ImageButton = itemView.findViewById(R.id.btnEditarPost)
        val btnEliminar: ImageButton = itemView.findViewById(R.id.btnEliminarPost)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PublicacionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_publicacion, parent, false)
        return PublicacionViewHolder(view)
    }

    override fun onBindViewHolder(holder: PublicacionViewHolder, position: Int) {
        val publicacion = listaPublicaciones[position]

        // Nombre del autor
        val usuario = publicacion.usuario
        val nombreAutor = when {
            usuario?.alumno != null -> "${usuario.alumno.nombre ?: ""} ${usuario.alumno.apellidoPaterno ?: ""}".trim()
            usuario?.empresa != null -> usuario.empresa.nombreEmpresa ?: "Empresa"
            else -> "Usuario"
        }
        holder.tvNombreAutor.text = if (nombreAutor.isNotEmpty()) nombreAutor else "Usuario"

        // Cargar Avatar del Autor
        val BASE_STORAGE_URL = "http://10.0.2.2:8000/storage/"
        if (!usuario?.avatar.isNullOrEmpty()) {
            val urlAvatar = if (usuario!!.avatar.startsWith("http")) usuario.avatar else BASE_STORAGE_URL + usuario.avatar
            Glide.with(holder.itemView.context)
                .load(urlAvatar)
                .circleCrop()
                .into(holder.ivAvatarAutor)
        } else {
            holder.ivAvatarAutor.setImageResource(R.mipmap.ic_launcher_round)
        }

        // EVENTO DE CLIC EN FOTO O NOMBRE DEL AUTOR PARA REDIRIGIR AL PERFIL
        val listenerPerfil = View.OnClickListener {
            publicacion.idUsuario?.let { id -> onPerfilClick(id) }
        }
        holder.ivAvatarAutor.setOnClickListener(listenerPerfil)
        holder.tvNombreAutor.setOnClickListener(listenerPerfil)

        // Título y Contenido
        holder.tvTitulo.text = publicacion.titulo
        holder.tvContenido.text = publicacion.contenido

        // Manejo de Multimedia (Imagen o Video)
        val pathMultimedia = publicacion.multimedia
        if (!pathMultimedia.isNullOrEmpty()) {
            val urlCompleta = if (pathMultimedia.startsWith("http")) pathMultimedia else BASE_STORAGE_URL + pathMultimedia
            val esVideo = urlCompleta.endsWith(".mp4", ignoreCase = true) ||
                    urlCompleta.endsWith(".mov", ignoreCase = true) ||
                    urlCompleta.endsWith(".mkv", ignoreCase = true) ||
                    urlCompleta.endsWith(".avi", ignoreCase = true) ||
                    urlCompleta.endsWith(".webm", ignoreCase = true)

            if (esVideo) {
                holder.ivImagenPost.visibility = View.GONE
                holder.vvVideoPost.visibility = View.VISIBLE
                holder.vvVideoPost.setVideoPath(urlCompleta)
                holder.vvVideoPost.setOnPreparedListener { mp ->
                    mp.isLooping = true
                    holder.vvVideoPost.start()
                }
            } else {
                holder.vvVideoPost.stopPlayback()
                holder.vvVideoPost.visibility = View.GONE
                holder.ivImagenPost.visibility = View.VISIBLE
                Glide.with(holder.itemView.context).load(urlCompleta).into(holder.ivImagenPost)
            }
        } else {
            holder.vvVideoPost.stopPlayback()
            holder.vvVideoPost.visibility = View.GONE
            holder.ivImagenPost.visibility = View.GONE
        }

        if (publicacion.esMia == true) {
            holder.btnEditar.visibility = View.VISIBLE
            holder.btnEliminar.visibility = View.VISIBLE

            holder.btnEditar.setOnClickListener { onEditarClick(publicacion) }
            holder.btnEliminar.setOnClickListener { onEliminarClick(publicacion) }
        } else {
            holder.btnEditar.visibility = View.GONE
            holder.btnEliminar.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = listaPublicaciones.size
}