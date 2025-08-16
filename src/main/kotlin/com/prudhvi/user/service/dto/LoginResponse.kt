package com.prudhvi.user.service.dto

import java.time.Instant

data class LoginResponse(
    val accessToken: String,
    val accessTokenExpiresAt: Instant,
    val refreshToken: String,
    val refreshTokenExpiresAt: Instant
)
