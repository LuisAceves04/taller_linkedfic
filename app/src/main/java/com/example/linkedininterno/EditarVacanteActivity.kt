package com.example.linkedininterno

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.linkedininterno.network.RetrofitClient
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.JsonElement
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

class EditarVacanteActivity : AppCompatActivity() {

    private lateinit var etTitulo: TextInputEditText
    private lateinit var etDescripcion: TextInputEditText
    private lateinit var actvModalidad: AutoCompleteTextView
    private lateinit var etSalarioMin: TextInputEditText
    private lateinit var etSalarioMax: TextInputEditText
    private lateinit var btnGuardar: Button
    private lateinit var btnCancelar: Button

    private lateinit var btnSeleccionarImagen: Button
    private lateinit var btnSeleccionarVideo: Button
    private lateinit var ivPreviewImagen: ImageView
    private lateinit var vvPreviewVideo: VideoView

    private var mediaUri: Uri? = null
    private var esVideoSeleccionado: Boolean = false
    private val listaModalidades = mutableListOf<Modalidad>()
    private var modalidadSeleccionada: Modalidad? = null
    private var idVacante: Int = -1

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_vacante)

        etTitulo = findViewById(R.id.etEditarTituloVacante)
        etDescripcion = findViewById(R.id.etEditarDescripcionVacante)
        actvModalidad = findViewById(R.id.actvEditarModalidadVacante)
        etSalarioMin = findViewById(R.id.etEditarSalarioMinVacante)
        etSalarioMax = findViewById(R.id.etEditarSalarioMaxVacante)
        btnGuardar = findViewById(R.id.btnGuardarEditarVacante)
        btnCancelar = findViewById(R.id.btnCancelarEditarVacante)

        btnSeleccionarImagen = findViewById(R.id.btnEditarSeleccionarImagenVacante)
        btnSeleccionarVideo = findViewById(R.id.btnEditarSeleccionarVideoVacante)
        ivPreviewImagen = findViewById(R.id.ivEditarPreviewImagenVacante)
        vvPreviewVideo = findViewById(R.id.vvEditarPreviewVideoVacante)

        cargarDatosPrevios()
        cargarModalidades()

        btnSeleccionarImagen.setOnClickListener { seleccionarImagenLauncher.launch("image/*") }
        btnSeleccionarVideo.setOnClickListener { seleccionarVideoLauncher.launch("video/*") }
        btnCancelar.setOnClickListener { finish() }
        btnGuardar.setOnClickListener { guardarEdicion() }
    }

    private fun cargarDatosPrevios() {
        idVacante = intent.getIntExtra("ID_VACANTE", -1)
        if (idVacante == -1) {
            Toast.makeText(this, "Error: Vacante no encontrada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        etTitulo.setText(intent.getStringExtra("TITULO"))
        etDescripcion.setText(intent.getStringExtra("DESCRIPCION"))
        etSalarioMin.setText(intent.getStringExtra("SUELDO_MIN"))
        etSalarioMax.setText(intent.getStringExtra("SUELDO_MAX"))

        val pathMultimedia = intent.getStringExtra("MULTIMEDIA")
        if (!pathMultimedia.isNullOrEmpty()) {
            val urlCompleta = if (pathMultimedia.startsWith("http")) pathMultimedia else "http://10.0.2.2:8000/storage/" + pathMultimedia.trimStart('/')
            val esVideo = urlCompleta.endsWith(".mp4", true) || urlCompleta.endsWith(".mov", true)

            if (esVideo) {
                ivPreviewImagen.visibility = View.GONE
                vvPreviewVideo.visibility = View.VISIBLE
                vvPreviewVideo.setVideoPath(urlCompleta)
                vvPreviewVideo.setOnPreparedListener { mp ->
                    mp.isLooping = true
                    vvPreviewVideo.start()
                }
            } else {
                vvPreviewVideo.visibility = View.GONE
                ivPreviewImagen.visibility = View.VISIBLE
                Glide.with(this).load(urlCompleta).into(ivPreviewImagen)
            }
        }
    }

    private fun cargarModalidades() {
        RetrofitClient.instance.obtenerModalidades().enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if (response.isSuccessful && response.body() != null) {
                    listaModalidades.clear()
                    val element = response.body()!!

                    // Manejo más robusto de la respuesta JSON
                    val jsonArray = when {
                        element.isJsonArray -> element.asJsonArray
                        element.isJsonObject && element.asJsonObject.has("data") -> element.asJsonObject.getAsJsonArray("data")
                        element.isJsonObject && element.asJsonObject.has("modalidades") -> element.asJsonObject.getAsJsonArray("modalidades")
                        else -> null
                    }

                    if (jsonArray != null) {
                        jsonArray.forEach {
                            if (it.isJsonObject) {
                                val obj = it.asJsonObject
                                // Extrae el ID basado en nombres de campos comunes
                                val id = when {
                                    obj.has("idModalidad") -> obj.get("idModalidad").asInt
                                    obj.has("id") -> obj.get("id").asInt
                                    else -> 0
                                }
                                // Extrae el nombre basado en nombres de campos comunes
                                val nombre = when {
                                    obj.has("nombre") -> obj.get("nombre").asString
                                    obj.has("modalidad") -> obj.get("modalidad").asString
                                    obj.has("descripcion") -> obj.get("descripcion").asString
                                    else -> "Desconocida"
                                }
                                if (id != 0) {
                                    listaModalidades.add(Modalidad(id, nombre))
                                }
                            }
                        }

                        // Actualiza el adaptador en el hilo principal
                        runOnUiThread {
                            val adapter = ArrayAdapter(this@EditarVacanteActivity, android.R.layout.simple_dropdown_item_1line, listaModalidades)
                            actvModalidad.setAdapter(adapter)

                            actvModalidad.setOnItemClickListener { parent, _, position, _ ->
                                modalidadSeleccionada = parent.getItemAtPosition(position) as Modalidad
                            }
                        }
                    } else {
                        Toast.makeText(this@EditarVacanteActivity, "Estructura JSON no reconocida", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@EditarVacanteActivity, "No se pudieron cargar las modalidades", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                Toast.makeText(this@EditarVacanteActivity, "Error de red al cargar modalidades", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun guardarEdicion() {
        val titulo = etTitulo.text.toString().trim()
        val descripcion = etDescripcion.text.toString().trim()
        val idModalidad = modalidadSeleccionada?.id

        val salarioMinTexto = etSalarioMin.text.toString().replace(",", "").replace(Regex("[^0-9.]"), "").trim()
        val salarioMaxTexto = etSalarioMax.text.toString().replace(",", "").replace(Regex("[^0-9.]"), "").trim()
        val salarioMinimo = if (salarioMinTexto.isEmpty()) null else salarioMinTexto.toDoubleOrNull()
        val salarioMaximo = if (salarioMaxTexto.isEmpty()) null else salarioMaxTexto.toDoubleOrNull()

        if (titulo.isEmpty() || descripcion.isEmpty()) {
            Toast.makeText(this, "El puesto y el contenido son obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        if (idModalidad == null) {
            Toast.makeText(this, "Por favor, vuelve a seleccionar la modalidad", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = getSharedPreferences("SesionUsuario", MODE_PRIVATE)
        val token = prefs.getString("TOKEN", "") ?: ""

        // Configuración para simular PUT a través de POST
        val methodBody = "PUT".toRequestBody("text/plain".toMediaTypeOrNull())
        val puestoBody = titulo.toRequestBody("text/plain".toMediaTypeOrNull())
        val contenidoBody = descripcion.toRequestBody("text/plain".toMediaTypeOrNull())
        val modalidadBody = idModalidad.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val minBody = salarioMinimo?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
        val maxBody = salarioMaximo?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())

        var multimediaPart: MultipartBody.Part? = null
        if (mediaUri != null) {
            try {
                val mimeType = contentResolver.getType(mediaUri!!) ?: if (esVideoSeleccionado) "video/mp4" else "image/jpeg"
                val nombreArchivo = if (esVideoSeleccionado) "edit_vacante_video.mp4" else "edit_vacante_image.jpg"
                val file = File(cacheDir, nombreArchivo)
                contentResolver.openInputStream(mediaUri!!)?.use { inputStream ->
                    FileOutputStream(file).use { outputStream -> inputStream.copyTo(outputStream) }
                }
                val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                multimediaPart = MultipartBody.Part.createFormData("multimedia", file.name, requestFile)
            } catch (_: Exception) {}
        }

        RetrofitClient.instance.editarVacante("Bearer $token", idVacante, methodBody, puestoBody, contenidoBody, modalidadBody, minBody, maxBody, multimediaPart)
            .enqueue(object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@EditarVacanteActivity, "Vacante actualizada", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@EditarVacanteActivity, "Error al actualizar", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                    Toast.makeText(this@EditarVacanteActivity, "Error de red", Toast.LENGTH_SHORT).show()
                }
            })
    }
}