package com.example.linkedininterno

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
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

class RegisterEmpresaActivity : AppCompatActivity() {

    private lateinit var tvAvatarEmpresaInitial: TextView
    private lateinit var ivAvatarEmpresaPreview: ImageView
    private lateinit var btnCambiarAvatarEmpresa: Button
    private lateinit var etNombreEmpresa: TextInputEditText
    private lateinit var etCorreoEmpresa: TextInputEditText
    private lateinit var etPasswordEmpresa: TextInputEditText
    private lateinit var etTelefonoEmpresa: TextInputEditText
    private lateinit var etPaginaWeb: TextInputEditText
    private lateinit var etSobreMiEmpresa: TextInputEditText
    private lateinit var spinnerIndustria: Spinner
    private lateinit var btnRegistrarEmpresa: Button

    private var listaIndustrias = listOf<Industria>()
    private var imageUri: Uri? = null

    // Selector de imagen de la galería
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            imageUri = result.data?.data
            if (imageUri != null) {
                tvAvatarEmpresaInitial.visibility = View.GONE
                ivAvatarEmpresaPreview.visibility = View.VISIBLE
                Glide.with(this)
                    .load(imageUri)
                    .centerCrop()
                    .into(ivAvatarEmpresaPreview)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_company)

        tvAvatarEmpresaInitial = findViewById(R.id.tvAvatarEmpresaInitial)
        ivAvatarEmpresaPreview = findViewById(R.id.ivAvatarEmpresaPreview)
        btnCambiarAvatarEmpresa = findViewById(R.id.btnCambiarAvatarEmpresa)
        etNombreEmpresa = findViewById(R.id.etNombreEmpresa)
        etCorreoEmpresa = findViewById(R.id.etCorreoEmpresa)
        etPasswordEmpresa = findViewById(R.id.etPasswordEmpresa)
        etTelefonoEmpresa = findViewById(R.id.etTelefonoEmpresa)
        etPaginaWeb = findViewById(R.id.etPaginaWeb)
        etSobreMiEmpresa = findViewById(R.id.etSobreMiEmpresa)
        spinnerIndustria = findViewById(R.id.spinnerIndustria)
        btnRegistrarEmpresa = findViewById(R.id.btnRegistrarEmpresa)

        // Carga dinámica de industrias desde la API de Laravel
        cargarIndustrias()

        // Botón para seleccionar el logotipo
        btnCambiarAvatarEmpresa.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            pickImageLauncher.launch(intent)
        }

        // Botón para enviar el registro
        btnRegistrarEmpresa.setOnClickListener {
            enviarRegistroEmpresa()
        }
    }

    private fun cargarIndustrias() {
        RetrofitClient.instance.obtenerIndustrias().enqueue(object : Callback<RespuestaIndustrias> {
            override fun onResponse(call: Call<RespuestaIndustrias>, response: Response<RespuestaIndustrias>) {
                Log.d("INDUSTRIAS_DEBUG", "Código HTTP: ${response.code()}")

                if (response.isSuccessful && response.body() != null) {
                    listaIndustrias = response.body()!!.industrias

                    if (listaIndustrias.isEmpty()) {
                        Toast.makeText(this@RegisterEmpresaActivity, "La lista de industrias vino vacía", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val nombres = listaIndustrias.map { it.industria }
                    val adapter = ArrayAdapter(
                        this@RegisterEmpresaActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        nombres
                    )
                    spinnerIndustria.adapter = adapter
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("INDUSTRIAS_DEBUG", "Error de respuesta: $errorBody")
                    Toast.makeText(this@RegisterEmpresaActivity, "Error ${response.code()} al obtener industrias", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<RespuestaIndustrias>, t: Throwable) {
                Log.e("INDUSTRIAS_DEBUG", "Fallo de red: ${t.message}", t)
                Toast.makeText(this@RegisterEmpresaActivity, "Fallo de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun enviarRegistroEmpresa() {
        val nombre = etNombreEmpresa.text.toString().trim()
        val email = etCorreoEmpresa.text.toString().trim()
        val password = etPasswordEmpresa.text.toString().trim()
        val telefono = etTelefonoEmpresa.text.toString().trim()
        val paginaWeb = etPaginaWeb.text.toString().trim()
        val sobreMi = etSobreMiEmpresa.text.toString().trim()

        if (nombre.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Completa los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        val posIndustria = spinnerIndustria.selectedItemPosition
        val idIndustriaElegida = if (posIndustria >= 0 && listaIndustrias.isNotEmpty()) {
            listaIndustrias[posIndustria].idIndustria
        } else {
            1
        }

        val rolBody = "2".toRequestBody("text/plain".toMediaTypeOrNull())
        val nombreBody = nombre.toRequestBody("text/plain".toMediaTypeOrNull())
        val emailBody = email.toRequestBody("text/plain".toMediaTypeOrNull())
        val passwordBody = password.toRequestBody("text/plain".toMediaTypeOrNull())
        val telefonoBody = if (telefono.isNotEmpty()) telefono.toRequestBody("text/plain".toMediaTypeOrNull()) else null
        val webBody = if (paginaWeb.isNotEmpty()) paginaWeb.toRequestBody("text/plain".toMediaTypeOrNull()) else null
        val sobreMiBody = if (sobreMi.isNotEmpty()) sobreMi.toRequestBody("text/plain".toMediaTypeOrNull()) else null
        val idIndustriaBody = idIndustriaElegida.toString().toRequestBody("text/plain".toMediaTypeOrNull())

        // Procesar imagen del avatar/logotipo
        var avatarPart: MultipartBody.Part? = null
        imageUri?.let { uri ->
            try {
                val file = File(cacheDir, "temp_avatar_empresa.jpg")
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                avatarPart = MultipartBody.Part.createFormData("avatar", file.name, requestFile)
            } catch (e: Exception) {
                Log.e("AVATAR_ERROR", "Error al procesar la imagen: ${e.message}")
            }
        }


        RetrofitClient.instance.registrarEmpresa(
            idRol = rolBody,
            nombreEmpresa = nombreBody,
            email = emailBody,
            password = passwordBody,
            telefono = telefonoBody,
            paginaWeb = webBody,
            sobreMi = sobreMiBody,
            idIndustria = idIndustriaBody,
            avatar = avatarPart
        ).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@RegisterEmpresaActivity, "¡Empresa registrada con éxito!", Toast.LENGTH_LONG).show()

                    val intent = Intent(this@RegisterEmpresaActivity, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@RegisterEmpresaActivity, "Error al registrar la empresa. Revisa los datos.", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Toast.makeText(this@RegisterEmpresaActivity, "Error de red: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
}