package com.example.linkedininterno

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.linkedininterno.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_container)

        val sharedPreferences = getSharedPreferences("SesionUsuario", MODE_PRIVATE)
        val miIdUsuario = sharedPreferences.getInt("ID_USUARIO", -1)
        val miRol = sharedPreferences.getInt("ID_ROL", sharedPreferences.getInt("idRol", 1))

        val idUsuarioVisitado = intent.getIntExtra("ID_USUARIO", -1)
        val esMiPropioPerfil = (idUsuarioVisitado == -1 || idUsuarioVisitado == miIdUsuario)

        if (esMiPropioPerfil) {
            // Si es perfil propio, cargamos según el rol en SharedPreferences
            cargarFragmentoPorRol(miRol, miIdUsuario)
        } else {
            // Si es perfil de terceros, consultamos al backend para resolver si es Alumno o Empresa
            obtenerTipoUsuarioYCargarFragmento(idUsuarioVisitado)
        }
    }

    private fun obtenerTipoUsuarioYCargarFragmento(idUsuario: Int) {
        val sharedPreferences = getSharedPreferences("SesionUsuario", MODE_PRIVATE)
        val token = sharedPreferences.getString("TOKEN", null)

        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Sesión no válida", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        RetrofitClient.instance.obtenerPerfilPorId("Bearer $token", idUsuario).enqueue(object : Callback<RespuestaPerfil> {
            override fun onResponse(call: Call<RespuestaPerfil>, response: Response<RespuestaPerfil>) {
                if (response.isSuccessful && response.body() != null) {
                    val usuario = response.body()!!.usuario
                    if (usuario != null) {
                        val idRol = when {
                            usuario.empresa != null -> 2
                            usuario.alumno != null -> 1
                            else -> 1
                        }
                        cargarFragmentoPorRol(idRol, idUsuario)
                    } else {
                        Toast.makeText(this@ProfileActivity, "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this@ProfileActivity, "Error ${response.code()} al cargar perfil", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

            override fun onFailure(call: Call<RespuestaPerfil>, t: Throwable) {
                Toast.makeText(this@ProfileActivity, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
    }

    private fun cargarFragmentoPorRol(idRol: Int, idUsuario: Int) {
        val bundle = Bundle().apply {
            putInt("ID_USUARIO", idUsuario)
        }

        val fragment = if (idRol == 2) {
            PerfilEmpresaFragment().apply { arguments = bundle }
        } else {
            PerfilAlumnoFragment().apply { arguments = bundle }
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.profileContainer, fragment)
            .commit()
    }
}