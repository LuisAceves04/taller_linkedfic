package com.example.linkedininterno

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.linkedininterno.network.RetrofitClient
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
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

class EditProfileActivity : AppCompatActivity() {
    private lateinit var etEditName: TextInputEditText
    private lateinit var etEditApellidoP: TextInputEditText
    private lateinit var etEditApellidoM: TextInputEditText
    private lateinit var etEditPhone: TextInputEditText
    private lateinit var etEditAbout: TextInputEditText
    private lateinit var btnSaveChanges: Button

    // Componente para la foto de perfil (Avatar)
    private lateinit var ivAvatarPreview: ImageView
    private lateinit var btnCambiarAvatar: Button
    private var avatarUri: Uri? = null

    // Los dos spinners
    private lateinit var spinnerMajor: Spinner
    private lateinit var spinnerSemester: Spinner

    private lateinit var chipGroupHabilidades: ChipGroup

    private var listaCarreras = listOf<Carrera>()
    private var tokenActual = ""

    private var semestreGuardadoUsuario: Int? = null

    // Launcher para seleccionar la imagen de la galería
    private val seleccionarAvatarLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            avatarUri = uri
            ivAvatarPreview.setImageURI(uri) // Muestra la vista previa instantánea
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        val sharedPreferences = getSharedPreferences("SesionUsuario", MODE_PRIVATE)
        tokenActual = sharedPreferences.getString("TOKEN", "") ?: ""

        etEditName = findViewById(R.id.etEditName)
        etEditApellidoP = findViewById(R.id.etEditApellidoP)
        etEditApellidoM = findViewById(R.id.etEditApellidoM)
        etEditPhone = findViewById(R.id.etEditPhone)
        etEditAbout = findViewById(R.id.etEditAbout)
        btnSaveChanges = findViewById(R.id.btnSaveChanges)

        // Vincular vistas de la imagen de perfil
        ivAvatarPreview = findViewById(R.id.ivAvatarPreview) // Asegúrate de que este ID coincida con tu XML
        btnCambiarAvatar = findViewById(R.id.btnCambiarAvatar) // Asegúrate de que este ID coincida con tu XML

        spinnerMajor = findViewById(R.id.spinnerEditMajor)
        spinnerSemester = findViewById(R.id.spinnerEditSemester)

        chipGroupHabilidades = findViewById(R.id.chipGroupHabilidades)

        btnCambiarAvatar.setOnClickListener {
            seleccionarAvatarLauncher.launch("image/*")
        }

        findViewById<Button>(R.id.btnCancelEdit).setOnClickListener { finish() }
        btnSaveChanges.setOnClickListener { enviarDatosActualizados() }

