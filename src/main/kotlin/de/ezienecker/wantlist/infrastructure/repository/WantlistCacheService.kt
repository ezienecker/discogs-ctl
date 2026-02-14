package de.ezienecker.wantlist.infrastructure.repository

import de.ezienecker.core.infrastructure.discogs.wantlist.BasicInformation
import de.ezienecker.core.infrastructure.discogs.wantlist.Url
import de.ezienecker.core.infrastructure.discogs.wantlist.Want
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.time.Duration.Companion.hours

private val logger = KotlinLogging.logger {}

class WantlistCacheService(val clock: Clock, val json: Json) {

    /**
     * Check if wantlist data exists in cache and is not expired
     */
    fun hasValidCache(username: String): Boolean = transaction {
        val cutoffTime = clock.now() - CACHE_EXPIRY_DURATION
        Wants.select(Wants.id).where {
            (Wants.username eq username) and
                    (Wants.cachedAt greater cutoffTime)
        }.count() > 0
    }

    /**
     * Get cached wantlist data for a user
     */
    fun getCached(username: String): List<Want> = transaction {
        logger.debug { "Retrieving cached wantlist for user: [$username]" }

        val cachedWants = Wants.selectAll().where {
            Wants.username eq username
        }.map { row ->
            val basicInfo = json.decodeFromString<BasicInformation>(row[Wants.basicInformation])
            Want(
                id = row[Wants.wantId],
                rating = row[Wants.rating],
                resourceUrl = Url(row[Wants.resourceUrl]),
                basicInformation = basicInfo
            )
        }

        logger.info { "Retrieved ${cachedWants.size} cached wants for user: [$username]" }
        cachedWants
    }

    /**
     * Cache wantlist data for a user
     */
    fun cache(username: String, wants: List<Want>) = transaction {
        logger.debug { "Caching ${wants.size} wants for user: [$username]" }

        clearCache(username)

        val now = clock.now()

        wants
            .distinctBy { it.id }
            .forEach { want ->
                Wants.insert {
                    it[this.username] = username
                    it[wantId] = want.id
                    it[rating] = want.rating
                    it[resourceUrl] = want.resourceUrl.value
                    it[basicInformation] = json.encodeToString(want.basicInformation)
                    it[cachedAt] = now
                }
            }

        logger.info { "Successfully cached ${wants.size} wants for user: [$username]" }
    }

    /**
     * Clear cache data for a specific user
     */
    fun clearCache(username: String) = transaction {
        logger.info { "Clearing wantlist cache for user: [$username]" }
        Wants.deleteWhere { Wants.username eq username }
        logger.info { "Successfully cleared wantlist cache for user: [$username]" }
    }

    /**
     * Clear expired cache entries
     */
    fun clearExpiredCache() = transaction {
        val cutoffTime = clock.now() - CACHE_EXPIRY_DURATION
        val deletedCount = Wants.deleteWhere {
            Wants.cachedAt less cutoffTime
        }
        logger.info { "Cleared $deletedCount expired wantlist cache entries" }
    }

    companion object {
        private val CACHE_EXPIRY_DURATION = 24.hours
    }
}
