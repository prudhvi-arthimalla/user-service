package com.prudhvi.user.service.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class RegisterUserRequest (

    @field:Email
    @field:NotBlank
    @field:Size(min = 5, message = "email should be a minimum of 5 characters")
    val email: String,

    @field:NotBlank
    @field:Size(min = 8, max = 14, message = "password should be a minimum of 8 characters and a maximum of 14 characters")
    val password: String,

    @field:Size(min = 5, max = 14, message = "username should be a minimum of 5 characters and maximum of 14 characters")
    val username: String? = null,

    @field:Pattern(
        regexp = "^\\+?[1-9]\\d{1,14}$",
        message = "Phone must be in E.164 format (e.g., +14155552671)"
    )
    val phone: String? = null
)
