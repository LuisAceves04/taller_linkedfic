package com.example.linkedininterno.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.linkedininterno.R
import com.example.linkedininterno.data.local.SessionManager

open class MainActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sessionManager = SessionManager(this)

        val tvWelcomeUser = findViewById<TextView>(R.id.tvWelcomeUser)
        val tvUserEmail = findViewById<TextView>(R.id.tvUserEmail)
        val tvUserRole = findViewById<TextView>(R.id.tvUserRole)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        val user = sessionManager.fetchUser()
        if (user != null) {
            val displayName = user.name ?: user.email
            tvWelcomeUser.text = "¡Bienvenido, $displayName!"
            tvUserEmail.text = "Email: ${user.email}"
            tvUserRole.text = "Rol: ${user.roleName}"
        } else {
            tvWelcomeUser.text = "¡Bienvenido a Linkedfic!"
        }

        btnLogout.setOnClickListener {
            sessionManager.clearSession()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
