package com.example.linkedininterno.ui

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.linkedininterno.R
import com.example.linkedininterno.data.api.ApiClient
import com.example.linkedininterno.data.local.SessionManager
import com.example.linkedininterno.data.model.LoginRequest
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sessionManager = SessionManager(this)

        val etEmail = findViewById<EditText>(R.id.etcorreo)
        val etPassword = findViewById<EditText>(R.id.etcontrasena)
        val btnTogglePassword = findViewById<ImageButton>(R.id.btnOjoContrasena)
        val btnLogin = findViewById<Button>(R.id.button)
        val progressBar = findViewById<ProgressBar>(R.id.progressBarLogin)
        val tvRegisterHere = findViewById<TextView>(R.id.tvRegistrateAqui)

        var isPasswordVisible = false
        btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
            } else {
                etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            }
            etPassword.setSelection(etPassword.text.length)
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (!validateInputs(email, password, etEmail, etPassword)) {
                return@setOnClickListener
            }

            performLogin(email, password, btnLogin, progressBar)
        }

        tvRegisterHere.setOnClickListener {
            val intent = Intent(this, RoleSelectionActivity::class.java)
            startActivity(intent)
        }
    }

    private fun validateInputs(
        email: String,
        password: String,
        etEmail: EditText,
        etPassword: EditText
    ): Boolean {
        if (email.isEmpty()) {
            etEmail.error = "Ingresa tu correo electrónico"
            etEmail.requestFocus()
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Ingresa un correo electrónico válido"
            etEmail.requestFocus()
            return false
        }

        if (password.isEmpty()) {
            etPassword.error = "Ingresa tu contraseña"
            etPassword.requestFocus()
            return false
        }

        return true
    }

    private fun performLogin(
        email: String,
        password: String,
        btnLogin: Button,
        progressBar: ProgressBar
    ) {
        btnLogin.isEnabled = false
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val authService = ApiClient.getAuthService(this@LoginActivity)
                val response = authService.login(LoginRequest(email, password))

                btnLogin.isEnabled = true
                progressBar.visibility = View.GONE

                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    sessionManager.saveAuthToken(authResponse.token)
                    sessionManager.saveUser(authResponse.user)

                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    val errorMessage = when (response.code()) {
                        401 -> "Credenciales incorrectas. Verifique correo y contraseña."
                        404 -> "Usuario no encontrado."
                        else -> "Error de inicio de sesión: ${response.code()}"
                    }
                    showSnackbar(errorMessage)
                }
            } catch (e: Exception) {
                btnLogin.isEnabled = true
                progressBar.visibility = View.GONE
                showSnackbar("Error de conexión. Intente nuevamente.")
            }
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(findViewById(R.id.login), message, Snackbar.LENGTH_LONG).show()
    }
}
