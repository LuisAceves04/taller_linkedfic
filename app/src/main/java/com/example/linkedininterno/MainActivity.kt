package com.example.linkedininterno

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.linkedininterno.network.RetrofitClient
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var rvPublicaciones: RecyclerView
    private lateinit var adapter: PublicacionesAdapter
    private val listaPublicaciones = mutableListOf<Publicacion>()

    private lateinit var etTituloPost: TextInputEditText
    private lateinit var etContenidoPost: TextInputEditText
    private lateinit var btnSeleccionarImagen: Button
    private lateinit var btnSeleccionarVideo: Button
    private lateinit var ivPreviewImagen: ImageView
    private lateinit var vvPreviewVideo: VideoView
    private lateinit var btnPublicar: Button

    private lateinit var tvFechaProgramada: TextView
    private lateinit var btnLimpiarFecha: ImageButton

    private var mediaUri: Uri? = null
    private var esVideoSeleccionado: Boolean = false

    private var nuevaMediaUri: Uri? = null
    private var esNuevoVideoEdicion: Boolean = false
    private var ivEditarPreviewRef: ImageView? = null
    private var vvEditarPreviewRef: VideoView? = null

    private var fechaProgramadaFormateada: String? = null

    // Selector para Imagen
    private val seleccionarImagenLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            mediaUri = uri
            esVideoSeleccionado = false
            vvPreviewVideo.visibility = View.GONE
            vvPreviewVideo.stopPlayback()

            ivPreviewImagen.setImageURI(uri)
            ivPreviewImagen.visibility = View.VISIBLE
        }
    }

    // Selector para Video
    private val seleccionarVideoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            mediaUri = uri
            esVideoSeleccionado = true
            ivPreviewImagen.visibility = View.GONE

            vvPreviewVideo.visibility = View.VISIBLE
            vvPreviewVideo.setVideoURI(uri)
            vvPreviewVideo.setOnPreparedListener { mp ->
                mp.isLooping = true
                vvPreviewVideo.start()
            }
        }
    }

    // Selector para Edición
    private val selectorEdicionLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            nuevaMediaUri = uri
            val mimeType = contentResolver.getType(uri) ?: ""
            esNuevoVideoEdicion = mimeType.startsWith("video")

            if (esNuevoVideoEdicion) {
                ivEditarPreviewRef?.visibility = View.GONE
                vvEditarPreviewRef?.visibility = View.VISIBLE
                vvEditarPreviewRef?.setVideoURI(uri)
                vvEditarPreviewRef?.setOnPreparedListener { mp ->
                    mp.isLooping = true
                    vvEditarPreviewRef?.start()
                }
            } else {
                vvEditarPreviewRef?.stopPlayback()
                vvEditarPreviewRef?.visibility = View.GONE
                ivEditarPreviewRef?.setImageURI(uri)
                ivEditarPreviewRef?.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_activity)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnProfile = findViewById<ImageButton>(R.id.btnGoToProfile)
        val btnLogout = findViewById<ImageButton>(R.id.btnLogout)
        etTituloPost = findViewById(R.id.etTituloPost)
        etContenidoPost = findViewById(R.id.etContenidoPost)
        btnSeleccionarImagen = findViewById(R.id.btnSeleccionarImagen)
        btnSeleccionarVideo = findViewById(R.id.btnSeleccionarVideo)
        ivPreviewImagen = findViewById(R.id.ivPreviewImagen)
        vvPreviewVideo = findViewById(R.id.vvPreviewVideo)
        btnPublicar = findViewById(R.id.btnPublicar)
        rvPublicaciones = findViewById(R.id.rvPublicaciones)
        tvFechaProgramada = findViewById(R.id.tvFechaProgramada)
        btnLimpiarFecha = findViewById(R.id.btnLimpiarFecha)

        // Feed de Publicaciones con redirección al perfil del autor
        rvPublicaciones.layoutManager = LinearLayoutManager(this)
        adapter = PublicacionesAdapter(
            listaPublicaciones,
            onEditarClick = { publicacion -> mostrarDialogoEditar(publicacion) },
            onEliminarClick = { publicacion -> mostrarDialogoEliminar(publicacion.idPublicacion) },
            onPerfilClick = { idUsuario -> abrirPerfilUsuario(idUsuario) }
        )
        rvPublicaciones.adapter = adapter

        tvFechaProgramada.setOnClickListener {
            mostrarSelectorFechaHora()
        }

        btnLimpiarFecha.setOnClickListener {
            limpiarFechaProgramada()
        }

        btnSeleccionarImagen.setOnClickListener {
            seleccionarImagenLauncher.launch("image/*")
        }

        btnSeleccionarVideo.setOnClickListener {
            seleccionarVideoLauncher.launch("video/*")
        }

        btnProfile.setOnClickListener {
            abrirPerfilSegunRol()
        }

        btnLogout.setOnClickListener {
            cerrarSesion()
        }

        btnPublicar.setOnClickListener {
            crearNuevaPublicacion()
        }

        cargarPublicaciones()
    }

    private fun abrirPerfilUsuario(idUsuario: Int) {
        val intent = Intent(this, ProfileActivity::class.java).apply {
            putExtra("ID_USUARIO", idUsuario)
        }
        startActivity(intent)
    }

    private fun mostrarSelectorFechaHora() {
        val calendario = Calendar.getInstance()
        val anio = calendario.get(Calendar.YEAR)
        val mes = calendario.get(Calendar.MONTH)
        val dia = calendario.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, year, month, dayOfMonth ->
            val hora = calendario.get(Calendar.HOUR_OF_DAY)
            val minuto = calendario.get(Calendar.MINUTE)

            val timePickerDialog = TimePickerDialog(this, { _, hourOfDay, minute ->
                fechaProgramadaFormateada = String.format(
                    Locale.US,
                    "%04d-%02d-%02d %02d:%02d:00",
                    year, month + 1, dayOfMonth, hourOfDay, minute
                )

                val minutoStr = String.format(Locale.US, "%02d", minute)
                tvFechaProgramada.text = "Programado para: $dayOfMonth/${month + 1}/$year $hourOfDay:$minutoStr"
                tvFechaProgramada.setTextColor(Color.parseColor("#0A66C2"))
                btnLimpiarFecha.visibility = View.VISIBLE

            }, hora, minuto, true)

            timePickerDialog.show()
        }, anio, mes, dia)

        datePickerDialog.datePicker.minDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun limpiarFechaProgramada() {
        fechaProgramadaFormateada = null
        tvFechaProgramada.text = "Programar publicación (Opcional)"
        tvFechaProgramada.setTextColor(Color.parseColor("#536471"))
        btnLimpiarFecha.visibility = View.GONE
    }

    private fun obtenerToken(): String {
        val prefs = getSharedPreferences("SesionUsuario", MODE_PRIVATE)
        return prefs.getString("TOKEN", "") ?: ""
    }

    private fun abrirPerfilSegunRol() {
        val prefs = getSharedPreferences("SesionUsuario", MODE_PRIVATE)
        val idRol = prefs.getInt("ID_ROL", prefs.getInt("idRol", 1))

        val intent = Intent(this, ProfileActivity::class.java).apply {
            putExtra("ID_ROL", idRol)
        }
        startActivity(intent)
    }

    private fun cargarPublicaciones() {
        val token = obtenerToken()
        if (token.isEmpty()) return

        RetrofitClient.instance.obtenerPublicaciones("Bearer $token").enqueue(object : Callback<RespuestaPublicaciones> {
            @SuppressLint("NotifyDataSetChanged")
            override fun onResponse(call: Call<RespuestaPublicaciones>, response: Response<RespuestaPublicaciones>) {
                if (response.isSuccessful && response.body() != null) {
                    val respuesta = response.body()!!
                    val publicacionesObtenidas = respuesta.publicacionesPagina?.data ?: emptyList()

                    val ahora = java.util.Date()
                    val formatoIso = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                    val formatoStandard = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())

                    val publicacionesVisibles = publicacionesObtenidas.filter { post ->
                        val fechaStr = post.publishedAt ?: return@filter true
                        try {
                            val fechaPost = if (fechaStr.contains("T")) {
                                formatoIso.parse(fechaStr.substring(0, 19))
                            } else {
                                formatoStandard.parse(fechaStr)
                            }
                            fechaPost != null && !fechaPost.after(ahora)
                        } catch (e: Exception) {
                            true
                        }
                    }

                    listaPublicaciones.clear()
                    listaPublicaciones.addAll(publicacionesVisibles)
                    adapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this@MainActivity, "Error al cargar feed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<RespuestaPublicaciones>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error de red: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun crearNuevaPublicacion() {
        val titulo = etTituloPost.text.toString().trim()
        val contenido = etContenidoPost.text.toString().trim()

        val prefs = getSharedPreferences("SesionUsuario", MODE_PRIVATE)
        val token = prefs.getString("TOKEN", "") ?: ""
        val idUsuario = prefs.getInt("ID_USUARIO", -1)

        if (titulo.isEmpty() || contenido.isEmpty()) {
            Toast.makeText(this, "Escribe un título y contenido", Toast.LENGTH_SHORT).show()
            return
        }

        if (idUsuario == -1) {
            Toast.makeText(this, "Falta el ID del usuario. Cierra sesión y vuelve a entrar.", Toast.LENGTH_LONG).show()
            return
        }

        val idUsuarioBody = idUsuario.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val tituloBody = titulo.toRequestBody("text/plain".toMediaTypeOrNull())
        val contenidoBody = contenido.toRequestBody("text/plain".toMediaTypeOrNull())
        val publishedAtBody = fechaProgramadaFormateada?.toRequestBody("text/plain".toMediaTypeOrNull())

        var multimediaPart: MultipartBody.Part? = null

        if (mediaUri != null) {
            try {
                val mimeType = contentResolver.getType(mediaUri!!) ?: if (esVideoSeleccionado) "video/mp4" else "image/jpeg"
                val nombreArchivo = if (esVideoSeleccionado) "post_temp_video.mp4" else "post_temp_image.jpg"

                val file = File(cacheDir, nombreArchivo)
                contentResolver.openInputStream(mediaUri!!)?.use { inputStream ->
                    FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                multimediaPart = MultipartBody.Part.createFormData("multimedia", file.name, requestFile)
            } catch (_: Exception) {
                Toast.makeText(this, "Error al procesar el archivo multimedia", Toast.LENGTH_SHORT).show()
            }
        }

        RetrofitClient.instance.crearPublicacionConImagen(
            token = "Bearer $token",
            idUsuario = idUsuarioBody,
            titulo = tituloBody,
            contenido = contenidoBody,
            multimedia = multimediaPart,
            publishedAt = publishedAtBody
        ).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful) {
                    val mensaje = if (fechaProgramadaFormateada != null) "¡Publicación programada!" else "¡Publicado con éxito!"
                    Toast.makeText(this@MainActivity, mensaje, Toast.LENGTH_SHORT).show()

                    etTituloPost.text?.clear()
                    etContenidoPost.text?.clear()
                    limpiarFechaProgramada()
                    mediaUri = null
                    ivPreviewImagen.visibility = View.GONE
                    vvPreviewVideo.stopPlayback()
                    vvPreviewVideo.visibility = View.GONE
                    cargarPublicaciones()
                } else {
                    Toast.makeText(this@MainActivity, "Error al publicar", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun mostrarDialogoEditar(publicacion: Publicacion) {
        val dialogView = layoutInflater.inflate(R.layout.activity_editar_publicacion, null)

        val etTitulo = dialogView.findViewById<TextInputEditText>(R.id.etEditarTitulo)
        val etContenido = dialogView.findViewById<TextInputEditText>(R.id.etEditarContenido)
        val btnSeleccionarImagen = dialogView.findViewById<Button>(R.id.btnEditarSeleccionarImagen)
        val ivPreview = dialogView.findViewById<ImageView>(R.id.ivEditarPreview)
        val vvPreview = dialogView.findViewById<VideoView>(R.id.vvEditarPreview)
        val btnCancelar = dialogView.findViewById<Button>(R.id.btnCancelarEdicion)
        val btnGuardar = dialogView.findViewById<Button>(R.id.btnGuardarEdicion)

        etTitulo.setText(publicacion.titulo)
        etContenido.setText(publicacion.contenido)

        nuevaMediaUri = null
        esNuevoVideoEdicion = false
        ivEditarPreviewRef = ivPreview
        vvEditarPreviewRef = vvPreview

        val pathMultimedia = publicacion.multimedia
        if (!pathMultimedia.isNullOrEmpty()) {
            val BASE_STORAGE_URL = "http://10.0.2.2:8000/storage/"
            val urlCompleta = if (pathMultimedia.startsWith("http")) pathMultimedia else BASE_STORAGE_URL + pathMultimedia

            val esVideo = urlCompleta.endsWith(".mp4", ignoreCase = true) ||
                    urlCompleta.endsWith(".mov", ignoreCase = true) ||
                    urlCompleta.endsWith(".mkv", ignoreCase = true) ||
                    urlCompleta.endsWith(".avi", ignoreCase = true) ||
                    urlCompleta.endsWith(".webm", ignoreCase = true)

            if (esVideo) {
                ivPreview.visibility = View.GONE
                vvPreview.visibility = View.VISIBLE
                vvPreview.setVideoPath(urlCompleta)
                vvPreview.setOnPreparedListener { mp ->
                    mp.isLooping = true
                    vvPreview.start()
                }
            } else {
                vvPreview.visibility = View.GONE
                ivPreview.visibility = View.VISIBLE
                com.bumptech.glide.Glide.with(this).load(urlCompleta).into(ivPreview)
            }
        } else {
            ivPreview.visibility = View.GONE
            vvPreview.visibility = View.GONE
        }

        btnSeleccionarImagen.setOnClickListener {
            selectorEdicionLauncher.launch("*/*")
        }

        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancelar.setOnClickListener {
            vvPreview.stopPlayback()
            dialog.dismiss()
        }

        btnGuardar.setOnClickListener {
            val nuevoTitulo = etTitulo.text.toString().trim()
            val nuevoContenido = etContenido.text.toString().trim()
            val token = obtenerToken()

            if (nuevoTitulo.isEmpty() || nuevoContenido.isEmpty()) {
                Toast.makeText(this, "Completa ambos campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val methodBody = "PUT".toRequestBody("text/plain".toMediaTypeOrNull())
            val tituloBody = nuevoTitulo.toRequestBody("text/plain".toMediaTypeOrNull())
            val contenidoBody = nuevoContenido.toRequestBody("text/plain".toMediaTypeOrNull())

            var multimediaPart: MultipartBody.Part? = null
            if (nuevaMediaUri != null) {
                try {
                    val mimeType = contentResolver.getType(nuevaMediaUri!!) ?: if (esNuevoVideoEdicion) "video/mp4" else "image/jpeg"
                    val nombreArchivo = if (esNuevoVideoEdicion) "edit_temp_video.mp4" else "edit_temp_image.jpg"

                    val file = File(cacheDir, nombreArchivo)
                    contentResolver.openInputStream(nuevaMediaUri!!)?.use { inputStream ->
                        FileOutputStream(file).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                    multimediaPart = MultipartBody.Part.createFormData("multimedia", file.name, requestFile)
                } catch (_: Exception) {
                    Toast.makeText(this, "Error al procesar el archivo multimedia", Toast.LENGTH_SHORT).show()
                }
            }

            RetrofitClient.instance.editarPublicacion(
                "Bearer $token",
                publicacion.idPublicacion,
                methodBody,
                tituloBody,
                contenidoBody,
                multimediaPart
            ).enqueue(object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@MainActivity, "Actualizado correctamente", Toast.LENGTH_SHORT).show()
                        vvPreview.stopPlayback()
                        dialog.dismiss()
                        cargarPublicaciones()
                    } else {
                        Toast.makeText(this@MainActivity, "Error al actualizar", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                    Toast.makeText(this@MainActivity, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        dialog.show()
    }

    private fun mostrarDialogoEliminar(idPublicacion: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Eliminar publicación")
        builder.setMessage("¿Estás seguro de que quieres borrar esta publicación? Esta acción no se puede deshacer.")

        builder.setPositiveButton("Eliminar") { dialog, _ ->
            eliminarPublicacionServer(idPublicacion)
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun eliminarPublicacionServer(idPublicacion: Int) {
        val token = obtenerToken()
        RetrofitClient.instance.eliminarPublicacion("Bearer $token", idPublicacion).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@MainActivity, "Publicación eliminada", Toast.LENGTH_SHORT).show()
                    cargarPublicaciones()
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error de red", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun cerrarSesion() {
        val prefs = getSharedPreferences("SesionUsuario", MODE_PRIVATE)
        prefs.edit {
            clear()
        }

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}