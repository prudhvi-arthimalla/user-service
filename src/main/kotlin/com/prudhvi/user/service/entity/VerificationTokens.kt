package com.prudhvi.user.service.entity

import com.prudhvi.user.service.utils.VerificationType
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("verification_tokens")
data class VerificationTokens(

    val userId: String,

    val type: VerificationType,

    @Indexed(unique = true)
    val token: String,

    val expiresAt: Instant

)
