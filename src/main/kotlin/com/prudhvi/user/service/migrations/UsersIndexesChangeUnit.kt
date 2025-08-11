package com.prudhvi.user.service.migrations

import com.mongodb.reactivestreams.client.MongoDatabase
import io.mongock.api.annotations.ChangeUnit
import io.mongock.api.annotations.Execution

import com.mongodb.client.model.Collation
import com.mongodb.client.model.CollationStrength
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.reactivestreams.client.MongoCollection
import org.bson.Document
import org.bson.conversions.Bson
import reactor.core.publisher.Mono

@ChangeUnit(id = "usersIndex", order = "001")
class UsersIndexesChangeUnit {

    @Execution
    fun changeSet(db: MongoDatabase) {
        val users = db.getCollection("users")
        val ci = Collation.builder()
            .locale("en")
            .caseLevel(false)
            .collationStrength(CollationStrength.SECONDARY)
            .build()

        createIndex(
            users,
            Indexes.ascending("email"),
            IndexOptions().name("users.unique_email_ci").unique(true).collation(ci)
        )
        createIndex(
            users,
            Indexes.ascending("userName"),
            IndexOptions().name("users.unique_userName_ci").unique(true).collation(ci)
        )
        createIndex(
            users,
            Indexes.ascending("status"),
            IndexOptions().name("users.status_idx")
        )
        createIndex(
            users,
            Indexes.compoundIndex(Indexes.ascending("status"), Indexes.descending("createdAt")),
            IndexOptions().name("users.status_createdAt_idx")
        )
    }

    private fun createIndex(collection: MongoCollection<Document>, keys: Bson, options: IndexOptions) {
        Mono.from(collection.createIndex(keys, options)).block()
    }
}