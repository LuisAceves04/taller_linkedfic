package com.example.linkedininterno

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class RegisterTypeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_type)

        val cardAlumno = findViewById<MaterialCardView>(R.id.cardOptionAlumno)
        val cardEmpresa = findViewById<MaterialCardView>(R.id.cardOptionEmpresa)
        val btnVolver = findViewById<Button>(R.id.btnVolverLogin)

        cardAlumno.setOnClickListener {
            val intent = Intent(this, RegisterStudentActivity::class.java)
            startActivity(intent)
        }

        cardEmpresa.setOnClickListener {
            // Aun no eh implementado este activity
            val intent = Intent(this, RegisterEmpresaActivity::class.java)
            startActivity(intent)
        }

        btnVolver.setOnClickListener {
            finish()
        }
    }
}