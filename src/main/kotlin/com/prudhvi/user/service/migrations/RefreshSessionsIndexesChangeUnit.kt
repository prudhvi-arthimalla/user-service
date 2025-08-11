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
import java.util.concurrent.TimeUnit

@ChangeUnit(id = "refreshSessionsIndex", order = "001")
class RefreshSessionsIndexesChangeUnit {
    @Execution
    fun changeSet(db: MongoDatabase) {
        val refreshSessions = db.getCollection("refresh_sessions")

        createIndex(
            refreshSessions,
            Indexes.ascending("refreshTokenHash"),
            IndexOptions().name("refresh_sessions.unique_refreshTokenHash").unique(true)
        )

        createIndex(
            refreshSessions,
            Indexes.ascending("expiresAt"),
            IndexOptions().name("refresh_sessions.ttl_expiresAt").expireAfter(0, TimeUnit.SECONDS),
        )

        createIndex(
            refreshSessions,
            Indexes.compoundIndex(Indexes.ascending("userId"), Indexes.descending("createdAt")),
            IndexOptions().name("refresh_sessions.user_createdAt_idx")
        )

        createIndex(
            refreshSessions,
            Indexes.compoundIndex(Indexes.ascending("userID"), Indexes.ascending("expiresAt")),
            IndexOptions().name("refresh_sessions.active_sessions_idx").partialFilterExpression(Document("revokedAt", null))
        )
    }
    private fun createIndex(collection: MongoCollection<Document>, keys: Bson, options: IndexOptions) {
        Mono.from(collection.createIndex(keys, options)).block()
    }
}