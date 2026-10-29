package com.example.linkedininterno

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.linkedininterno.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PerfilEmpresaFragment : Fragment() {

    private lateinit var tvHeaderUsernameEmpresa: TextView
    private lateinit var tvNombreEmpresa: TextView
    private lateinit var tvEmailEmpresa: TextView
    private lateinit var tvTelefonoEmpresa: TextView
    private lateinit var tvWebEmpresa: TextView
    private lateinit var tvIndustriaEmpresa: TextView
    private lateinit var tvSobreMiEmpresa: TextView
    private lateinit var tvAvatarInitialEmpresa: TextView
    private lateinit var ivAvatarEmpresa: ImageView
    private lateinit var btnLogoutEmpresa: Button
    private lateinit var btnEditProfileEmpresa: Button

    private var idUsuarioVisitado: Int = -1
    private var esMiPropioPerfil: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Asegúrate de que el nombre del layout coincida con el nombre de tu archivo XML (ej. fragment_perfil_empresa o fragment_perfil_empresa_activity)
        val view = inflater.inflate(R.layout.fragment_perfil_empresa_activity, container, false)

        val prefs = requireContext().getSharedPreferences("SesionUsuario", Context.MODE_PRIVATE)
        val miIdUsuario = prefs.getInt("ID_USUARIO", -1)

        idUsuarioVisitado = arguments?.getInt("ID_USUARIO", -1) ?: -1
        esMiPropioPerfil = (idUsuarioVisitado == -1 || idUsuarioVisitado == miIdUsuario)

        // Vincular vistas con los IDs exactos de tu XML
        tvHeaderUsernameEmpresa = view.findViewById(R.id.tvHeaderUsernameEmpresa)
        tvNombreEmpresa = view.findViewById(R.id.tvProfileNameEmpresa)
        tvEmailEmpresa = view.findViewById(R.id.tvProfileEmailEmpresaValue)
        tvTelefonoEmpresa = view.findViewById(R.id.tvProfilePhoneEmpresaValue)
        tvWebEmpresa = view.findViewById(R.id.tvProfileWebValue)
        tvIndustriaEmpresa = view.findViewById(R.id.tvProfileIndustryValue)
        tvSobreMiEmpresa = view.findViewById(R.id.tvProfileAboutEmpresaValue)
        tvAvatarInitialEmpresa = view.findViewById(R.id.tvAvatarInitialEmpresa)
        ivAvatarEmpresa = view.findViewById(R.id.ivAvatarProfileEmpresa)
        btnLogoutEmpresa = view.findViewById(R.id.btnLogoutProfileEmpresa)
        btnEditProfileEmpresa = view.findViewById(R.id.btnEditProfileEmpresa)

        // Si es un perfil de tercero, ocultamos botones de gestión personal
        if (!esMiPropioPerfil) {
            btnEditProfileEmpresa.visibility = View.GONE
            btnLogoutEmpresa.visibility = View.GONE
        }

        btnEditProfileEmpresa.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileEmpresaActivity::class.java))
        }

        btnLogoutEmpresa.setOnClickListener {
            prefs.edit().clear().apply()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }

        cargarPerfilEmpresa()

        return view
    }

    private fun cargarPerfilEmpresa() {
        val prefs = requireContext().getSharedPreferences("SesionUsuario", Context.MODE_PRIVATE)
        val token = prefs.getString("TOKEN", null) ?: return

        val call: Call<RespuestaPerfil> = if (esMiPropioPerfil) {
            RetrofitClient.instance.obtenerPerfil("Bearer $token")
        } else {
            RetrofitClient.instance.obtenerPerfilPorId("Bearer $token", idUsuarioVisitado)
        }

        call.enqueue(object : Callback<RespuestaPerfil> {
            override fun onResponse(call: Call<RespuestaPerfil>, response: Response<RespuestaPerfil>) {
                if (response.isSuccessful && response.body() != null) {
                    val usuario = response.body()!!.usuario ?: return
                    val empresa = usuario.empresa

                    val nombreEmpresa = empresa?.nombreEmpresa ?: "Empresa"
                    tvHeaderUsernameEmpresa.text = nombreEmpresa
                    tvNombreEmpresa.text = nombreEmpresa
                    tvEmailEmpresa.text = usuario.email ?: "Sin correo"
                    tvTelefonoEmpresa.text = usuario.telefono ?: "Sin teléfono"
                    tvWebEmpresa.text = empresa?.paginaWeb ?: "Sin página web"
                    tvIndustriaEmpresa.text = empresa?.industria?.industria ?: "Sin especificar"
                    tvSobreMiEmpresa.text = usuario.sobreMi ?: "Sin descripción"

                    val avatarUrl = usuario.avatar
                    if (!avatarUrl.isNullOrEmpty()) {
                        tvAvatarInitialEmpresa.visibility = View.GONE
                        ivAvatarEmpresa.visibility = View.VISIBLE
                        val urlCompleta = if (avatarUrl.startsWith("http")) avatarUrl else "http://10.0.2.2:8000/storage/$avatarUrl"
                        Glide.with(this@PerfilEmpresaFragment)
                            .load(urlCompleta)
                            .circleCrop()
                            .into(ivAvatarEmpresa)
                    } else {
                        tvAvatarInitialEmpresa.visibility = View.VISIBLE
                        ivAvatarEmpresa.visibility = View.GONE
                        tvAvatarInitialEmpresa.text = nombreEmpresa.firstOrNull()?.toString()?.uppercase() ?: "E"
                    }
                }
            }

            override fun onFailure(call: Call<RespuestaPerfil>, t: Throwable) {
                Toast.makeText(context, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}