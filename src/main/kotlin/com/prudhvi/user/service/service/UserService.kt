package com.prudhvi.user.service.service

import com.prudhvi.user.service.dto.RegisterUserRequest
import com.prudhvi.user.service.entity.AuditLogs
import com.prudhvi.user.service.entity.Credentials
import com.prudhvi.user.service.entity.Users
import com.prudhvi.user.service.entity.VerificationTokens
import com.prudhvi.user.service.repositories.AuditLogsRepository
import com.prudhvi.user.service.repositories.CredentialsRepository
import com.prudhvi.user.service.repositories.UsersRepository
import com.prudhvi.user.service.repositories.VerificationTokensRepository
import com.prudhvi.user.service.utils.UserActions
import com.prudhvi.user.service.utils.UserStatus
import com.prudhvi.user.service.utils.VerificationType
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.UUID

@Service
class UserService(
    val usersRepository: UsersRepository,
    val credentialsRepository: CredentialsRepository,
    val auditLogsRepository: AuditLogsRepository,
    val passwordEncoder: PasswordEncoder,
    val verificationTokensRepository: VerificationTokensRepository
) {

    fun registerUser(user: RegisterUserRequest): Mono<Void> {
        val normalizedEmail = user.email.trim().lowercase()
        val normalizedPhone = user.phone?.trim()

        return usersRepository.findByEmail(normalizedEmail)
            .flatMap { existing ->
                when (existing.status) {
                    UserStatus.PENDING -> {
                        // idempotent accept: delete verification tokens and create new verification token, return 200
                        val newToken = VerificationTokens(
                            userId = existing.userId!!,
                            type = VerificationType.EMAIL_VERIFY,
                            token = generateToken(),
                            expiresAt = Instant.now().plus(24, ChronoUnit.HOURS)
                        )
                        verificationTokensRepository.deleteAllByUserIdAndType(
                            existing.userId,
                            VerificationType.EMAIL_VERIFY
                        )
                            .then(verificationTokensRepository.save(newToken))
                            .then(
                                auditLogsRepository.save(
                                    AuditLogs(
                                        actorId = existing.userId,
                                        targetId = existing.userId,
                                        action = UserActions.USER_REGISTER
                                    )
                                )
                            )
                            .then()
                    }

                    else -> {
                        // Email already registered & not pending → 409 Conflict
                        Mono.error(ResponseStatusException(HttpStatus.CONFLICT, "Email already registered"))
                    }
                }
            }
            .switchIfEmpty(
                // Create a brand-new user
                usersRepository.save(
                    Users(
                        email = normalizedEmail,
                        userName = user.username ?: generateDefaultUsername(normalizedEmail),
                        phone = normalizedPhone,
                        status = UserStatus.PENDING
                    )
                ).flatMap { savedUser ->
                    val credentials = Credentials(
                        userId = savedUser.userId!!,
                        passwordHash = passwordEncoder.encode(user.password)
                    )
                    val token = VerificationTokens(
                        userId = savedUser.userId,
                        type = VerificationType.EMAIL_VERIFY,
                        token = generateToken(),
                        expiresAt = Instant.now().plus(2, ChronoUnit.HOURS)
                    )

                    credentialsRepository.save(credentials)
                        .then(verificationTokensRepository.save(token))
                        .then(
                            auditLogsRepository.save(
                                AuditLogs(
                                    actorId = savedUser.userId,
                                    targetId = savedUser.userId,
                                    action = UserActions.USER_REGISTER
                                )
                            )
                        )
                        .then()
                }
            )
    }

    fun verifyEmail(token: String): Mono<Void> {
        if (token.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Token should not be empty")
        }

        val urlSafePattern = Regex("^[A-Za-z0-9_-]+$")
        if (!urlSafePattern.matches(token)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token format")
        }

        if (token.length != 43) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token length")
        }

        val gone = Mono.error<Void>(ResponseStatusException(HttpStatus.GONE, "Token not found or Token Invalid"))
        return verificationTokensRepository.findByTokenAndType(token, VerificationType.EMAIL_VERIFY)
            .flatMap { existingToken ->
                if (Instant.now() > existingToken.expiresAt) {
                    gone
                } else {
                    usersRepository.findById(existingToken.userId)
                        .flatMap { user ->
                            when (user.status) {
                                UserStatus.ACTIVE -> {
                                    // idempotent accept: delete verification tokens and return 200
                                    verificationTokensRepository.deleteAllByUserIdAndType(
                                        existingToken.userId,
                                        VerificationType.EMAIL_VERIFY
                                    ).then()
                                }

                                UserStatus.PENDING -> {
                                    user.status = UserStatus.ACTIVE
                                    user.emailVerifiedAt = Instant.now()
                                    usersRepository.save(user)
                                        .then(
                                            auditLogsRepository.save(
                                                AuditLogs(
                                                    actorId = existingToken.userId,
                                                    targetId = existingToken.userId,
                                                    action = UserActions.USER_VERIFICATION
                                                )
                                            )
                                        )
                                        .then(
                                            verificationTokensRepository.deleteAllByUserIdAndType(
                                                existingToken.userId,
                                                VerificationType.EMAIL_VERIFY
                                            )
                                        )
                                        .then()
                                }

                                else -> {
                                    Mono.error(ResponseStatusException(HttpStatus.FORBIDDEN, "Account is locked"))
                                }
                            }
                        }
                        .switchIfEmpty(gone)
                }
            }
            .switchIfEmpty(gone)
    }

    private fun generateDefaultUsername(email: String): String {
        return "${email.substringBefore("@")}-${UUID.randomUUID()}"
    }

    private fun generateToken(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

}