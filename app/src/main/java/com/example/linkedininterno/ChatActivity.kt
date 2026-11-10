package com.example.linkedininterno

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.linkedininterno.network.RetrofitClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChatActivity : AppCompatActivity() {

    private lateinit var rvMensajes: RecyclerView
    private lateinit var etMessageInput: TextInputEditText
    private lateinit var btnSendMessage: MaterialButton
    private lateinit var btnBackChat: ImageButton
    private lateinit var tvChatDestinatarioNombre: TextView
    private lateinit var tvChatAvatarInitials: TextView
    private lateinit var ivChatAvatarHeader: ImageView

    private lateinit var mensajesAdapter: MensajesAdapter
    private var listaMensajes = mutableListOf<Mensaje>()
    private var destinatarioId: String = ""
    private var miIdUsuario: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val prefs = getSharedPreferences("SesionUsuario", Context.MODE_PRIVATE)
        val token = prefs.getString("TOKEN", null) ?: return
        miIdUsuario = prefs.getInt("ID_USUARIO", -1).toString()
        destinatarioId = intent.getStringExtra("DESTINATARIO_ID") ?: ""

        if (destinatarioId.isEmpty()) {
            Toast.makeText(this, "Error: Usuario no válido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Vincular vistas
        rvMensajes = findViewById(R.id.rvMensajes)
        etMessageInput = findViewById(R.id.etMessageInput)
        btnSendMessage = findViewById(R.id.btnSendMessage)
        btnBackChat = findViewById(R.id.btnBackChat)
        tvChatDestinatarioNombre = findViewById(R.id.tvChatDestinatarioNombre)
        tvChatAvatarInitials = findViewById(R.id.tvChatAvatarInitials)
        ivChatAvatarHeader = findViewById(R.id.ivChatAvatarHeader)

        // Configuración inicial provisional antes de recibir respuesta de la red
        val nombreTemp = "Usuario $destinatarioId"
        tvChatDestinatarioNombre.text = nombreTemp
        tvChatAvatarInitials.text = nombreTemp.first().toString().uppercase()

        // Configurar RecyclerView
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true // Empieza desde abajo (estilo chat)
        rvMensajes.layoutManager = layoutManager

        mensajesAdapter = MensajesAdapter(listaMensajes, miIdUsuario)
        rvMensajes.adapter = mensajesAdapter

        // Botón regresar
        btnBackChat.setOnClickListener { finish() }

        // Botón enviar
        btnSendMessage.setOnClickListener {
            val texto = etMessageInput.text.toString().trim()
            if (texto.isNotEmpty()) {
                enviarMensaje(token, texto)
            }
        }

        // Cargar perfil del interlocutor y mensajes
        cargarInformacionInterlocutor(token)
        cargarMensajes(token)
    }

    private fun cargarInformacionInterlocutor(token: String) {
        RetrofitClient.instance.obtenerPerfilPorId("Bearer $token", destinatarioId.toInt()).enqueue(object : Callback<RespuestaPerfil> {
            override fun onResponse(call: Call<RespuestaPerfil>, response: Response<RespuestaPerfil>) {
                if (response.isSuccessful && response.body() != null) {
                    val usuario = response.body()!!.usuario ?: return
                    val alumno = usuario.alumno
                    val empresa = usuario.empresa

                    val nombreReal = when {
                        alumno != null -> "${alumno.nombre ?: ""} ${alumno.apellidoPaterno ?: ""}".trim()
                        empresa != null -> empresa.nombreEmpresa ?: "Empresa"
                        else -> "Usuario $destinatarioId"
                    }

                    tvChatDestinatarioNombre.text = nombreReal

                    val avatarUrl = usuario.avatar
                    if (!avatarUrl.isNullOrEmpty()) {
                        tvChatAvatarInitials.visibility = View.GONE
                        ivChatAvatarHeader.visibility = View.VISIBLE
                        val urlCompleta = if (avatarUrl.startsWith("http")) avatarUrl else "http://10.0.2.2:8000/storage/$avatarUrl"

                        Glide.with(this@ChatActivity)
                            .load(urlCompleta)
                            .centerCrop()
                            .into(ivChatAvatarHeader)
                    } else {
                        tvChatAvatarInitials.visibility = View.VISIBLE
                        ivChatAvatarHeader.visibility = View.GONE
                        tvChatAvatarInitials.text = (nombreReal.firstOrNull() ?: 'U').toString().uppercase()
                    }
                }
            }

            override fun onFailure(call: Call<RespuestaPerfil>, t: Throwable) {
                // Silencioso, conserva los valores por defecto si falla la red
            }
        })
    }

    private fun cargarMensajes(token: String) {
        RetrofitClient.instance.obtenerMensajes("Bearer $token", destinatarioId).enqueue(object : Callback<RespuestaMensajes> {
            override fun onResponse(call: Call<RespuestaMensajes>, response: Response<RespuestaMensajes>) {
                if (response.isSuccessful && response.body() != null) {
                    val respuesta = response.body()!!
                    if (respuesta.success) {
                        listaMensajes = (respuesta.mensajes ?: emptyList()).toMutableList()
                        mensajesAdapter.actualizarMensajes(listaMensajes)
                        if (listaMensajes.isNotEmpty()) {
                            rvMensajes.scrollToPosition(listaMensajes.size - 1)
                        }
                    }
                }
            }

            override fun onFailure(call: Call<RespuestaMensajes>, t: Throwable) {
                Toast.makeText(this@ChatActivity, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun enviarMensaje(token: String, texto: String) {
        etMessageInput.text?.clear()

        val peticion = PeticionEnviarMensaje(destinatarioId = destinatarioId, mensaje = texto)

        RetrofitClient.instance.enviarMensaje("Bearer $token", peticion).enqueue(object : Callback<RespuestaEnviarMensaje> {
            override fun onResponse(call: Call<RespuestaEnviarMensaje>, response: Response<RespuestaEnviarMensaje>) {
                if (response.isSuccessful && response.body() != null) {
                    val respuesta = response.body()!!
                    if (respuesta.success && respuesta.mensaje != null) {
                        listaMensajes.add(respuesta.mensaje)
                        mensajesAdapter.actualizarMensajes(listaMensajes)
                        rvMensajes.scrollToPosition(listaMensajes.size - 1)
                    }
                } else {
                    Toast.makeText(this@ChatActivity, "No se pudo enviar", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<RespuestaEnviarMensaje>, t: Throwable) {
                Toast.makeText(this@ChatActivity, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}