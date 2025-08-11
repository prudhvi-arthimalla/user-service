package com.prudhvi.user.service.entity

import com.prudhvi.user.service.utils.UserActions
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("audit_logs")
data class AuditLogs(

    @Id val id: String? = null,

    val actorId: String,

    val action: UserActions,

    val targetId: String,

    @CreatedDate
    val createdAt: Instant? = null
)
