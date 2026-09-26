package com.example.linkedininterno.data.model

data class MajorDto(
    val id: Int,
    val name: String
) {
    override fun toString(): String = name
}
