package com.prudhvi.user.service.entity

import com.prudhvi.user.service.utils.UserRoles
import com.prudhvi.user.service.utils.UserStatus
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("users")
data class Users(

    @Id
    val userId: String? = null,

    @Indexed(unique = true)
    @field:Email
    @field:NotBlank
    val email: String,

    @Indexed(unique = true)
    @field:NotBlank
    val userName: String,

    @field:Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone must be E.164")
    val phone: String?,

    @Indexed
    var status: UserStatus,

    var temporaryLockedUntil: Instant? = null,

    var failedLoginCount: Int? = null,

    var lastLoginFailed: Instant? =null,

    val roles: Set<UserRoles> = setOf<UserRoles>(UserRoles.USER),

    @CreatedDate
    @Indexed
    val createdAt: Instant? = null,

    var lastLoggedInAt: Instant? = null,

    @LastModifiedDate
    val updatedAt: Instant? = null,

    var emailVerifiedAt: Instant? = null,

    @Version
    val version: Long? = null,

    )
