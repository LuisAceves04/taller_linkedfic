package com.example.linkedininterno.data.model

data class UserDto(
    val id: Int,
    val email: String,
    val roleId: Int,
    val roleName: String,
    val name: String? = null
)
