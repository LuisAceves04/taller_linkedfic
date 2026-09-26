package com.example.linkedininterno.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Patterns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.linkedininterno.R
import com.example.linkedininterno.data.api.ApiClient
import com.example.linkedininterno.data.local.SessionManager
import com.example.linkedininterno.data.model.MajorDto
import com.example.linkedininterno.data.model.RegisterStudentRequest
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class RegisterStudentActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private var majorsList: List<MajorDto> = emptyList()
    private var selectedPdfUri: Uri? = null

    private val selectPdfLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedPdfUri = uri
            val fileName = getFileName(this, uri) ?: "Archivo PDF seleccionado"
            findViewById<TextView>(R.id.tvSelectedCvName).text = fileName
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register_student)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.scrollRegisterStudent)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sessionManager = SessionManager(this)

        val etFirstName = findViewById<EditText>(R.id.etStudentFirstName)
        val etPaternalSurname = findViewById<EditText>(R.id.etStudentPaternalSurname)
        val etMaternalSurname = findViewById<EditText>(R.id.etStudentMaternalSurname)
        val etEmail = findViewById<EditText>(R.id.etStudentEmail)
        val etPassword = findViewById<EditText>(R.id.etStudentPassword)
        val etPhone = findViewById<EditText>(R.id.etStudentPhone)
        val etAboutMe = findViewById<EditText>(R.id.etStudentAboutMe)
        val spMajors = findViewById<Spinner>(R.id.spMajors)
        val btnSelectCv = findViewById<Button>(R.id.btnSelectCv)
        val btnRegister = findViewById<Button>(R.id.btnRegisterStudent)
        val progressBar = findViewById<ProgressBar>(R.id.progressBarStudent)
        val tvBackToLogin = findViewById<TextView>(R.id.tvBackToLoginFromStudent)

        loadMajors(spMajors)

        btnSelectCv.setOnClickListener {
            selectPdfLauncher.launch("application/pdf")
        }

        btnRegister.setOnClickListener {
            val nombre = etFirstName.text.toString().trim()
            val apellidoPaterno = etPaternalSurname.text.toString().trim()
            val apellidoMaterno = etMaternalSurname.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val telefono = etPhone.text.toString().trim()
            val sobreMi = etAboutMe.text.toString().trim()
            val selectedMajorIndex = spMajors.selectedItemPosition

            if (!validateInputs(
                    nombre, apellidoPaterno, apellidoMaterno, email, password, telefono, sobreMi,
                    selectedMajorIndex, etFirstName, etPaternalSurname, etMaternalSurname, etEmail, etPassword, etPhone, etAboutMe
                )
            ) {
                return@setOnClickListener
            }

            val selectedMajor = majorsList.getOrNull(selectedMajorIndex)
            if (selectedMajor == null) {
                showSnackbar("Por favor selecciona una carrera válida")
                return@setOnClickListener
            }

            val request = RegisterStudentRequest(
                idRol = 1,
                email = email,
                password = password,
                telefono = telefono,
                sobreMi = sobreMi,
                nombre = nombre,
                apellidoPaterno = apellidoPaterno,
                apellidoMaterno = apellidoMaterno,
                idCarrera = selectedMajor.id
            )

            performRegistration(request, selectedPdfUri, btnRegister, progressBar)
        }

        tvBackToLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun loadMajors(spMajors: Spinner) {
        lifecycleScope.launch {
            try {
                val catalogService = ApiClient.getCatalogService(this@RegisterStudentActivity)
                val response = catalogService.getMajors()

                if (response.isSuccessful && response.body() != null) {
                    majorsList = response.body()!!
                } else {
                    majorsList = listOf(
                        MajorDto(1, "Ingeniería en Software"),
                        MajorDto(2, "Ingeniería en Computación"),
                        MajorDto(3, "Licenciatura en Informática")
                    )
                }
            } catch (e: Exception) {
                majorsList = listOf(
                    MajorDto(1, "Ingeniería en Software"),
                    MajorDto(2, "Ingeniería en Computación"),
                    MajorDto(3, "Licenciatura en Informática")
                )
            }

            val adapter = ArrayAdapter(
                this@RegisterStudentActivity,
                android.R.layout.simple_spinner_item,
                majorsList.map { it.name }
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spMajors.adapter = adapter
        }
    }

    private fun validateInputs(
        nombre: String,
        apellidoPaterno: String,
        apellidoMaterno: String,
        email: String,
        password: String,
        telefono: String,
        sobreMi: String,
        majorIndex: Int,
        etFirstName: EditText,
        etPaternalSurname: EditText,
        etMaternalSurname: EditText,
        etEmail: EditText,
        etPassword: EditText,
        etPhone: EditText,
        etAboutMe: EditText
    ): Boolean {
        if (nombre.isEmpty()) {
            etFirstName.error = "Ingresa tu nombre"
            etFirstName.requestFocus()
            return false
        }

        if (apellidoPaterno.isEmpty()) {
            etPaternalSurname.error = "Ingresa tu apellido paterno"
            etPaternalSurname.requestFocus()
            return false
        }

        if (apellidoMaterno.isEmpty()) {
            etMaternalSurname.error = "Ingresa tu apellido materno"
            etMaternalSurname.requestFocus()
            return false
        }

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

        if (password.length < 6) {
            etPassword.error = "La contraseña debe tener al menos 6 caracteres"
            etPassword.requestFocus()
            return false
        }

        if (telefono.isEmpty()) {
            etPhone.error = "Ingresa tu número de teléfono"
            etPhone.requestFocus()
            return false
        }

        if (sobreMi.isEmpty()) {
            etAboutMe.error = "Ingresa una breve descripción sobre ti"
            etAboutMe.requestFocus()
            return false
        }

        if (majorIndex < 0 || majorsList.isEmpty()) {
            showSnackbar("Selecciona tu carrera")
            return false
        }

        return true
    }

    private fun performRegistration(
        request: RegisterStudentRequest,
        pdfUri: Uri?,
        btnRegister: Button,
        progressBar: ProgressBar
    ) {
        btnRegister.isEnabled = false
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val authService = ApiClient.getAuthService(this@RegisterStudentActivity)

                val idRolPart = request.idRol.toString().toTextRequestBody()
                val emailPart = request.email.toTextRequestBody()
                val passwordPart = request.password.toTextRequestBody()
                val telefonoPart = request.telefono.toTextRequestBody()
                val sobreMiPart = request.sobreMi.toTextRequestBody()
                val nombrePart = request.nombre.toTextRequestBody()
                val apellidoPaternoPart = request.apellidoPaterno.toTextRequestBody()
                val apellidoMaternoPart = request.apellidoMaterno.toTextRequestBody()
                val idCarreraPart = request.idCarrera.toString().toTextRequestBody()

                val cvPart = pdfUri?.let { createPdfPart(this@RegisterStudentActivity, it) }

                var response = authService.registerStudentMultipart(
                    idRol = idRolPart,
                    email = emailPart,
                    password = passwordPart,
                    telefono = telefonoPart,
                    sobreMi = sobreMiPart,
                    nombre = nombrePart,
                    apellidoPaterno = apellidoPaternoPart,
                    apellidoMaterno = apellidoMaternoPart,
                    idCarrera = idCarreraPart,
                    curriculum = cvPart
                )

                if (!response.isSuccessful && response.code() == 404) {
                    response = authService.registerStudentJson(request)
                }

                btnRegister.isEnabled = true
                progressBar.visibility = View.GONE

                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    sessionManager.saveAuthToken(authResponse.token)
                    sessionManager.saveUser(authResponse.user)

                    val intent = Intent(this@RegisterStudentActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    val errorMsg = when (response.code()) {
                        400 -> "Datos de registro inválidos o correo ya registrado."
                        409 -> "El correo ya se encuentra registrado."
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

    private fun String.toTextRequestBody(): RequestBody {
        return this.toRequestBody("text/plain".toMediaTypeOrNull())
    }

    private fun createPdfPart(context: Context, uri: Uri): MultipartBody.Part? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bytes = inputStream.readBytes()
            val requestFile = bytes.toRequestBody("application/pdf".toMediaTypeOrNull())
            val fileName = getFileName(context, uri) ?: "curriculum.pdf"
            MultipartBody.Part.createFormData("curriculum", fileName, requestFile)
        } catch (e: Exception) {
            null
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        result = it.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(findViewById(R.id.scrollRegisterStudent), message, Snackbar.LENGTH_LONG).show()
    }
}
