package com.example.linkedininterno

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.VideoView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.NumberFormat
import java.util.Locale

class FeedAdapter(
    private var listaFeed: List<FeedItem>,
    private val onEditarPublicacion: (FeedItem) -> Unit,
    private val onEliminarPublicacion: (FeedItem) -> Unit,
    private val onEditarVacante: (FeedItem) -> Unit,
    private val onEliminarVacante: (FeedItem) -> Unit,
    private val onPerfilClick: (Int) -> Unit,
    private val onVerVacanteClick: (FeedItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TIPO_PUBLICACION = 1
    private val TIPO_VACANTE = 2

    private val BASE_STORAGE_URL =
        "http://10.0.2.2:8000/storage/"

    override fun getItemViewType(position: Int): Int {
        return if (listaFeed[position].tipo == "vacante") {
            TIPO_VACANTE
        } else {
            TIPO_PUBLICACION
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {

        return if (viewType == TIPO_VACANTE) {

            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_vacante, parent, false)

            VacanteViewHolder(view)

        } else {

            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_publicacion, parent, false)

            PublicacionViewHolder(view)
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {

        val item = listaFeed[position]

        if (holder is PublicacionViewHolder) {

            val usuario = item.usuario

            val nombreAutor = when {
                usuario?.alumno != null ->
                    "${usuario.alumno.nombre ?: ""} ${usuario.alumno.apellidoPaterno ?: ""}".trim()

                usuario?.empresa != null ->
                    usuario.empresa.nombreEmpresa ?: "Empresa"

                else -> "Usuario"
            }

            holder.tvNombreAutor.text =
                nombreAutor.ifEmpty { "Usuario" }

            val fechaOriginal =
                item.publishedAt ?: item.createdAt ?: item.updatedAt ?: ""

            holder.tvTiempoPost.text =
                formatearFecha(fechaOriginal)

            // Avatar del usuario
            val avatar = usuario?.avatar

            if (!avatar.isNullOrEmpty()) {

                Glide.with(holder.itemView.context)
                    .load(construirUrl(avatar))
                    .circleCrop()
                    .into(holder.ivAvatarAutor)

            } else {

                holder.ivAvatarAutor.setImageResource(
                    R.mipmap.ic_launcher_round
                )
            }

            val listenerPerfil = View.OnClickListener {
                item.idUsuario?.let { id ->
                    onPerfilClick(id)
                }
            }

            holder.ivAvatarAutor.setOnClickListener(listenerPerfil)
            holder.tvNombreAutor.setOnClickListener(listenerPerfil)

            // Datos de la publicación
            holder.tvTitulo.text = item.titulo ?: ""
            holder.tvContenido.text = item.contenido ?: ""

            // Imagen o video
            val pathMultimedia = item.multimedia

            if (!pathMultimedia.isNullOrEmpty()) {

                val urlCompleta = construirUrl(pathMultimedia)

                val esVideo =
                    urlCompleta.endsWith(".mp4", true) ||
                            urlCompleta.endsWith(".mov", true)

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

                    Glide.with(holder.itemView.context)
                        .load(urlCompleta)
                        .into(holder.ivImagenPost)
                }

            } else {

                holder.vvVideoPost.stopPlayback()
                holder.vvVideoPost.visibility = View.GONE
                holder.ivImagenPost.visibility = View.GONE
            }

            // Botones de edición y eliminación
            if (item.esMia == true) {

                holder.btnEditar.visibility = View.VISIBLE
                holder.btnEliminar.visibility = View.VISIBLE

                holder.btnEditar.setOnClickListener {
                    onEditarPublicacion(item)
                }

                holder.btnEliminar.setOnClickListener {
                    onEliminarPublicacion(item)
                }

            } else {

                holder.btnEditar.visibility = View.GONE
                holder.btnEliminar.visibility = View.GONE

                holder.btnEditar.setOnClickListener(null)
                holder.btnEliminar.setOnClickListener(null)
            }
        }

        else if (holder is VacanteViewHolder) {

            val empresa = item.usuario?.empresa

            val nombreEmpresa =
                empresa?.nombreEmpresa ?: "Empresa confidencial"

            // OBTENER MODALIDAD DINÁMICAMENTE PARA MOSTRARLA JUNTO A LA EMPRESA
            val nombreModalidad = item.modalidadObj?.nombre
                ?: item.modalidadTexto
                ?: ""

            holder.tvEmpresaUbicacion.text = if (nombreModalidad.isNotBlank()) {
                "$nombreEmpresa · $nombreModalidad"
            } else {
                nombreEmpresa
            }

            holder.tvTituloVacante.text =
                item.puesto?.takeIf { it.isNotBlank() }
                    ?: item.titulo?.takeIf { it.isNotBlank() }
                            ?: "Vacante sin título"

            // Salario mínimo y máximo
            holder.tvSalarioVacante.text =
                obtenerSalario(item)

            holder.tvDescripcionVacante.text =
                item.contenido?.takeIf { it.isNotBlank() }
                    ?: item.descripcion
                            ?: ""

            // GESTIÓN DE MULTIMEDIA EN VACANTES (IMAGEN / VIDEO)
            val pathMultimediaVacante = item.multimedia

            if (!pathMultimediaVacante.isNullOrEmpty()) {
                val urlCompletaVacante = construirUrl(pathMultimediaVacante)
                val esVideoVacante = urlCompletaVacante.endsWith(".mp4", true) ||
                        urlCompletaVacante.endsWith(".mov", true) ||
                        urlCompletaVacante.endsWith(".mkv", true) ||
                        urlCompletaVacante.endsWith(".avi", true)

                if (esVideoVacante) {
                    holder.ivImagenVacante.visibility = View.GONE
                    holder.vvVideoVacante.visibility = View.VISIBLE
                    holder.vvVideoVacante.setVideoPath(urlCompletaVacante)
                    holder.vvVideoVacante.setOnPreparedListener { mp ->
                        mp.isLooping = true
                        holder.vvVideoVacante.start()
                    }
                } else {
                    holder.vvVideoVacante.stopPlayback()
                    holder.vvVideoVacante.visibility = View.GONE
                    holder.ivImagenVacante.visibility = View.VISIBLE

                    Glide.with(holder.itemView.context)
                        .load(urlCompletaVacante)
                        .into(holder.ivImagenVacante)
                }
            } else {
                holder.vvVideoVacante.stopPlayback()
                holder.vvVideoVacante.visibility = View.GONE
                holder.ivImagenVacante.visibility = View.GONE
            }

            // Fecha de publicación
            val fechaOriginal =
                item.publishedAt ?: item.createdAt ?: item.updatedAt ?: ""

            holder.tvTiempoVacante.text =
                formatearFecha(fechaOriginal)

            // Logo de empresa
            val avatar = item.usuario?.avatar

            if (!avatar.isNullOrEmpty()) {

                Glide.with(holder.itemView.context)
                    .load(construirUrl(avatar))
                    .circleCrop()
                    .into(holder.ivLogoEmpresa)

            } else {

                holder.ivLogoEmpresa.setImageResource(
                    R.mipmap.ic_launcher_round
                )
            }

            holder.btnVerVacante.setOnClickListener {
                onVerVacanteClick(item)
            }

            // Opciones de edición y eliminación
            if (item.esMia == true) {

                holder.btnOpcionesVacante.visibility = View.VISIBLE

                holder.btnOpcionesVacante.setOnClickListener { view ->

                    val popup = PopupMenu(view.context, view)

                    popup.menu.add(0, 1, 0, "Editar Vacante")
                    popup.menu.add(0, 2, 0, "Eliminar Vacante")

                    popup.setOnMenuItemClickListener { menuItem ->

                        when (menuItem.itemId) {

                            1 -> onEditarVacante(item)

                            2 -> onEliminarVacante(item)
                        }

                        true
                    }

                    popup.show()
                }

            } else {

                holder.btnOpcionesVacante.visibility = View.GONE
                holder.btnOpcionesVacante.setOnClickListener(null)
            }
        }
    }

    private fun obtenerSalario(item: FeedItem): String {

        val minimo = item.sueldoMin
            ?.trim()
            ?.toBigDecimalOrNull()

        val maximo = item.sueldoMax
            ?.trim()
            ?.toBigDecimalOrNull()

        val formato = NumberFormat.getNumberInstance(
            Locale("es", "MX")
        ).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 0
        }

        return when {

            minimo != null && maximo != null -> {

                if (minimo.compareTo(maximo) == 0) {
                    "\$${formato.format(minimo)} MXN"
                } else {
                    "\$${formato.format(minimo)} - \$${formato.format(maximo)} MXN"
                }
            }

            minimo != null ->
                "Sueldo MIN \$${formato.format(minimo)} MXN"

            maximo != null ->
                "Sueldo MAX \$${formato.format(maximo)} MXN"

            !item.salario.isNullOrBlank() ->
                item.salario

            else ->
                "Salario no mostrado"
        }
    }

    private fun construirUrl(path: String): String {

        return if (path.startsWith("http")) {
            path
        } else {
            BASE_STORAGE_URL + path.trimStart('/')
        }
    }

    private fun formatearFecha(fecha: String): String {

        return if (fecha.length >= 16) {
            fecha.substring(0, 16).replace("T", " ")
        } else {
            fecha.ifEmpty { "Reciente" }
        }
    }

    override fun getItemCount(): Int = listaFeed.size

    // Actualizar información del RecyclerView
    fun actualizarDatos(nuevaLista: List<FeedItem>) {
        listaFeed = nuevaLista
        notifyDataSetChanged()
    }

    // VIEW HOLDER DE PUBLICACIONES
    class PublicacionViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val ivAvatarAutor: ImageView =
            itemView.findViewById(R.id.ivAvatarAutor)

        val tvNombreAutor: TextView =
            itemView.findViewById(R.id.tvNombreAutor)

        val tvTiempoPost: TextView =
            itemView.findViewById(R.id.tvTiempoPost)

        val tvTitulo: TextView =
            itemView.findViewById(R.id.tvTituloPost)

        val tvContenido: TextView =
            itemView.findViewById(R.id.tvContenidoPost)

        val ivImagenPost: ImageView =
            itemView.findViewById(R.id.ivImagenPost)

        val vvVideoPost: VideoView =
            itemView.findViewById(R.id.vvVideoPost)

        val btnEditar: ImageButton =
            itemView.findViewById(R.id.btnEditarPost)

        val btnEliminar: ImageButton =
            itemView.findViewById(R.id.btnEliminarPost)
    }

    // VIEW HOLDER DE VACANTES
    class VacanteViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val ivLogoEmpresa: ImageView =
            itemView.findViewById(R.id.ivLogoEmpresaVacante)

        val tvTituloVacante: TextView =
            itemView.findViewById(R.id.tvTituloVacante)

        val tvEmpresaUbicacion: TextView =
            itemView.findViewById(R.id.tvEmpresaUbicacion)

        val tvTiempoVacante: TextView =
            itemView.findViewById(R.id.tvTiempoVacante)

        val tvSalarioVacante: TextView =
            itemView.findViewById(R.id.tvSalarioVacante)

        val tvDescripcionVacante: TextView =
            itemView.findViewById(R.id.tvDescripcionVacante)

        // Vistas multimedia agregadas
        val ivImagenVacante: ImageView =
            itemView.findViewById(R.id.ivImagenVacante)

        val vvVideoVacante: VideoView =
            itemView.findViewById(R.id.vvVideoVacante)

        val btnOpcionesVacante: ImageButton =
            itemView.findViewById(R.id.btnOpcionesVacante)

        val btnVerVacante: Button =
            itemView.findViewById(R.id.btnVerVacante)
    }
}