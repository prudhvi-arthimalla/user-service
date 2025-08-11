package com.prudhvi.user.service.repositories

import com.prudhvi.user.service.entity.AuditLogs
import org.springframework.data.mongodb.repository.ReactiveMongoRepository

interface AuditLogsRepository: ReactiveMongoRepository<AuditLogs, String> {
}