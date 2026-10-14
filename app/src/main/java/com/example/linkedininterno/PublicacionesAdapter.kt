package com.example.linkedininterno

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class PublicacionesAdapter(
    private var listaPublicaciones: List<Publicacion>,
    private val onEditarClick: (Publicacion) -> Unit,
    private val onEliminarClick: (Publicacion) -> Unit
) : RecyclerView.Adapter<PublicacionesAdapter.PublicacionViewHolder>() {

    class PublicacionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAutor: TextView = itemView.findViewById(R.id.tvAutorPost)
        val tvTiempo: TextView = itemView.findViewById(R.id.tvTiempoPost)
        val tvTitulo: TextView = itemView.findViewById(R.id.tvTituloPost)
        val tvContenido: TextView = itemView.findViewById(R.id.tvContenidoPost)
        val ivMultimedia: ImageView = itemView.findViewById(R.id.ivMultimediaPost)
        val btnOpciones: ImageView = itemView.findViewById(R.id.btnMenuOpciones)
        val ivAvatar: ImageView = itemView.findViewById(R.id.ivAvatarPost)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PublicacionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_publicacion, parent, false)
        return PublicacionViewHolder(view)
    }

    override fun onBindViewHolder(holder: PublicacionViewHolder, position: Int) {
        val post = listaPublicaciones[position]

        holder.tvTitulo.text = post.titulo
        holder.tvContenido.text = post.contenido


        val fechaCruda = post.updatedAt ?: post.publishedAt ?: post.createdAt

        if (!fechaCruda.isNullOrEmpty()) {
            try {
                val fechaLimpia = fechaCruda.substring(0, 19)
                val formatoEntrada = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                formatoEntrada.timeZone = java.util.TimeZone.getTimeZone("UTC")
                val fecha = formatoEntrada.parse(fechaLimpia)

                val formatoSalida = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                formatoSalida.timeZone = java.util.TimeZone.getDefault()

                var textoFecha = if (fecha != null) formatoSalida.format(fecha) else "Reciente"

                holder.tvTiempo.text = textoFecha

            } catch (e: Exception) {
                holder.tvTiempo.text = "Reciente"
            }
        } else {
            holder.tvTiempo.text = "Reciente"
        }

        val nombre = post.usuario?.alumno?.nombre ?: "Usuario"
        val apellido = post.usuario?.alumno?.apellidoPaterno ?: ""
        holder.tvAutor.text = "$nombre $apellido".trim()

        val BASE_STORAGE_URL = "http://10.0.2.2:8000/storage/"

        if (!post.multimedia.isNullOrEmpty()) {
            holder.ivMultimedia.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(BASE_STORAGE_URL + post.multimedia)
                .into(holder.ivMultimedia)
        } else {
            holder.ivMultimedia.visibility = View.GONE
        }

        if (!post.usuario?.avatar.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(BASE_STORAGE_URL + post.usuario?.avatar)
                .circleCrop()
                .into(holder.ivAvatar)
        } else {
            holder.ivAvatar.setImageResource(R.mipmap.ic_launcher_round)
        }

        holder.btnOpciones.setOnClickListener { view ->
            val popup = PopupMenu(view.context, view)
            popup.menu.add("Editar")
            popup.menu.add("Eliminar")
            popup.setOnMenuItemClickListener { item ->
                when (item.title) {
                    "Editar" -> onEditarClick(post)
                    "Eliminar" -> onEliminarClick(post)
                }
                true
            }
            popup.show()
        }
    }

    override fun getItemCount(): Int = listaPublicaciones.size

    fun actualizarLista(nuevaLista: List<Publicacion>) {
        listaPublicaciones = nuevaLista
        notifyDataSetChanged()
    }
}