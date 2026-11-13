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

class CrearVacanteActivity : AppCompatActivity() {

    private lateinit var etTitulo: TextInputEditText
    private lateinit var etDescripcion: TextInputEditText
    private lateinit var actvModalidad: AutoCompleteTextView
    private lateinit var etSalarioMin: TextInputEditText
    private lateinit var etSalarioMax: TextInputEditText
    private lateinit var btnGuardar: Button

    // Vistas multimedia
    private lateinit var btnSeleccionarImagen: Button
    private lateinit var btnSeleccionarVideo: Button
    private lateinit var ivPreviewImagen: ImageView
    private lateinit var vvPreviewVideo: VideoView

    private var mediaUri: Uri? = null
    private var esVideoSeleccionado: Boolean = false

    // Variables para las Modalidades
    private val listaModalidades = mutableListOf<Modalidad>()
    private var modalidadSeleccionada: Modalidad? = null

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_vacante)

        etTitulo = findViewById(R.id.etTituloVacante)
        etDescripcion = findViewById(R.id.etDescripcionVacante)
        actvModalidad = findViewById(R.id.actvModalidadVacante)
        etSalarioMin = findViewById(R.id.etSalarioMinVacante)
        etSalarioMax = findViewById(R.id.etSalarioMaxVacante)
        btnGuardar = findViewById(R.id.btnGuardarVacante)

        btnSeleccionarImagen = findViewById(R.id.btnSeleccionarImagenVacante)
        btnSeleccionarVideo = findViewById(R.id.btnSeleccionarVideoVacante)
        ivPreviewImagen = findViewById(R.id.ivPreviewImagenVacante)
        vvPreviewVideo = findViewById(R.id.vvPreviewVideoVacante)

        btnSeleccionarImagen.setOnClickListener {
            seleccionarImagenLauncher.launch("image/*")
        }

        btnSeleccionarVideo.setOnClickListener {
            seleccionarVideoLauncher.launch("video/*")
        }

        btnGuardar.setOnClickListener {
            enviarVacanteAServidor()
        }

        cargarModalidades()
    }

    private fun cargarModalidades() {
        RetrofitClient.instance.obtenerModalidades().enqueue(object : Callback<JsonElement> {
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                if (response.isSuccessful && response.body() != null) {
                    listaModalidades.clear()
                    val element = response.body()!!

                    val jsonArray = when {
                        element.isJsonArray -> element.asJsonArray
                        element.isJsonObject && element.asJsonObject.has("data") -> element.asJsonObject.getAsJsonArray("data")
                        element.isJsonObject && element.asJsonObject.has("modalidades") -> element.asJsonObject.getAsJsonArray("modalidades")
                        else -> null
                    }

                    jsonArray?.forEach {
                        val obj = it.asJsonObject
                        val id = when {
                            obj.has("idModalidad") -> obj.get("idModalidad").asInt
                            obj.has("id") -> obj.get("id").asInt
                            else -> 0
                        }
                        val nombre = when {
                            obj.has("nombre") -> obj.get("nombre").asString
                            obj.has("modalidad") -> obj.get("modalidad").asString
                            obj.has("descripcion") -> obj.get("descripcion").asString
                            else -> "Desconocida"
                        }
                        listaModalidades.add(Modalidad(id, nombre))
                    }

                    val adapter = ArrayAdapter(this@CrearVacanteActivity, android.R.layout.simple_dropdown_item_1line, listaModalidades)
                    actvModalidad.setAdapter(adapter)

                    actvModalidad.setOnItemClickListener { parent, _, position, _ ->
                        modalidadSeleccionada = parent.getItemAtPosition(position) as Modalidad
                    }
                } else {
                    Toast.makeText(this@CrearVacanteActivity, "No se pudieron cargar las modalidades", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                Toast.makeText(this@CrearVacanteActivity, "Error de red al cargar modalidades", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun enviarVacanteAServidor() {
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
            Toast.makeText(this, "Debes seleccionar una modalidad de la lista", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = getSharedPreferences("SesionUsuario", MODE_PRIVATE)
        val token = prefs.getString("TOKEN", "") ?: ""

        if (token.isEmpty()) {
            Toast.makeText(this, "Sesión no válida", Toast.LENGTH_SHORT).show()
            return
        }

        val puestoBody = titulo.toRequestBody("text/plain".toMediaTypeOrNull())
        val contenidoBody = descripcion.toRequestBody("text/plain".toMediaTypeOrNull())
        val modalidadBody = idModalidad.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val minBody = salarioMinimo?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
        val maxBody = salarioMaximo?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())

        var multimediaPart: MultipartBody.Part? = null

        if (mediaUri != null) {
            try {
                val mimeType = contentResolver.getType(mediaUri!!) ?: if (esVideoSeleccionado) "video/mp4" else "image/jpeg"
                val nombreArchivo = if (esVideoSeleccionado) "vacante_temp_video.mp4" else "vacante_temp_image.jpg"

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

        RetrofitClient.instance
            .crearVacanteConMultimedia(
                token = "Bearer $token",
                puesto = puestoBody,
                contenido = contenidoBody,
                idModalidad = modalidadBody,
                sueldoMin = minBody,
                sueldoMax = maxBody,
                multimedia = multimediaPart
            )
            .enqueue(object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@CrearVacanteActivity, "¡Vacante publicada con éxito!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        val error = response.errorBody()?.string()
                        android.util.Log.e("CREAR_VACANTE", "Error ${response.code()}: $error")
                        Toast.makeText(this@CrearVacanteActivity, "Error ${response.code()} al crear vacante", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                    android.util.Log.e("CREAR_VACANTE", "Error de red", t)
                    Toast.makeText(this@CrearVacanteActivity, "Error de red: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
    }
}