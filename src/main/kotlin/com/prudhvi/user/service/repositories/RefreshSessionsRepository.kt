package com.prudhvi.user.service.repositories

import com.prudhvi.user.service.entity.RefreshSessions
import org.springframework.data.mongodb.repository.ReactiveMongoRepository

interface RefreshSessionsRepository: ReactiveMongoRepository<RefreshSessions, String> {
}