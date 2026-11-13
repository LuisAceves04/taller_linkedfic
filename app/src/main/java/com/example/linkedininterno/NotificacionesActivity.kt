package com.example.linkedininterno

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.linkedininterno.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NotificacionesActivity : AppCompatActivity() {

    private lateinit var rvNotificaciones: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notificaciones)

        val btnVolver = findViewById<Button>(R.id.btnVolverMain)
        btnVolver.setOnClickListener {
            finish()
        }
        val toolbar = findViewById<Toolbar>(R.id.toolbarNotificaciones)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Notificaciones"

        rvNotificaciones = findViewById(R.id.rvNotificaciones)
        rvNotificaciones.layoutManager = LinearLayoutManager(this)

        cargarNotificaciones()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun cargarNotificaciones() {
        val prefs = getSharedPreferences("SesionUsuario", Context.MODE_PRIVATE)
        val token = prefs.getString("TOKEN", null)

        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Sesión no válida", Toast.LENGTH_SHORT).show()
            return
        }

        RetrofitClient.instance.obtenerNotificaciones("Bearer $token").enqueue(object : Callback<com.google.gson.JsonElement> {
            override fun onResponse(call: Call<com.google.gson.JsonElement>, response: Response<com.google.gson.JsonElement>) {
                if (response.isSuccessful && response.body() != null) {
                    try {
                        val element = response.body()!!
                        val listaNotificaciones = mutableListOf<Notificacion>()

                        val jsonArray = when {
                            element.isJsonArray -> element.asJsonArray
                            element.isJsonObject && element.asJsonObject.has("data") -> element.asJsonObject.getAsJsonArray("data")
                            element.isJsonObject && element.asJsonObject.has("notificaciones") -> element.asJsonObject.getAsJsonArray("notificaciones")
                            else -> null
                        }

                        if (jsonArray != null) {
                            val gson = com.google.gson.Gson()
                            jsonArray.forEach { jsonElement ->
                                try {
                                    val notif = gson.fromJson(jsonElement, Notificacion::class.java)
                                    if (notif != null) listaNotificaciones.add(notif)
                                } catch (e: Exception) {
                                }
                            }
                        }

                        val adapter = NotificacionesAdapter(listaNotificaciones)
                        rvNotificaciones.adapter = adapter

                    } catch (e: Exception) {
                        Toast.makeText(this@NotificacionesActivity, "Error al procesar datos: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@NotificacionesActivity, "Error en servidor: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.google.gson.JsonElement>, t: Throwable) {
                Toast.makeText(this@NotificacionesActivity, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}