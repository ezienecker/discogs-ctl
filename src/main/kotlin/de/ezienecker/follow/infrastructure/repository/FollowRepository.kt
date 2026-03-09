package de.ezienecker.follow.infrastructure.repository

import de.ezienecker.follow.service.FollowDto
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

private val logger = KotlinLogging.logger {}

class FollowRepository {

    fun listByTag(tag: String?) = transaction {
        Follows.selectAll()
            .map { row ->
                FollowDto(
                    username = row[Follows.username],
                    tags = row[Follows.tags].split(", ").map { it.trim() },
                )
            }
            .also { result ->
                logger.trace {
                    "Retrieved [${result.size}] follows ${
                        tag?.let { "filtered by tag: [$it]" } ?: "."
                    }"
                }
            }
    }

    fun add(username: String, tags: List<String>) {
        transaction {
            Follows
                .insert {
                    it[Follows.username] = username
                    it[Follows.tags] = tags.joinToString(", ")
                }.also {
                    logger.debug { "Added follow for username: [$username] with tags: $tags." }
                }
        }
    }

    fun remove(username: String) {
        transaction {
            Follows
                .deleteWhere { this.username eq username }
                .also { deletedCount ->
                    logger.debug { "Removed [$deletedCount] follow(s) for username: [$username]." }
                }
        }
    }
}
