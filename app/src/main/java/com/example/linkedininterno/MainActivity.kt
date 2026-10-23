package com.example.linkedininterno

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.linkedininterno.R
import com.example.linkedininterno.network.EditPublicacionRequest
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

class MainActivity : AppCompatActivity() {

    private lateinit var rvPublicaciones: RecyclerView
    private lateinit var adapter: PublicacionesAdapter
    private val listaPublicaciones = mutableListOf<Publicacion>()

    private lateinit var etTituloPost: TextInputEditText
    private lateinit var etContenidoPost: TextInputEditText
    private lateinit var btnSeleccionarImagen: Button
    private lateinit var ivPreviewImagen: ImageView
    private lateinit var btnPublicar: Button

    private var imagenUri: Uri? = null

    // Variables globales para la edición de la imagen dentro del diálogo
    private var nuevaImagenUri: Uri? = null
    private var ivEditarPreviewRef: ImageView? = null

    private val seleccionarImagenLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            imagenUri = uri
            ivPreviewImagen.setImageURI(uri)
            ivPreviewImagen.visibility = View.VISIBLE
        }
    }

    // Launcher global para la edición (evita cierres forzosos)
    private val selectorEdicionLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            nuevaImagenUri = uri
            ivEditarPreviewRef?.setImageURI(uri)
            ivEditarPreviewRef?.visibility = View.VISIBLE
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
        ivPreviewImagen = findViewById(R.id.ivPreviewImagen)
        btnPublicar = findViewById(R.id.btnPublicar)
        rvPublicaciones = findViewById(R.id.rvPublicaciones)

        rvPublicaciones.layoutManager = LinearLayoutManager(this)
        adapter = PublicacionesAdapter(
            listaPublicaciones,
            onEditarClick = { publicacion -> mostrarDialogoEditar(publicacion) },
            onEliminarClick = { publicacion -> mostrarDialogoEliminar(publicacion.idPublicacion) }
        )
        rvPublicaciones.adapter = adapter

        btnSeleccionarImagen.setOnClickListener {
            seleccionarImagenLauncher.launch("image/*")
        }

        btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        btnLogout.setOnClickListener {
            cerrarSesion()
        }

        btnPublicar.setOnClickListener {
            crearNuevaPublicacion()
        }

        cargarPublicaciones()
    }

    private fun obtenerToken(): String {
        val prefs = getSharedPreferences("SesionUsuario", MODE_PRIVATE)
        return prefs.getString("TOKEN", "") ?: ""
    }

    private fun cargarPublicaciones() {
        val token = obtenerToken()
        if (token.isEmpty()) return

        RetrofitClient.instance.obtenerPublicaciones("Bearer $token").enqueue(object : Callback<RespuestaPublicaciones> {
            override fun onResponse(call: Call<RespuestaPublicaciones>, response: Response<RespuestaPublicaciones>) {
                if (response.isSuccessful && response.body() != null) {
                    val respuesta = response.body()!!
                    val publicacionesObtenidas = respuesta.publicacionesPagina?.data ?: emptyList()

                    listaPublicaciones.clear()
                    listaPublicaciones.addAll(publicacionesObtenidas)
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

        var multimediaPart: MultipartBody.Part? = null

        if (imagenUri != null) {
            try {
                val file = File(cacheDir, "post_temp_image.jpg")
                contentResolver.openInputStream(imagenUri!!)?.use { inputStream ->
                    FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                multimediaPart = MultipartBody.Part.createFormData("multimedia", file.name, requestFile)
            } catch (_: Exception) {
                Toast.makeText(this, "Error al procesar la imagen", Toast.LENGTH_SHORT).show()
            }
        }

        RetrofitClient.instance.crearPublicacionConImagen("Bearer $token", idUsuarioBody, tituloBody, contenidoBody, multimediaPart)
            .enqueue(object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@MainActivity, "¡Publicado con éxito!", Toast.LENGTH_SHORT).show()
                        etTituloPost.text?.clear()
                        etContenidoPost.text?.clear()
                        imagenUri = null
                        ivPreviewImagen.visibility = View.GONE
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
        val btnCancelar = dialogView.findViewById<Button>(R.id.btnCancelarEdicion)
        val btnGuardar = dialogView.findViewById<Button>(R.id.btnGuardarEdicion)

        etTitulo.setText(publicacion.titulo)
        etContenido.setText(publicacion.contenido)

        nuevaImagenUri = null
        ivEditarPreviewRef = ivPreview
        ivPreview.visibility = View.GONE

        btnSeleccionarImagen.setOnClickListener {
            selectorEdicionLauncher.launch("image/*")
        }

        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancelar.setOnClickListener {
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

            // Preparar la imagen solo si el usuario seleccionó una nueva
            var multimediaPart: MultipartBody.Part? = null
            if (nuevaImagenUri != null) {
                try {
                    val file = File(cacheDir, "edit_temp_image.jpg")
                    contentResolver.openInputStream(nuevaImagenUri!!)?.use { inputStream ->
                        FileOutputStream(file).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    multimediaPart = MultipartBody.Part.createFormData("multimedia", file.name, requestFile)
                } catch (_: Exception) {
                    Toast.makeText(this, "Error al procesar la imagen", Toast.LENGTH_SHORT).show()
                }
            }

            // Llamada a Retrofit
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
            remove("TOKEN")
        }

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}