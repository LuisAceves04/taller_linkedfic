package com.example.linkedininterno

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.linkedininterno.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MessagingActivity : AppCompatActivity() {

    private lateinit var rvConversaciones: RecyclerView
    private lateinit var conversacionesAdapter: ConversacionesAdapter
    private var listaConversaciones = listOf<Conversacion>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversaciones)

        rvConversaciones = findViewById(R.id.rvConversaciones)
        rvConversaciones.layoutManager = LinearLayoutManager(this)

        // Configurar el adaptador
        val prefs = getSharedPreferences("SesionUsuario", Context.MODE_PRIVATE)
        val token = prefs.getString("TOKEN", "") ?: ""

        conversacionesAdapter = ConversacionesAdapter(listaConversaciones, token) { conversacion ->
            abrirChat(conversacion.interlocutorId)
        }
        rvConversaciones.adapter = conversacionesAdapter

        cargarConversaciones()
    }

    override fun onResume() {
        super.onResume()
        cargarConversaciones()
    }

    private fun cargarConversaciones() {
        val prefs = getSharedPreferences("SesionUsuario", Context.MODE_PRIVATE)
        val token = prefs.getString("TOKEN", null) ?: return

        RetrofitClient.instance.obtenerConversaciones("Bearer $token").enqueue(object : Callback<RespuestaConversaciones> {
            override fun onResponse(call: Call<RespuestaConversaciones>, response: Response<RespuestaConversaciones>) {
                if (response.isSuccessful && response.body() != null) {
                    val respuesta = response.body()!!
                    if (respuesta.success) {
                        listaConversaciones = respuesta.conversaciones ?: emptyList()
                        conversacionesAdapter.actualizarDatos(listaConversaciones)
                    }
                } else {
                    Toast.makeText(this@MessagingActivity, "Error ${response.code()} al cargar chats", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<RespuestaConversaciones>, t: Throwable) {
                Toast.makeText(this@MessagingActivity, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun abrirChat(destinatarioId: String) {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("DESTINATARIO_ID", destinatarioId)
        startActivity(intent)
    }
}