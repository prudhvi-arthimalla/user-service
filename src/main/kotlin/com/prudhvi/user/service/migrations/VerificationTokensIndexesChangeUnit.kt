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

@ChangeUnit(id = "verificationTokensIndex", order = "001")
class VerificationTokensIndexesChangeUnit {

    @Execution
    fun changeSet(db: MongoDatabase) {
        val verificationTokens = db.getCollection("verification_tokens")

        createIndex(
            verificationTokens,
            Indexes.ascending("token"),
            IndexOptions().name("verification_tokens.unique_token").unique(true)
        )

        createIndex(
            verificationTokens,
            Indexes.ascending("expiresAt"),
            IndexOptions().name("verification_tokens.ttl_expiresAt").expireAfter(0, TimeUnit.SECONDS),
        )
    }

    private fun createIndex(collection: MongoCollection<Document>, keys: Bson, options: IndexOptions) {
        Mono.from(collection.createIndex(keys, options)).block()
    }
}