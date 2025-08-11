package com.prudhvi.user.service.repositories

import com.prudhvi.user.service.entity.Profiles
import org.springframework.data.mongodb.repository.ReactiveMongoRepository

interface ProfilesRepository: ReactiveMongoRepository<Profiles, String> {
}