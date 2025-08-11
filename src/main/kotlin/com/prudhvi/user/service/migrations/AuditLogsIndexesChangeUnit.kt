package com.prudhvi.user.service.migrations

import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.reactivestreams.client.MongoCollection
import com.mongodb.reactivestreams.client.MongoDatabase
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution
import org.bson.Document
import org.bson.conversions.Bson
import reactor.core.publisher.Mono

@ChangeUnit(id = "auditLogsIndex", order = "001")
class AuditLogsIndexesChangeUnit {

    @Execution
    fun changeUnit(db: MongoDatabase) {
        val auditLogs = db.getCollection("audit_logs")

        createIndex(
            auditLogs,
            Indexes.compoundIndex(Indexes.ascending("actorId"), Indexes.descending("createdAt")),
            IndexOptions().name("audit_logs.actor_createdAt_idx")
        )

        createIndex(
            auditLogs,
            Indexes.compoundIndex(Indexes.ascending("targetId"), Indexes.descending("createdAt")),
            IndexOptions().name("audit_logs.target_createdAt_idx")
        )

        createIndex(
            auditLogs,
            Indexes.compoundIndex(Indexes.ascending("action"), Indexes.descending("createdAt")),
            IndexOptions().name("audit_logs.action_createdAt_idx")
        )

    }
    private fun createIndex(collection: MongoCollection<Document>, keys: Bson, options: IndexOptions) {
        Mono.from(collection.createIndex(keys, options)).block()
    }
}