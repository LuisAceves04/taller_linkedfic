package com.example.linkedininterno.ui

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.linkedininterno.R
import com.example.linkedininterno.data.api.ApiClient
import com.example.linkedininterno.data.local.SessionManager
import com.example.linkedininterno.data.model.IndustryDto
import com.example.linkedininterno.data.model.RegisterCompanyRequest
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class RegisterCompanyActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private var industriesList: List<IndustryDto> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register_company)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.scrollRegisterCompany)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sessionManager = SessionManager(this)

        val etCompanyName = findViewById<EditText>(R.id.etCompanyName)
        val etCompanyEmail = findViewById<EditText>(R.id.etCompanyEmail)
        val etCompanyPassword = findViewById<EditText>(R.id.etCompanyPassword)
        val etCompanyDescription = findViewById<EditText>(R.id.etCompanyDescription)
        val spIndustries = findViewById<Spinner>(R.id.spIndustries)
        val btnRegister = findViewById<Button>(R.id.btnRegisterCompany)
        val progressBar = findViewById<ProgressBar>(R.id.progressBarCompany)
        val tvBackToLogin = findViewById<TextView>(R.id.tvBackToLoginFromCompany)

        loadIndustries(spIndustries)

        btnRegister.setOnClickListener {
            val companyName = etCompanyName.text.toString().trim()
            val email = etCompanyEmail.text.toString().trim()
            val password = etCompanyPassword.text.toString().trim()
            val description = etCompanyDescription.text.toString().trim().ifEmpty { null }
            val selectedIndustryIndex = spIndustries.selectedItemPosition

            if (!validateInputs(companyName, email, password, selectedIndustryIndex, etCompanyName, etCompanyEmail, etCompanyPassword)) {
                return@setOnClickListener
            }

            val selectedIndustry = industriesList.getOrNull(selectedIndustryIndex)
            if (selectedIndustry == null) {
                showSnackbar("Por favor selecciona una industria válida")
                return@setOnClickListener
            }

            val request = RegisterCompanyRequest(
                email = email,
                password = password,
                companyName = companyName,
                industryId = selectedIndustry.id,
                description = description
            )

            performRegistration(request, btnRegister, progressBar)
        }

        tvBackToLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun loadIndustries(spIndustries: Spinner) {
        lifecycleScope.launch {
            try {
                val catalogService = ApiClient.getCatalogService(this@RegisterCompanyActivity)
                val response = catalogService.getIndustries()

                if (response.isSuccessful && response.body() != null) {
                    industriesList = response.body()!!
                } else {
                    industriesList = listOf(
                        IndustryDto(1, "Tecnología de la Información"),
                        IndustryDto(2, "Desarrollo de Software"),
                        IndustryDto(3, "Consultoría e Ingeniería"),
                        IndustryDto(4, "Telecomunicaciones")
                    )
                }
            } catch (e: Exception) {
                // Default fallback if catalog endpoint is not available yet
                industriesList = listOf(
                    IndustryDto(1, "Tecnología de la Información"),
                    IndustryDto(2, "Desarrollo de Software"),
                    IndustryDto(3, "Consultoría e Ingeniería"),
                    IndustryDto(4, "Telecomunicaciones")
                )
            }

            val adapter = ArrayAdapter(
                this@RegisterCompanyActivity,
                android.R.layout.simple_spinner_item,
                industriesList.map { it.name }
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spIndustries.adapter = adapter
        }
    }

    private fun validateInputs(
        companyName: String,
        email: String,
        password: String,
        industryIndex: Int,
        etCompanyName: EditText,
        etCompanyEmail: EditText,
        etCompanyPassword: EditText
    ): Boolean {
        if (companyName.isEmpty()) {
            etCompanyName.error = "Ingresa el nombre de la empresa"
            etCompanyName.requestFocus()
            return false
        }

        if (email.isEmpty()) {
            etCompanyEmail.error = "Ingresa tu correo electrónico corporativo"
            etCompanyEmail.requestFocus()
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etCompanyEmail.error = "Ingresa un correo electrónico válido"
            etCompanyEmail.requestFocus()
            return false
        }

        if (password.length < 6) {
            etCompanyPassword.error = "La contraseña debe tener al menos 6 caracteres"
            etCompanyPassword.requestFocus()
            return false
        }

        if (industryIndex < 0 || industriesList.isEmpty()) {
            showSnackbar("Selecciona la industria o sector de la empresa")
            return false
        }

        return true
    }

    private fun performRegistration(
        request: RegisterCompanyRequest,
        btnRegister: Button,
        progressBar: ProgressBar
    ) {
        btnRegister.isEnabled = false
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val authService = ApiClient.getAuthService(this@RegisterCompanyActivity)
                val response = authService.registerCompany(request)

                btnRegister.isEnabled = true
                progressBar.visibility = View.GONE

                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    sessionManager.saveAuthToken(authResponse.token)
                    sessionManager.saveUser(authResponse.user)

                    val intent = Intent(this@RegisterCompanyActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    val errorMsg = when (response.code()) {
                        400 -> "Datos de registro inválidos o correo ya registrado."
                        409 -> "El correo corporativo ya se encuentra registrado."
                        else -> "Error en el registro: ${response.code()}"
                    }
                    showSnackbar(errorMsg)
                }
            } catch (e: Exception) {
                btnRegister.isEnabled = true
                progressBar.visibility = View.GONE
                showSnackbar("Error de conexión. Intente nuevamente.")
            }
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(findViewById(R.id.scrollRegisterCompany), message, Snackbar.LENGTH_LONG).show()
    }
}
