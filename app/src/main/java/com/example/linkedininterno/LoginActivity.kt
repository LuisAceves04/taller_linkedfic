package com.example.linkedininterno

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
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

        // Vincular los elementos visuales con sus IDs correctos
        val etEmail = findViewById<EditText>(R.id.etcorreo)
        val etPassword = findViewById<EditText>(R.id.etcontrasena)
        val btnLogin = findViewById<Button>(R.id.buttonLogin)
        val tvRegistrate = findViewById<TextView>(R.id.tvRegistrateAqui)

        tvRegistrate.setOnClickListener {
            val intent = Intent(this, RegisterStudentActivity::class.java)
            startActivity(intent)
        }

        // Acción cuando presionen el botón
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Validar que los campos no estén vacíos
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor llena todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // JSON que se mandará a Laravel
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
                    try {
                        if (json.has("usuario")) {
                            idUsuario = json.getAsJsonObject("usuario").get("idUsuario").asInt
                        } else if (json.has("user")) {
                            idUsuario = json.getAsJsonObject("user").get("id").asInt
                        } else if (json.has("data")) {
                            idUsuario = json.getAsJsonObject("data").get("id").asInt
                        } else if (json.has("id")) {
                            idUsuario = json.get("id").asInt
                        }
                    } catch (e: Exception) {
                        Log.e("LOGIN_DEBUG", "Error extrayendo ID: ${e.message}")
                    }

                    if (idUsuario != -1) {
                        val sharedPreferences = getSharedPreferences("SesionUsuario", MODE_PRIVATE)
                        sharedPreferences.edit().putInt("ID_USUARIO", idUsuario).commit()

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