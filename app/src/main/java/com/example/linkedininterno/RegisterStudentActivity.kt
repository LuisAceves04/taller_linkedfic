package com.example.linkedininterno

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.linkedininterno.network.RetrofitClient
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class RegisterStudentActivity : AppCompatActivity() {

    // Variables para almacenar las URIs de los archivos seleccionados
    private var avatarUri: Uri? = null
    private var curriculumUri: Uri? = null

    // Vistas y variables para Carreras y semestres dinámicos
    private lateinit var spinnerCarrera: Spinner
    private lateinit var etSemester: EditText
    private var listaCarreras: List<Carrera> = listOf()
    private var idCarreraSeleccionada: Int = 1
    private var maxSemestresPermitidos: Int = 10 // Valor por defecto

    // Lanzador para seleccionar el Avatar (Imágenes)
    private val pickAvatarLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            avatarUri = uri
            Toast.makeText(this, "Avatar seleccionado ✓", Toast.LENGTH_SHORT).show()
        }
    }

    // Lanzador para seleccionar el Currículum (PDF)
    private val pickCurriculumLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            curriculumUri = uri
            Toast.makeText(this, "Currículum seleccionado ✓", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_student)

        // Vincular vistas con los IDs del layout de register student
        val etFirstName = findViewById<EditText>(R.id.etStudentFirstName)
        val etPaternalSurname = findViewById<EditText>(R.id.etStudentPaternalSurname)
        val etMaternalSurname = findViewById<EditText>(R.id.etStudentMaternalSurname)
        val etEmail = findViewById<EditText>(R.id.etStudentEmail)
        val etPassword = findViewById<EditText>(R.id.etStudentPassword)
        val etPhone = findViewById<EditText>(R.id.etStudentPhone)
        val etAboutMe = findViewById<EditText>(R.id.etStudentAboutMe)
        etSemester = findViewById(R.id.etStudentSemester)
        spinnerCarrera = findViewById(R.id.spMajors)

        val btnSelectAvatar = findViewById<Button>(R.id.btnSelectAvatar)
        val btnSelectCurriculum = findViewById<Button>(R.id.btnSelectCv)
        val btnRegister = findViewById<Button>(R.id.btnRegisterStudent)
        val progressBar = findViewById<ProgressBar>(R.id.progressBarStudent)

        //parte de texto que regresa al layout de login
        val tvGoToLogin = findViewById<TextView>(R.id.tvBackToLoginFromStudent)
        tvGoToLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Cargar las carreras de la base de datos al iniciar la pantalla
        cargarCarreras()

        // abrir los selectores de archivos
        btnSelectAvatar.setOnClickListener {
            pickAvatarLauncher.launch("image/*")
        }

        btnSelectCurriculum.setOnClickListener {
            // Permitimos archivos PDF
            pickCurriculumLauncher.launch("application/pdf")
        }

        // Botón principal de registro
        btnRegister.setOnClickListener {
            val firstName = etFirstName.text.toString().trim()
            val paternalSurname = etPaternalSurname.text.toString().trim()
            val maternalSurname = etMaternalSurname.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val aboutMe = etAboutMe.text.toString().trim()
            val semesterStr = etSemester.text.toString().trim()

            // Validaciones básicas de campos obligatorios
            if (firstName.isEmpty() || paternalSurname.isEmpty() || maternalSurname.isEmpty() ||
                email.isEmpty() || password.isEmpty() || phone.isEmpty() || semesterStr.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos obligatorios", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Validar que el semestre ingresado sea válido y no rebase el límite de la carrera seleccionada
            val semestreIngresado = semesterStr.toIntOrNull() ?: 0
            if (semestreIngresado <= 0) {
                Toast.makeText(this, "El semestre debe ser mayor a 0", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (semestreIngresado > maxSemestresPermitidos) {
                Toast.makeText(
                    this,
                    "La carrera seleccionada solo tiene un máximo de $maxSemestresPermitidos semestres",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            if (phone.length > 10) {
                Toast.makeText(this, "El teléfono no puede exceder los 10 dígitos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validar que el currículum esté seleccionado
            if (curriculumUri == null) {
                Toast.makeText(this, "El currículum en PDF es obligatorio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnRegister.isEnabled = false

            // Convertir textos a RequestBody para Multipart
            val bodyIdRol = "2".toRequestBody("text/plain".toMediaTypeOrNull()) // 2 = Rol Alumno
            val bodyEmail = email.toRequestBody("text/plain".toMediaTypeOrNull())
            val bodyPassword = password.toRequestBody("text/plain".toMediaTypeOrNull())
            val bodyPhone = phone.toRequestBody("text/plain".toMediaTypeOrNull())
            val bodyAboutMe = aboutMe.toRequestBody("text/plain".toMediaTypeOrNull())
            val bodyFirstName = firstName.toRequestBody("text/plain".toMediaTypeOrNull())
            val bodyPaternal = paternalSurname.toRequestBody("text/plain".toMediaTypeOrNull())
            val bodyMaternal = maternalSurname.toRequestBody("text/plain".toMediaTypeOrNull())
            val bodyCarrera = idCarreraSeleccionada.toString().toRequestBody("text/plain".toMediaTypeOrNull()) // ID dinámico
            val bodySemester = semesterStr.toRequestBody("text/plain".toMediaTypeOrNull())

            // Preparar archivo de Currículum (Obligatorio)
            val curriculumFile = getFileFromUri(curriculumUri!!)
            val curriculumRequestBody = curriculumFile.asRequestBody("application/pdf".toMediaTypeOrNull())
            val curriculumPart = MultipartBody.Part.createFormData("curriculum", curriculumFile.name, curriculumRequestBody)

            // Preparar archivo de Avatar (Opcional)
            var avatarPart: MultipartBody.Part? = null
            if (avatarUri != null) {
                val avatarFile = getFileFromUri(avatarUri!!)
                val avatarRequestBody = avatarFile.asRequestBody("image/*".toMediaTypeOrNull())
                avatarPart = MultipartBody.Part.createFormData("avatar", avatarFile.name, avatarRequestBody)
            }

            // Ejecutar la petición con Retrofit
            val call = RetrofitClient.instance.registrarAlumno(
                bodyIdRol, bodyEmail, bodyPassword, bodyPhone, bodyAboutMe,
                bodyFirstName, bodyPaternal, bodyMaternal, bodyCarrera, bodySemester,
                avatarPart, curriculumPart
            )

            call.enqueue(object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    progressBar.visibility = View.GONE
                    btnRegister.isEnabled = true

                    if (response.isSuccessful) {
                        Toast.makeText(this@RegisterStudentActivity, "¡Registro de alumno exitoso!", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        Toast.makeText(this@RegisterStudentActivity, "Error al registrar. Revisa los datos.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    btnRegister.isEnabled = true
                    Toast.makeText(this@RegisterStudentActivity, "Error de conexión: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
        }
    }

    // Función para obtener las carreras desde la API y llenar el Spinner
    private fun cargarCarreras() {
        RetrofitClient.instance.obtenerCarreras().enqueue(object : Callback<RespuestaCarreras> {
            override fun onResponse(call: Call<RespuestaCarreras>, response: Response<RespuestaCarreras>) {
                if (response.isSuccessful && response.body() != null) {
                    val respuesta = response.body()!!
                    if (respuesta.success) {
                        listaCarreras = respuesta.carreras
                        Log.d("API_CARRERAS", "¡Éxito! Carreras recibidas: ${listaCarreras.size}")

                        // Mapear solo los nombres de las carreras para mostrarlas en el menú desplegable
                        val nombresCarreras = listaCarreras.map { it.carrera }

                        val adapter = ArrayAdapter(
                            this@RegisterStudentActivity,
                            android.R.layout.simple_spinner_item,
                            nombresCarreras
                        )
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        spinnerCarrera.adapter = adapter
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Sin detalles"
                    Log.e("API_CARRERAS", "Error HTTP ${response.code()}: $errorBody")
                }
            }

            override fun onFailure(call: Call<RespuestaCarreras>, t: Throwable) {
                Log.e("API_CARRERAS", "Fallo total de red o conversión: ${t.message}")
                Toast.makeText(this@RegisterStudentActivity, "No se pudieron cargar las carreras", Toast.LENGTH_SHORT).show()
            }
        })

        // Capturar la opción seleccionada del Spinner y actualizar el límite de semestres
        spinnerCarrera.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (listaCarreras.isNotEmpty()) {
                    val carreraSeleccionada = listaCarreras[position]
                    idCarreraSeleccionada = carreraSeleccionada.idCarrera
                    maxSemestresPermitidos = carreraSeleccionada.semestres

                    // Muestra en la pista del campo cuántos semestres son el límite
                    etSemester.hint = "Semestre (Máx. $maxSemestresPermitidos)"
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    // Función para transformar un Uri de Android en un archivo físico temporal para Retrofit
    private fun getFileFromUri(uri: Uri): File {
        val file = File(cacheDir, "temp_file_${System.currentTimeMillis()}.pdf")
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(file)
            if (inputStream != null) {
                inputStream.copyTo(outputStream)
                inputStream.close()
            }
            outputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return file
    }
}