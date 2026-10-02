package com.example.linkedininterno

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.linkedininterno.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileActivity : AppCompatActivity() {

    private lateinit var tvHeaderUsername: TextView
    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileRoleBadge: TextView
    private lateinit var tvProfileMajorValue: TextView
    private lateinit var tvProfileSemesterValue: TextView
    private lateinit var tvProfileEmailValue: TextView
    private lateinit var tvProfilePhoneValue: TextView
    private lateinit var tvProfileAboutValue: TextView
    private lateinit var tvAvatarInitial: TextView
    private lateinit var btnLogoutProfile: Button
    private lateinit var btnVerCurriculum: Button

    private var urlCurriculum: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Vincular con los IDs del layout
        tvHeaderUsername = findViewById(R.id.tvHeaderUsername)
        tvProfileName = findViewById(R.id.tvProfileName)
        tvProfileRoleBadge = findViewById(R.id.tvProfileRoleBadge)
        tvProfileMajorValue = findViewById(R.id.tvProfileMajorValue)
        tvProfileSemesterValue = findViewById(R.id.tvProfileSemesterValue)
        tvProfileEmailValue = findViewById(R.id.tvProfileEmailValue)
        tvProfilePhoneValue = findViewById(R.id.tvProfilePhoneValue)
        tvProfileAboutValue = findViewById(R.id.tvProfileAboutValue)
        tvAvatarInitial = findViewById(R.id.tvAvatarInitial)
        btnLogoutProfile = findViewById(R.id.btnLogoutProfile)
        btnVerCurriculum = findViewById(R.id.btnDownloadCv)

        // Configura el evento de clic para abrir el enlace del PDF
        btnVerCurriculum.setOnClickListener {
            try {
                // Imprimir en el Logcat qué URL está intentando abrir
                Log.d("CURRICULUM_DEBUG", "URL actual del currículum: $urlCurriculum")

                if (!urlCurriculum.isNullOrEmpty()) {
                    val uri = Uri.parse(urlCurriculum)
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Este alumno no tiene un currículum registrado", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "No se pudo abrir el archivo: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("CURRICULUM_DEBUG", "Excepción al abrir intent: ${e.message}")
            }
        }

        // Botón de Cerrar Sesión
        btnLogoutProfile.setOnClickListener {
            val sharedPreferences = getSharedPreferences("SesionUsuario", MODE_PRIVATE)
            sharedPreferences.edit().clear().apply()

            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Cargar los datos desde el servidor
        cargarDatosPerfil()
    }

    private fun cargarDatosPerfil() {
        val sharedPreferences = getSharedPreferences("SesionUsuario", MODE_PRIVATE)
        val token = sharedPreferences.getString("TOKEN", null)

        if (token == null) {
            Toast.makeText(this, "Sesión expirada o no iniciada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Petición GET con el token hacia Laravel
        RetrofitClient.instance.obtenerPerfil("Bearer $token").enqueue(object : Callback<RespuestaPerfil> {
            override fun onResponse(call: Call<RespuestaPerfil>, response: Response<RespuestaPerfil>) {
                if (response.isSuccessful && response.body() != null) {
                    val respuesta = response.body()!!
                    val usuario = respuesta.usuario
                    val alumno = usuario.alumno

                    if (alumno != null) {
                        val nombreCompleto = "${alumno.nombre} ${alumno.apellidoPaterno} ${alumno.apellidoMaterno}"

                        // Asignar datos académicos y personales a la UI
                        tvProfileName.text = nombreCompleto
                        tvHeaderUsername.text = alumno.nombre
                        tvProfileMajorValue.text = alumno.carrera?.carrera ?: "Sin carrera"
                        tvProfileSemesterValue.text = alumno.semestre.toString()

                        urlCurriculum = "http://10.0.2.2:8000/storage/" + alumno.curriculum

                        // Poner la primera letra del nombre en el avatar circular
                        if (alumno.nombre.isNotEmpty()) {
                            tvAvatarInitial.text = alumno.nombre.first().toString().uppercase()
                        }
                    } else {
                        tvProfileName.text = "Usuario sin perfil de alumno"
                        tvHeaderUsername.text = "Usuario"
                        urlCurriculum = null
                    }

                    tvProfileRoleBadge.text = "Alumno"
                    tvProfileEmailValue.text = usuario.email
                    tvProfilePhoneValue.text = usuario.telefono ?: "No registrado"
                    tvProfileAboutValue.text = usuario.sobreMi ?: "Sin descripción"

                } else {
                    Toast.makeText(this@ProfileActivity, "No se pudo cargar la información del perfil", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<RespuestaPerfil>, t: Throwable) {
                Toast.makeText(this@ProfileActivity, "Error de red: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
}