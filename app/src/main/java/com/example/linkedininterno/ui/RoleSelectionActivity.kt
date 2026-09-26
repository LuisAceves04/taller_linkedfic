package com.example.linkedininterno.ui

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.linkedininterno.R
import com.google.android.material.card.MaterialCardView

class RoleSelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_role_selection)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.roleSelection)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val cardStudent = findViewById<MaterialCardView>(R.id.cardStudent)
        val cardCompany = findViewById<MaterialCardView>(R.id.cardCompany)
        val tvBackToLogin = findViewById<TextView>(R.id.tvBackToLogin)

        cardStudent.setOnClickListener {
            val intent = Intent(this, RegisterStudentActivity::class.java)
            startActivity(intent)
        }

        cardCompany.setOnClickListener {
            val intent = Intent(this, RegisterCompanyActivity::class.java)
            startActivity(intent)
        }

        tvBackToLogin.setOnClickListener {
            finish()
        }
    }
}
