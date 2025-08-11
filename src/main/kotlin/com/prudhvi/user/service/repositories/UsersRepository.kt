package com.prudhvi.user.service.repositories

import com.prudhvi.user.service.utils.UserStatus

import com.prudhvi.user.service.entity.Users
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Mono

interface UsersRepository: ReactiveMongoRepository<Users, String> {
    fun findByEmail(email: String): Mono<Users>
    fun existsByEmail(email: String): Mono<Boolean>
    fun existsByUserName(userName: String): Mono<Boolean>
    fun findByEmailAndStatus(email: String, status: UserStatus): Mono<Users>
}