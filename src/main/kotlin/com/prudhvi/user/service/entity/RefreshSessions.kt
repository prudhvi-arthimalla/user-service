package com.prudhvi.user.service.entity

import com.prudhvi.user.service.utils.UserDevice
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("refresh_sessions")
data class RefreshSessions(

    @Id
    val sessionId: String,

    val userId: String,

    val device: UserDevice,

    val ip: String,

    val refreshTokenHash: String,

    @CreatedDate
    val createdAt: Instant,

    val expiresAt: Instant,

    val revokedAt: Instant? = null,

    val reason: String? = null
)
