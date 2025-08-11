package com.prudhvi.user.service.entity

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("credentials")
data class Credentials(

    @Id
    val userId: String,

    val passwordHash: String,

    @CreatedDate
    val createdAt: Instant? = null,

    @LastModifiedDate
    val passwordUpdatedAt: Instant? = null,

    val mfaEnabled: Boolean? = null,

    val mfaSecret: String? = null

)
