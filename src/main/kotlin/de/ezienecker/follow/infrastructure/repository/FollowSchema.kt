package de.ezienecker.follow.infrastructure.repository

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object Follows : LongIdTable("follows") {
    val username: Column<String> = varchar("username", 255)
    val tags: Column<String> = varchar("tags", 255)
}
