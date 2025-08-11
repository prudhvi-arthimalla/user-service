package com.prudhvi.user.service.repositories

import com.prudhvi.user.service.entity.Credentials
import org.springframework.data.mongodb.repository.ReactiveMongoRepository

interface CredentialsRepository: ReactiveMongoRepository<Credentials, String> {
}