package com.example.linkedininterno

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.linkedininterno.network.RetrofitClient
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.etcorreo)
        val etPassword = findViewById<EditText>(R.id.etcontrasena)
        val btnLogin = findViewById<Button>(R.id.buttonLogin)
        val tvRegistrate = findViewById<TextView>(R.id.tvRegistrateAqui)

        val btnOjoContrasena = findViewById<ImageButton>(R.id.btnOjoContrasena)

        var isPasswordVisible = false

        btnOjoContrasena.setOnClickListener {
            if (isPasswordVisible) {
                etPassword.transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
                btnOjoContrasena.setImageResource(android.R.drawable.ic_menu_view)
                isPasswordVisible = false
            } else {
                etPassword.transformationMethod = android.text.method.HideReturnsTransformationMethod.getInstance()
                btnOjoContrasena.setImageResource(android.R.drawable.ic_menu_upload)
                isPasswordVisible = true
            }
            etPassword.setSelection(etPassword.text.length)
        }

        tvRegistrate.setOnClickListener {
            val intent = Intent(this, RegisterTypeActivity::class.java)
            startActivity(intent)
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor llena todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val jsonBody = JsonObject().apply {
                addProperty("email", email)
                addProperty("password", password)
            }

            val call = RetrofitClient.instance.login(jsonBody)

            call.enqueue(object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    if (response.isSuccessful && response.body() != null) {
                        val jsonResponse = response.body()!!
                        val accessToken = jsonResponse.get("access_token")?.asString

                        if (accessToken != null) {
                            val sharedPreferences = getSharedPreferences("SesionUsuario", MODE_PRIVATE)
                            sharedPreferences.edit().putString("TOKEN", accessToken).apply()

                            obtenerIdDesdePerfil(accessToken)
                        } else {
                            Toast.makeText(this@LoginActivity, "No se recibió el token", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@LoginActivity, "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                    Toast.makeText(this@LoginActivity, "Error de conexión: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
        }
    }

    private fun obtenerIdDesdePerfil(token: String) {
        RetrofitClient.instance.obtenerPerfilJson("Bearer $token").enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                if (response.isSuccessful && response.body() != null) {
                    val json = response.body()!!

                    var idUsuario = -1
                    var idRol = 1 // Por defecto Alumno si no viene en la respuesta

                    try {
                        val usuarioObj = when {
                            json.has("usuario") -> json.getAsJsonObject("usuario")
                            json.has("user") -> json.getAsJsonObject("user")
                            json.has("data") -> json.getAsJsonObject("data")
                            else -> json
                        }

                        if (usuarioObj.has("idUsuario")) {
                            idUsuario = usuarioObj.get("idUsuario").asInt
                        } else if (usuarioObj.has("id")) {
                            idUsuario = usuarioObj.get("id").asInt
                        }

                        if (usuarioObj.has("idRol")) {
                            idRol = usuarioObj.get("idRol").asInt
                        }
                    } catch (e: Exception) {
                        Log.e("LOGIN_DEBUG", "Error extrayendo ID/Rol: ${e.message}")
                    }

                    if (idUsuario != -1) {
                        val sharedPreferences = getSharedPreferences("SesionUsuario", MODE_PRIVATE)
                        sharedPreferences.edit()
                            .putInt("ID_USUARIO", idUsuario)
                            .putInt("ID_ROL", idRol)
                            .putInt("idRol", idRol)
                            .commit()

                        Toast.makeText(this@LoginActivity, "¡Bienvenido!", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Error: JSON no coincide", Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Toast.makeText(this@LoginActivity, "Error de red al iniciar", Toast.LENGTH_LONG).show()
            }
        })
    }
}