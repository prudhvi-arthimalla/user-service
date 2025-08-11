package com.prudhvi.user.service.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing

@Configuration
@EnableReactiveMongoAuditing
class MongoAuditingConfig {
}