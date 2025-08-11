package com.prudhvi.user.service.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("profiles")
data class Profiles(

    @Id val userId: String,

    val firstName: String,

    val lastName: String,

    val avatarUrl: String,

    val addresses: List<String>,

    val preferences: Map<String, String>
)
