package com.example.model
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val login: String,
    val password: String,
    val fullName: String,
    val age: Int,
)