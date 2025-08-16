package com.prudhvi.user.service.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class LoginRequest(

    @field:Email
    @field:NotBlank
    @field:Size(min = 5, message = "email should be a minimum of 5 characters")
    val email: String,

    @field:NotBlank
    @field:Size(min = 8, max = 14, message = "password should be a minimum of 8 characters and a maximum of 14 characters")
    val password: String,
)
