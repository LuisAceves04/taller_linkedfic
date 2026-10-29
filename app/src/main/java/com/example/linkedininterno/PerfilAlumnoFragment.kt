package com.example.linkedininterno

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.linkedininterno.network.RetrofitClient
import com.google.android.material.card.MaterialCardView
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class PerfilAlumnoFragment : Fragment() {

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
    private var idUsuarioVisitado: Int = -1
    private var esMiPropioPerfil: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_profile, container, false)

        val prefs = requireContext().getSharedPreferences("SesionUsuario", Context.MODE_PRIVATE)
        val miIdUsuario = prefs.getInt("ID_USUARIO", -1)

        idUsuarioVisitado = arguments?.getInt("ID_USUARIO", -1) ?: -1
        esMiPropioPerfil = (idUsuarioVisitado == -1 || idUsuarioVisitado == miIdUsuario)

        // Vincular componentes
        tvHeaderUsername = view.findViewById(R.id.tvHeaderUsername)
        tvProfileName = view.findViewById(R.id.tvProfileName)
        tvProfileRoleBadge = view.findViewById(R.id.tvProfileRoleBadge)
        tvProfileMajorValue = view.findViewById(R.id.tvProfileMajorValue)
        tvProfileSemesterValue = view.findViewById(R.id.tvProfileSemesterValue)
        tvProfileEmailValue = view.findViewById(R.id.tvProfileEmailValue)
        tvProfilePhoneValue = view.findViewById(R.id.tvProfilePhoneValue)
        tvProfileAboutValue = view.findViewById(R.id.tvProfileAboutValue)
        tvProfileSkillsValue = view.findViewById(R.id.tvProfileSkillsValue)
        tvAvatarInitial = view.findViewById(R.id.tvAvatarInitial)
        cardAvatar = view.findViewById(R.id.cardAvatar)
        btnLogoutProfile = view.findViewById(R.id.btnLogoutProfile)
        btnVerCurriculum = view.findViewById(R.id.btnDownloadCv)
        btnEditProfile = view.findViewById(R.id.btnEditProfile)
        btnAddSkill = view.findViewById(R.id.btnAddSkill)

        if (!esMiPropioPerfil) {
            btnEditProfile.visibility = View.GONE
            btnAddSkill.visibility = View.GONE
            btnLogoutProfile.visibility = View.GONE
        }

        ivAvatarPerfil = ImageView(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            visibility = View.GONE
        }
        cardAvatar.addView(ivAvatarPerfil)

        btnEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }

        btnLogoutProfile.setOnClickListener {
            prefs.edit().clear().apply()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }

        cargarDatosAlumno()

        return view
    }

    private fun cargarDatosAlumno() {
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
                    val alumno = usuario.alumno ?: return

                    tvProfileName.text = "${alumno.nombre ?: ""} ${alumno.apellidoPaterno ?: ""} ${alumno.apellidoMaterno ?: ""}".trim()
                    tvHeaderUsername.text = alumno.nombre ?: "Usuario"
                    tvProfileRoleBadge.text = "Alumno"
                    tvProfileMajorValue.text = alumno.carrera?.carrera ?: "Sin carrera"
                    tvProfileSemesterValue.text = (alumno.semestre ?: 0).toString()

                    tvProfileEmailValue.text = usuario.email ?: "Sin correo"
                    tvProfilePhoneValue.text = usuario.telefono ?: "No registrado"
                    tvProfileAboutValue.text = usuario.sobreMi ?: "Sin descripción"

                    tieneCurriculum = !alumno.curriculum.isNullOrEmpty()
                    btnVerCurriculum.visibility = if (tieneCurriculum) View.VISIBLE else View.GONE

                    val habilidades = alumno.habilidades
                    tvProfileSkillsValue.text = if (!habilidades.isNullOrEmpty()) {
                        habilidades.joinToString(", ") { it.nombre ?: "" }
                    } else {
                        "Sin habilidades registradas"
                    }

                    val avatarUrl = usuario.avatar
                    if (!avatarUrl.isNullOrEmpty()) {
                        tvAvatarInitial.visibility = View.GONE
                        ivAvatarPerfil?.visibility = View.VISIBLE
                        val urlCompleta = if (avatarUrl.startsWith("http")) avatarUrl else "http://10.0.2.2:8000/storage/$avatarUrl"
                        Glide.with(this@PerfilAlumnoFragment).load(urlCompleta).centerCrop().into(ivAvatarPerfil!!)
                    } else {
                        tvAvatarInitial.visibility = View.VISIBLE
                        ivAvatarPerfil?.visibility = View.GONE
                        tvAvatarInitial.text = (alumno.nombre?.firstOrNull() ?: 'U').toString().uppercase()
                    }
                }
            }

            override fun onFailure(call: Call<RespuestaPerfil>, t: Throwable) {
                Toast.makeText(context, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}