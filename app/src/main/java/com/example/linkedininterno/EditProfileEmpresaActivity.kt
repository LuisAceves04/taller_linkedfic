package com.example.linkedininterno

import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
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

class EditProfileEmpresaActivity : AppCompatActivity() {

    private lateinit var ivAvatar: ImageView
    private lateinit var btnCambiarFoto: Button
    private lateinit var etNombreEmpresa: TextInputEditText
    private lateinit var etTelefono: TextInputEditText
    private lateinit var etPaginaWeb: TextInputEditText
    private lateinit var etSobreMi: TextInputEditText
    private lateinit var spinnerIndustrias: Spinner
    private lateinit var btnGuardar: Button
    private lateinit var btnCancelar: Button

    private var imagenUri: Uri? = null
    private val listaIndustrias = mutableListOf<Industria>()

    private val seleccionarImagenLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            imagenUri = uri
            ivAvatar.setImageURI(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile_empresa_activity)

        ivAvatar = findViewById(R.id.ivEditAvatarEmpresa)
        btnCambiarFoto = findViewById(R.id.btnCambiarFotoEmpresa)
        etNombreEmpresa = findViewById(R.id.etEditNombreEmpresa)
        etTelefono = findViewById(R.id.etEditTelefonoEmpresa)
        etPaginaWeb = findViewById(R.id.etEditPaginaWeb)
        etSobreMi = findViewById(R.id.etEditSobreMiEmpresa)
        spinnerIndustrias = findViewById(R.id.spinnerIndustrias)
        btnGuardar = findViewById(R.id.btnGuardarPerfilEmpresa)
        btnCancelar = findViewById(R.id.btnCancelarEditEmpresa)

        btnCambiarFoto.setOnClickListener {
            seleccionarImagenLauncher.launch("image/*")
        }

        btnCancelar.setOnClickListener {
            finish()
        }

        btnGuardar.setOnClickListener {
            guardarCambios()
        }

        cargarIndustriasYPerfil()
    }

    private fun obtenerToken(): String {
        val prefs = getSharedPreferences("SesionUsuario", MODE_PRIVATE)
        return prefs.getString("TOKEN", "") ?: ""
    }

    private fun cargarIndustriasYPerfil() {
        RetrofitClient.instance.obtenerIndustrias().enqueue(object : Callback<RespuestaIndustrias> {
            override fun onResponse(call: Call<RespuestaIndustrias>, response: Response<RespuestaIndustrias>) {
                if (response.isSuccessful && response.body() != null) {
                    val industrias = response.body()?.industrias ?: emptyList()
                    listaIndustrias.clear()
                    listaIndustrias.addAll(industrias)

                    val nombres = industrias.map { it.industria ?: "" }
                    val adapter = ArrayAdapter(this@EditProfileEmpresaActivity, android.R.layout.simple_spinner_dropdown_item, nombres)
                    spinnerIndustrias.adapter = adapter

                    cargarDatosActuales()
                }
            }

            override fun onFailure(call: Call<RespuestaIndustrias>, t: Throwable) {
                Toast.makeText(this@EditProfileEmpresaActivity, "Error al cargar industrias", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun cargarDatosActuales() {
        val token = obtenerToken()
        RetrofitClient.instance.obtenerPerfil("Bearer $token").enqueue(object : Callback<RespuestaPerfil> {
            override fun onResponse(call: Call<RespuestaPerfil>, response: Response<RespuestaPerfil>) {
                if (response.isSuccessful && response.body() != null) {
                    val usuario = response.body()!!.usuario
                    val empresa = usuario.empresa

                    etNombreEmpresa.setText(empresa?.nombreEmpresa ?: "")
                    etTelefono.setText(usuario.telefono ?: "")
                    etPaginaWeb.setText(empresa?.paginaWeb ?: "")
                    etSobreMi.setText(usuario.sobreMi ?: "")

                    empresa?.industria?.idIndustria?.let { idInd ->
                        val pos = listaIndustrias.indexOfFirst { it.idIndustria == idInd }
                        if (pos >= 0) spinnerIndustrias.setSelection(pos)
                    }

                    usuario.avatar?.let { avatarUrl ->
                        val urlCompleta = if (avatarUrl.startsWith("http")) avatarUrl else "http://10.0.2.2:8000/storage/$avatarUrl"
                        Glide.with(this@EditProfileEmpresaActivity).load(urlCompleta).into(ivAvatar)
                    }
                }
            }

            override fun onFailure(call: Call<RespuestaPerfil>, t: Throwable) {
                Toast.makeText(this@EditProfileEmpresaActivity, "Error al obtener perfil actual", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun guardarCambios() {
        val token = obtenerToken()
        val nombre = etNombreEmpresa.text.toString().trim()
        val telefono = etTelefono.text.toString().trim()
        val paginaWeb = etPaginaWeb.text.toString().trim()
        val sobreMi = etSobreMi.text.toString().trim()

        val posSeleccionada = spinnerIndustrias.selectedItemPosition
        val idIndustria = if (posSeleccionada in listaIndustrias.indices) listaIndustrias[posSeleccionada].idIndustria else null

        if (nombre.isEmpty()) {
            Toast.makeText(this, "El nombre de la empresa es obligatorio", Toast.LENGTH_SHORT).show()
            return
        }

        val methodBody = "PUT".toRequestBody("text/plain".toMediaTypeOrNull())
        val nombreBody = nombre.toRequestBody("text/plain".toMediaTypeOrNull())
        val telefonoBody = telefono.toRequestBody("text/plain".toMediaTypeOrNull())
        val webBody = paginaWeb.toRequestBody("text/plain".toMediaTypeOrNull())
        val sobreMiBody = sobreMi.toRequestBody("text/plain".toMediaTypeOrNull())
        val idIndustriaBody = idIndustria?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())

        var avatarPart: MultipartBody.Part? = null
        if (imagenUri != null) {
            try {
                val file = File(cacheDir, "empresa_avatar_temp.jpg")
                contentResolver.openInputStream(imagenUri!!)?.use { input ->
                    FileOutputStream(file).use { output -> input.copyTo(output) }
                }
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                avatarPart = MultipartBody.Part.createFormData("avatar", file.name, requestFile)
            } catch (e: Exception) {
                Toast.makeText(this, "Error procesando la imagen", Toast.LENGTH_SHORT).show()
            }
        }

        RetrofitClient.instance.updatePerfilEmpresa(
            "Bearer $token",
            methodBody,
            nombreBody,
            telefonoBody,
            webBody,
            sobreMiBody,
            idIndustriaBody,
            avatarPart
        ).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@EditProfileEmpresaActivity, "Perfil actualizado con éxito", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@EditProfileEmpresaActivity, "Error al actualizar perfil", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Toast.makeText(this@EditProfileEmpresaActivity, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}