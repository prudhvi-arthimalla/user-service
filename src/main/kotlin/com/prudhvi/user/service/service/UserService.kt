package com.prudhvi.user.service.service

import com.prudhvi.user.service.dto.LoginRequest
import com.prudhvi.user.service.dto.LoginResponse
import com.prudhvi.user.service.dto.RegisterUserRequest
import com.prudhvi.user.service.entity.AuditLogs
import com.prudhvi.user.service.entity.Credentials
import com.prudhvi.user.service.entity.RefreshSessions
import com.prudhvi.user.service.entity.Users
import com.prudhvi.user.service.entity.VerificationTokens
import com.prudhvi.user.service.repositories.AuditLogsRepository
import com.prudhvi.user.service.repositories.CredentialsRepository
import com.prudhvi.user.service.repositories.RefreshSessionsRepository
import com.prudhvi.user.service.repositories.UsersRepository
import com.prudhvi.user.service.repositories.VerificationTokensRepository
import com.prudhvi.user.service.utils.UserActions
import com.prudhvi.user.service.utils.UserStatus
import com.prudhvi.user.service.utils.VerificationType
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.security.Key
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.Date
import java.util.UUID

@Service
class UserService(
    val usersRepository: UsersRepository,
    val credentialsRepository: CredentialsRepository,
    val auditLogsRepository: AuditLogsRepository,
    val passwordEncoder: PasswordEncoder,
    val verificationTokensRepository: VerificationTokensRepository,
    val refreshSessionsRepository: RefreshSessionsRepository
) {
    private val key: Key by lazy {
        val secret = System.getenv("JWT_SECRET")
            ?: "dev-secret-change-me-dev-secret-change-me-123456" // >=32 bytes for HS256
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

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
                        ).then(verificationTokensRepository.save(newToken)).then(
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
                if (Instant.now().isAfter(existingToken.expiresAt)) {
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

    fun login(loginRequest: LoginRequest): Mono<LoginResponse> {
        val normalizedEmail = loginRequest.email.trim().lowercase()
        val dummyHash = "\$2a\$10\$CwTycUXWue0Thq9StjUM0uJ8dPp6ZbM5o6CZcHnyGQdDgkZ8eZ4y6"
        val now = Instant.now()

        return usersRepository.findByEmail(normalizedEmail)
            .switchIfEmpty(
                // Equalize timing when user is not found
                Mono.defer {
                    passwordEncoder.matches(loginRequest.password, dummyHash)
                    Mono.error<Users>(ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"))
                }
            )
            .flatMap { user ->
                // Account status and temporary lock checks
                val isTempLocked = user.temporaryLockedUntil != null && now.isBefore(user.temporaryLockedUntil)
                if (user.status == UserStatus.LOCKED || isTempLocked) {
                    return@flatMap Mono.error<LoginResponse>(
                        ResponseStatusException(HttpStatus.LOCKED, "Account locked")
                    )
                }
                if (user.status == UserStatus.PENDING) {
                    return@flatMap Mono.error<LoginResponse>(
                        ResponseStatusException(HttpStatus.CONFLICT, "Account verification pending")
                    )
                }

                credentialsRepository.findById(user.userId!!)
                    .switchIfEmpty(
                        Mono.error(ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"))
                    )
                    .flatMap { credentials ->
                        val passwordOk = passwordEncoder.matches(loginRequest.password, credentials.passwordHash)
                        if (!passwordOk) {
                            // Failed attempt tracking & potential lock
                            val newCount = (user.failedLoginCount ?: 0) + 1
                            user.failedLoginCount = newCount
                            user.lastLoginFailed = now
                            var justLocked = false
                            if (newCount >= 5) {
                                user.temporaryLockedUntil = now.plus(30, ChronoUnit.MINUTES)
                                justLocked = true
                            }

                            val auditMono = if (justLocked) {
                                auditLogsRepository.save(
                                    AuditLogs(
                                        actorId = user.userId,
                                        action = UserActions.ACCOUNT_LOCKED,
                                        targetId = user.userId
                                    )
                                ).then()
                            } else Mono.empty()

                            auditMono
                                .then(usersRepository.save(user))
                                .then(
                                    if (justLocked) {
                                        Mono.error<LoginResponse>(ResponseStatusException(HttpStatus.LOCKED, "Too many failed attempts"))
                                    } else {
                                        Mono.error<LoginResponse>(ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"))
                                    }
                                )
                        } else {
                            // Success: reset counters, update last login, create tokens
                            user.failedLoginCount = 0
                            user.temporaryLockedUntil = null
                            user.lastLoggedInAt = now

                            val accessToken = Jwts.builder()
                                .setSubject(user.userId.toString())
                                .setIssuer("user-service")
                                .setAudience("your-client-id")
                                .setIssuedAt(Date.from(now))
                                .setExpiration(Date.from(now.plus(15, ChronoUnit.MINUTES)))
                                .claim("email", user.email)
                                .claim("roles", listOf("USER"))
                                .signWith(key, SignatureAlgorithm.HS256)
                                .compact()
                            val accessExp = now.plus(15, ChronoUnit.MINUTES)

                            val rawRefreshToken = generateToken()
                            val storedRefreshHash = passwordEncoder.encode(rawRefreshToken)
                            val refreshExp = now.plus(90, ChronoUnit.DAYS)

                            val refreshSession = RefreshSessions(
                                userId = user.userId,
                                refreshTokenHash = storedRefreshHash,
                                createdAt = now,
                                expiresAt = refreshExp
                            )

                            refreshSessionsRepository.save(refreshSession)
                                .then(usersRepository.save(user))
                                .then(
                                    auditLogsRepository.save(AuditLogs(
                                        actorId = user.userId,
                                        action = UserActions.LOGIN_SUCCESS,
                                        targetId = user.userId
                                    ))
                                )
                                .then(
                                    Mono.just(
                                        LoginResponse(
                                            accessToken = accessToken,
                                            accessTokenExpiresAt = accessExp,
                                            refreshToken = rawRefreshToken,
                                            refreshTokenExpiresAt = refreshExp
                                        )
                                    )
                                )

                        }
                    }
            }
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