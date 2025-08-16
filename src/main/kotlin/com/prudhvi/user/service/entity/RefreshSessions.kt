package com.prudhvi.user.service.entity

import com.prudhvi.user.service.utils.UserDevice
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "refresh_sessions")
data class RefreshSessions(
    @Id
    val sessionId: String? = null,             // Mongo-generated _id, or UUID if you prefer

    val userId: String,                        // The user who owns this session

    val refreshTokenHash: String,              // Hashed refresh token (never store raw token)

    @CreatedDate
    val createdAt: Instant = Instant.now(),    // When session was created

    val lastUsedAt: Instant? = null,           // Optional: update whenever token is rotated

    val expiresAt: Instant,                    // TTL index for auto-expiry

    val revokedAt: Instant? = null,            // Set when user logs out / session revoked

    val device: UserDevice? = null,            // Structured client info (nullable if unknown)

    val ip: String? = null                     // Captured client IP (nullable if unavailable)
)