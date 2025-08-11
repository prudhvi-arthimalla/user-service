package com.prudhvi.user.service.repositories

import com.prudhvi.user.service.entity.Users
import com.prudhvi.user.service.entity.VerificationTokens
import com.prudhvi.user.service.utils.VerificationType
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Mono

interface VerificationTokensRepository: ReactiveMongoRepository<VerificationTokens, String> {
    fun findByTokenAndType(token: String, type: VerificationType): Mono<VerificationTokens>
    fun deleteAllByUserIdAndType(userId: String, type: VerificationType): Mono<Void>
}