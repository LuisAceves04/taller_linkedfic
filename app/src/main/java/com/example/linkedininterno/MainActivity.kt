package com.example.linkedininterno

import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        //-----parte de la funcion del ojo de la contraseña para mostrar la contraseña-----
        val etPassword = findViewById<EditText>(R.id.etcontrasena)
        val btnOjoContraseña = findViewById<ImageButton>(R.id.btnOjoContrasena)

        var isPasswordVisible = false
        btnOjoContraseña.setOnClickListener {
            isPasswordVisible = !isPasswordVisible

            if (isPasswordVisible) {
                // Muestra la contraseña en texto plano
                etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
            } else {
                // Oculta la contraseña con puntos
                etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            }

            etPassword.setSelection(etPassword.text.length)
        }
        //-----termina la parte de la funcion del ojo de la contraseña para mostrar la contraseña-----

    }
}