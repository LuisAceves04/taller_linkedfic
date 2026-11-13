package com.example.linkedininterno

data class Modalidad(
    val id: Int,
    val nombre: String
) {
    override fun toString(): String {
        return nombre
    }
}