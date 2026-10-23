package com.example.linkedininterno

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.example.linkedininterno.network.RetrofitClient
import com.google.android.material.card.MaterialCardView
import com.google.gson.JsonObject
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class ProfileActivity : AppCompatActivity() {

    private lateinit var tvHeaderUsername: TextView
    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileRoleBadge: TextView
    private lateinit var tvProfileMajorValue: TextView
    private lateinit var tvProfileSemesterValue: TextView
    private lateinit var tvProfileEmailValue: TextView
    private lateinit var tvProfilePhoneValue: TextView
    private lateinit var tvProfileAboutValue: TextView
    private lateinit var tvProfileSkillsValue: TextView
    private lateinit var tvAvatarInitial: TextView
    private lateinit var cardAvatar: MaterialCardView
    private var ivAvatarPerfil: ImageView? = null
    private lateinit var btnLogoutProfile: Button
    private lateinit var btnVerCurriculum: Button
    private lateinit var btnEditProfile: Button
    private lateinit var btnAddSkill: Button

    private var tieneCurriculum: Boolean = false

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
        tvProfileSkillsValue = findViewById(R.id.tvProfileSkillsValue)
        tvAvatarInitial = findViewById(R.id.tvAvatarInitial)
        cardAvatar = findViewById(R.id.cardAvatar)
        btnLogoutProfile = findViewById(R.id.btnLogoutProfile)
        btnVerCurriculum = findViewById(R.id.btnDownloadCv)
        btnEditProfile = findViewById(R.id.btnEditProfile)
        btnAddSkill = findViewById(R.id.btnAddSkill)

        // Crear e insertar el ImageView dinámicamente usando FrameLayout.LayoutParams
        ivAvatarPerfil = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            visibility = View.GONE
        }
        cardAvatar.addView(ivAvatarPerfil)

        // Botón para editar el perfil
        btnEditProfile.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }

        // Botón para abrir el diálogo y agregar habilidades
        btnAddSkill.setOnClickListener {
            mostrarDialogoAgregarHabilidad()
        }

        // Botón para descargar y ver el PDF usando Retrofit
        btnVerCurriculum.setOnClickListener {
            if (!tieneCurriculum) {
                Toast.makeText(this, "Este alumno no tiene un currículum registrado", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sharedPreferences = getSharedPreferences("SesionUsuario", MODE_PRIVATE)
            val token = sharedPreferences.getString("TOKEN", null)

            if (token.isNullOrEmpty()) {
                Toast.makeText(this, "Sesión no válida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "Abriendo currículum...", Toast.LENGTH_SHORT).show()

            RetrofitClient.instance.descargarCurriculum("Bearer $token").enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful && response.body() != null) {
                        try {
                            val file = File(cacheDir, "curriculum.pdf")
                            val inputStream = response.body()!!.byteStream()
                            val outputStream = FileOutputStream(file)
                            inputStream.copyTo(outputStream)
                            outputStream.close()
                            inputStream.close()

                            val uri = FileProvider.getUriForFile(
                                this@ProfileActivity,
                                "${packageName}.fileprovider",
                                file
                            )

                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            startActivity(intent)

                        } catch (e: Exception) {
                            Toast.makeText(this@ProfileActivity, "No se pudo abrir el archivo: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this@ProfileActivity, "No se pudo descargar el currículum", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Toast.makeText(this@ProfileActivity, "Error de red: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
        }

        // Botón de Cerrar Sesión
        btnLogoutProfile.setOnClickListener {
            val sharedPreferences = getSharedPreferences("SesionUsuario", MODE_PRIVATE)
            sharedPreferences.edit().clear().apply()

            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
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

        RetrofitClient.instance.obtenerPerfil("Bearer $token").enqueue(object : Callback<RespuestaPerfil> {
            override fun onResponse(call: Call<RespuestaPerfil>, response: Response<RespuestaPerfil>) {
                if (response.isSuccessful && response.body() != null) {
                    val respuesta = response.body()!!
                    val usuario = respuesta.usuario
                    if (usuario == null) {
                        Toast.makeText(this@ProfileActivity, "No se encontró la información del usuario", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val alumno = usuario.alumno

                    // Manejo del Avatar con Glide
                    val avatarUrl = usuario.avatar
                    if (!avatarUrl.isNullOrEmpty()) {
                        tvAvatarInitial.visibility = View.GONE
                        ivAvatarPerfil?.visibility = View.VISIBLE

                        val urlCompleta = if (avatarUrl.startsWith("http")) avatarUrl else "http://10.0.2.2:8000/storage/$avatarUrl"

                        Glide.with(this@ProfileActivity)
                            .load(urlCompleta)
                            .placeholder(R.drawable.logofic)
                            .error(R.drawable.logofic)
                            .centerCrop()
                            .into(ivAvatarPerfil!!)
                    } else {
                        tvAvatarInitial.visibility = View.VISIBLE
                        ivAvatarPerfil?.visibility = View.GONE
                        if (!alumno?.nombre.isNullOrEmpty()) {
                            tvAvatarInitial.text = alumno?.nombre?.first().toString().uppercase()
                        } else {
                            tvAvatarInitial.text = "U"
                        }
                    }

                    if (alumno != null) {
                        val nombreCompleto = "${alumno.nombre ?: ""} ${alumno.apellidoPaterno ?: ""} ${alumno.apellidoMaterno ?: ""}".trim()

                        tvProfileName.text = nombreCompleto
                        tvHeaderUsername.text = alumno.nombre ?: "Usuario"
                        tvProfileMajorValue.text = alumno.carrera?.carrera ?: "Sin carrera"
                        tvProfileSemesterValue.text = (alumno.semestre ?: 0).toString()

                        tieneCurriculum = !alumno.curriculum.isNullOrEmpty()
                    } else {
                        tvProfileName.text = "Usuario sin perfil de alumno"
                        tvHeaderUsername.text = "Usuario"
                        tieneCurriculum = false
                    }

                    tvProfileRoleBadge.text = "Alumno"
                    tvProfileEmailValue.text = usuario.email ?: "Sin correo"
                    tvProfilePhoneValue.text = usuario.telefono ?: "No registrado"
                    tvProfileAboutValue.text = usuario.sobreMi ?: "Sin descripción"

                    val habilidades = alumno?.habilidades
                    if (!habilidades.isNullOrEmpty()) {
                        tvProfileSkillsValue.text = habilidades.joinToString(", ") { it.nombre ?: "" }
                    } else {
                        tvProfileSkillsValue.text = "Sin habilidades registradas"
                    }

                } else {
                    Toast.makeText(this@ProfileActivity, "Error ${response.code()} al cargar perfil", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<RespuestaPerfil>, t: Throwable) {
                Toast.makeText(this@ProfileActivity, "Error de red/parseo: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun mostrarDialogoAgregarHabilidad() {
        val sharedPreferences = getSharedPreferences("SesionUsuario", MODE_PRIVATE)
        val token = sharedPreferences.getString("TOKEN", null) ?: return

        RetrofitClient.instance.obtenerHabilidades().enqueue(object : Callback<RespuestaHabilidades> {
            override fun onResponse(call: Call<RespuestaHabilidades>, response: Response<RespuestaHabilidades>) {
                if (response.isSuccessful && response.body() != null) {
                    val respuesta = response.body()!!
                    val listaHabilidades = respuesta.data ?: respuesta.habilidades ?: emptyList()

                    if (listaHabilidades.isEmpty()) {
                        Toast.makeText(this@ProfileActivity, "No hay habilidades disponibles en el catálogo", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val nombresHabilidades = listaHabilidades.map { it.nombre ?: "Sin nombre" }.toTypedArray()

                    AlertDialog.Builder(this@ProfileActivity)
                        .setTitle("Selecciona una habilidad")
                        .setItems(nombresHabilidades) { _, which ->
                            val habilidadSeleccionada = listaHabilidades[which]
                            mostrarDialogoSeleccionarNivel(token, idHabilidad = habilidadSeleccionada.id ?: 0)
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()

                } else {
                    Toast.makeText(this@ProfileActivity, "Error al obtener el catálogo de habilidades", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<RespuestaHabilidades>, t: Throwable) {
                Toast.makeText(this@ProfileActivity, "Error de red: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun mostrarDialogoSeleccionarNivel(token: String, idHabilidad: Int) {
        val niveles = arrayOf("Bajo", "Medio", "Alto")
        val idsNiveles = arrayOf(1, 2, 3)

        AlertDialog.Builder(this)
            .setTitle("Selecciona tu nivel")
            .setItems(niveles) { _, which ->
                val idNivelSeleccionado = idsNiveles[which]
                guardarHabilidadAlumno(token, idHabilidad, idNivelSeleccionado)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun guardarHabilidadAlumno(token: String, idHabilidad: Int, idNivel: Int) {
        RetrofitClient.instance.agregarHabilidad("Bearer $token", idHabilidad, idNivel).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@ProfileActivity, "Habilidad agregada con éxito", Toast.LENGTH_SHORT).show()
                    cargarDatosPerfil()
                } else {
                    Toast.makeText(this@ProfileActivity, "Error: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Toast.makeText(this@ProfileActivity, "Error de red: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
}