        cargarCarrerasYPerfil()
    }

    private fun cargarCarrerasYPerfil() {
        RetrofitClient.instance.obtenerCarreras().enqueue(object : Callback<RespuestaCarreras> {
            override fun onResponse(call: Call<RespuestaCarreras>, response: Response<RespuestaCarreras>) {
                if (response.isSuccessful && response.body() != null) {
                    listaCarreras = response.body()!!.carreras

                    val nombresCarreras = listaCarreras.map { it.carrera }
                    val adapterMajor = ArrayAdapter(this@EditProfileActivity, android.R.layout.simple_spinner_dropdown_item, nombresCarreras)
                    spinnerMajor.adapter = adapterMajor

                    configurarSelectorSemestres()
                    cargarDatosDelUsuario()
                }
            }
            override fun onFailure(call: Call<RespuestaCarreras>, t: Throwable) {}
        })
    }

    private fun configurarSelectorSemestres() {
        spinnerMajor.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val carreraSeleccionada = listaCarreras[position]
                val maxSemestres = carreraSeleccionada.semestres ?: 10
                val listaSemestres = (1..maxSemestres).toList().map { it.toString() }

                val adapterSemestre = ArrayAdapter(this@EditProfileActivity, android.R.layout.simple_spinner_dropdown_item, listaSemestres)
                spinnerSemester.adapter = adapterSemestre

                semestreGuardadoUsuario?.let { sem ->
                    val indexSem = listaSemestres.indexOf(sem.toString())
                    if (indexSem != -1) {
                        spinnerSemester.setSelection(indexSem)
                    }
                    semestreGuardadoUsuario = null
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun cargarDatosDelUsuario() {
        if (tokenActual.isEmpty()) return

        RetrofitClient.instance.obtenerPerfil("Bearer $tokenActual").enqueue(object : Callback<RespuestaPerfil> {
            override fun onResponse(call: Call<RespuestaPerfil>, response: Response<RespuestaPerfil>) {
                if (response.isSuccessful && response.body() != null) {
                    val alumno = response.body()?.usuario?.alumno
                    val usuario = response.body()?.usuario

                    if (alumno != null) {
                        etEditName.setText(alumno.nombre ?: "")
                        etEditApellidoP.setText(alumno.apellidoPaterno ?: "")
                        etEditApellidoM.setText(alumno.apellidoMaterno ?: "")
                        etEditPhone.setText(usuario?.telefono ?: "")
                        etEditAbout.setText(usuario?.sobreMi ?: "")

                        // NOTA: Si cargas la imagen actual del avatar con Glide o Picasso, puedes ponerla aquí:
                        // val avatarUrl = usuario?.avatar
                        // if (!avatarUrl.isNullOrEmpty()) { Glide.with(this).load(avatarUrl).into(ivAvatarPreview) }

                        semestreGuardadoUsuario = alumno.semestre

                        val idCarreraAlumno = alumno.carrera?.idCarrera
                        val indexCarrera = listaCarreras.indexOfFirst { it.idCarrera == idCarreraAlumno }
                        if (indexCarrera != -1) {
                            spinnerMajor.setSelection(indexCarrera)
                        }

                        dibujarHabilidades(alumno.habilidades)
                    }
                }
            }
            override fun onFailure(call: Call<RespuestaPerfil>, t: Throwable) {}
        })
    }

    private fun dibujarHabilidades(habilidades: List<Habilidad>?) {
        chipGroupHabilidades.removeAllViews()
        habilidades?.forEach { habilidad ->
            val chip = Chip(this)
            chip.text = habilidad.nombre
            chip.isCloseIconVisible = true
            chip.setCloseIconTintResource(R.color.black)

            chip.setOnCloseIconClickListener {
                val idRelacion = habilidad.pivot?.idAlumnoHabilidad
                if (idRelacion != null) {
                    mostrarDialogoEliminarHabilidad(idRelacion, chip)
                }
            }
            chipGroupHabilidades.addView(chip)
        }
    }

    private fun mostrarDialogoEliminarHabilidad(idAlumnoHabilidad: Int, chip: Chip) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Habilidad")
            .setMessage("¿Estás seguro de que deseas quitar esta habilidad de tu perfil?")
            .setPositiveButton("Eliminar") { _, _ -> eliminarHabilidadServer(idAlumnoHabilidad, chip) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarHabilidadServer(idAlumnoHabilidad: Int, chip: Chip) {
        RetrofitClient.instance.eliminarHabilidad("Bearer $tokenActual", idAlumnoHabilidad)
            .enqueue(object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@EditProfileActivity, "Habilidad eliminada", Toast.LENGTH_SHORT).show()
                        chipGroupHabilidades.removeView(chip)
                    }
                }
                override fun onFailure(call: Call<JsonObject>, t: Throwable) {}
            })
    }

    private fun enviarDatosActualizados() {
        val nombreStr = etEditName.text.toString().trim()
        val apellidoPStr = etEditApellidoP.text.toString().trim()
        val apellidoMStr = etEditApellidoM.text.toString().trim()
        val telefonoStr = etEditPhone.text.toString().trim()
        val sobreMiStr = etEditAbout.text.toString().trim()

        val posCarrera = spinnerMajor.selectedItemPosition
        val idCarreraElegida = if (posCarrera >= 0 && listaCarreras.isNotEmpty()) listaCarreras[posCarrera].idCarrera else null
        val semestreElegido = spinnerSemester.selectedItem?.toString() ?: "1"

        // 1. Preparar el método PUT oculto para Laravel
        val methodBody = "PUT".toRequestBody("text/plain".toMediaTypeOrNull())

        // 2. Convertir los campos de texto a RequestBody
        val nombreBody = nombreStr.toRequestBody("text/plain".toMediaTypeOrNull())
        val apellidoPBody = apellidoPStr.toRequestBody("text/plain".toMediaTypeOrNull())
        val apellidoMBody = apellidoMStr.toRequestBody("text/plain".toMediaTypeOrNull())
        val telefonoBody = telefonoStr.toRequestBody("text/plain".toMediaTypeOrNull())
        val semestreBody = semestreElegido.toRequestBody("text/plain".toMediaTypeOrNull())
        val sobreMiBody = sobreMiStr.toRequestBody("text/plain".toMediaTypeOrNull())
        val idCarreraBody = idCarreraElegida?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())

        // 3. Preparar la imagen del Avatar (si el usuario seleccionó una nueva)
        var avatarPart: MultipartBody.Part? = null
        if (avatarUri != null) {
            try {
                val file = File(cacheDir, "avatar_temp.jpg")
                contentResolver.openInputStream(avatarUri!!)?.use { inputStream ->
                    FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                avatarPart = MultipartBody.Part.createFormData("avatar", file.name, requestFile)
            } catch (e: Exception) {
                Toast.makeText(this, "Error al procesar la imagen de perfil", Toast.LENGTH_SHORT).show()
            }
        }

        // 4. Enviar mediante Retrofit con soporte Multipart
        RetrofitClient.instance.updatePerfil(
            token = "Bearer $tokenActual",
            method = methodBody,
            nombre = nombreBody,
            apellidoPaterno = apellidoPBody,
            apellidoMaterno = apellidoMBody,
            telefono = telefonoBody,
            semestre = semestreBody,
            sobreMi = sobreMiBody,
            idCarrera = idCarreraBody,
            avatar = avatarPart
        ).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@EditProfileActivity, "¡Perfil actualizado con éxito!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val errorDeLaravel = response.errorBody()?.string() ?: "Error desconocido"
                    Log.e("LARAVEL_ERROR", "Respuesta del servidor: $errorDeLaravel")
                    Toast.makeText(this@EditProfileActivity, "Error: $errorDeLaravel", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Toast.makeText(this@EditProfileActivity, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}