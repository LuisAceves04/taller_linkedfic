package com.example.linkedininterno.data.model

data class RegisterCompanyRequest(
    val email: String,
    val password: String,
    val companyName: String,
    val industryId: Int,
    val description: String? = null
)
