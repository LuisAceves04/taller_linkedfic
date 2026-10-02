package com.example.linkedininterno

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.linkedininterno.network.RetrofitClient
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EditProfileActivity : AppCompatActivity() {
    private lateinit var etEditName: TextInputEditText
    private lateinit var etEditPhone: TextInputEditText
    private lateinit var etEditSemester: TextInputEditText
    private lateinit var etEditAbout: TextInputEditText
    private lateinit var btnSaveChanges: Button
    private lateinit var spinnerMajor: Spinner

    // Listas para manejar los nombres y IDs de las carreras del Spinner
    private val listaNombresCarreras = mutableListOf<String>()
    private val listaIdsCarreras = mutableListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // 1. Inicializar vistas
        etEditName = findViewById(R.id.etEditName)
        etEditPhone = findViewById(R.id.etEditPhone)
        etEditSemester = findViewById(R.id.etEditSemester)
        etEditAbout = findViewById(R.id.etEditAbout)
        btnSaveChanges = findViewById(R.id.btnSaveChanges)
        spinnerMajor = findViewById(R.id.spinnerEditMajor)

        // 2. Cargar las carreras de la BD en el Spinner al abrir la pantalla
        cargarCarrerasEnSpinner()

        // 3. Botón cancelar
        findViewById<Button>(R.id.btnCancelEdit).setOnClickListener {
            finish()
        }

        // 4. Botón Guardar Cambios
        btnSaveChanges.setOnClickListener {
            enviarDatosActualizados()
        }
    }

    private fun cargarCarrerasEnSpinner() {
        RetrofitClient.instance.obtenerCarreras().enqueue(object : Callback<RespuestaCarreras> {
            override fun onResponse(call: Call<RespuestaCarreras>, response: Response<RespuestaCarreras>) {
                if (response.isSuccessful && response.body() != null) {
                    val respuesta = response.body()!!

                    listaNombresCarreras.clear()
                    listaIdsCarreras.clear()

                    for (carrera in respuesta.carreras) {
                        listaNombresCarreras.add(carrera.carrera)
                        listaIdsCarreras.add(carrera.idCarrera)
                    }

                    val adapter = ArrayAdapter(
                        this@EditProfileActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        listaNombresCarreras
                    )
                    spinnerMajor.adapter = adapter
                }
            }

            override fun onFailure(call: Call<RespuestaCarreras>, t: Throwable) {
                Toast.makeText(this@EditProfileActivity, "Error al cargar carreras: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun enviarDatosActualizados() {
        val nombreStr = etEditName.text.toString().trim()
        val telefonoStr = etEditPhone.text.toString().trim()
        val semestreStr = etEditSemester.text.toString().trim()
        val sobreMiStr = etEditAbout.text.toString().trim()

        val posicionSeleccionada = spinnerMajor.selectedItemPosition
        val idCarreraElegida = if (posicionSeleccionada >= 0 && listaIdsCarreras.isNotEmpty()) {
            listaIdsCarreras[posicionSeleccionada]
        } else {
            null
        }
        val nombreBody = nombreStr.toRequestBody("text/plain".toMediaTypeOrNull())
        val telefonoBody = telefonoStr.toRequestBody("text/plain".toMediaTypeOrNull())
        val semestreBody = semestreStr.toRequestBody("text/plain".toMediaTypeOrNull())
        val sobreMiBody = sobreMiStr.toRequestBody("text/plain".toMediaTypeOrNull())
        val idCarreraBody = idCarreraElegida?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())

        // Obtenemos el token usando la llave correcta "TOKEN" que viene del Login
        val sharedPreferences = getSharedPreferences("SesionUsuario", MODE_PRIVATE)
        val token = sharedPreferences.getString("TOKEN", "") ?: ""

        if (token.isEmpty()) {
            Toast.makeText(this, "No hay sesión activa", Toast.LENGTH_SHORT).show()
            return
        }

        RetrofitClient.instance.updatePerfil(
            token = "Bearer $token",
            nombre = nombreBody,
            telefono = telefonoBody,
            semestre = semestreBody,
            sobreMi = sobreMiBody,
            idCarrera = idCarreraBody
        ).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@EditProfileActivity, "¡Perfil actualizado con éxito!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@EditProfileActivity, "Error al actualizar en el servidor", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Toast.makeText(this@EditProfileActivity, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}