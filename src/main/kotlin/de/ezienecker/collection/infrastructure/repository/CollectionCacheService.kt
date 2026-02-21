@file:OptIn(ExperimentalTime::class)

package de.ezienecker.collection.infrastructure.repository

import de.ezienecker.core.infrastructure.discogs.collection.BasicInformation
import de.ezienecker.core.infrastructure.discogs.collection.Release
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime

private val logger = KotlinLogging.logger {}

class CollectionCacheService(val clock: Clock.System, val json: Json) {

    /**
     * Check if collection data exists in cache and is not expired
     */
    @OptIn(ExperimentalTime::class)
    fun hasValidCache(username: String): Boolean = transaction {
        val cutoffTime = clock.now() - CACHE_EXPIRY_DURATION
        CachedCollections.select(CachedCollections.id).where {
            (CachedCollections.username eq username) and
                    (CachedCollections.cachedAt greater cutoffTime)
        }.count() > 0
    }

    /**
     * Get cached collection data for a user
     */
    fun getCached(username: String): List<Release> = transaction {
        logger.debug { "Retrieving cached collection for user: [$username]." }

        CachedCollections.selectAll().where {
            CachedCollections.username eq username
        }.map { row ->
            val basicInfo = json.decodeFromString<BasicInformation>(row[CachedCollections.basicInformation])
            Release(
                id = row[CachedCollections.releaseId],
                instanceId = row[CachedCollections.instanceId],
                dateAdded = row[CachedCollections.dateAdded],
                rating = row[CachedCollections.rating],
                basicInformation = basicInfo
            )
        }.also {
            logger.info { "Retrieved ${it.size} cached releases for user: [$username]." }
        }
    }

    /**
     * Cache collection data for a user
     */
    fun cache(username: String, releases: List<Release>) = transaction {
        logger.debug { "Caching ${releases.size} releases for user: [$username]." }

        clearCache(username)

        val now = clock.now()

        releases
            .distinctBy { it.id to it.instanceId }
            .forEach { release ->
            CachedCollections.insert {
                it[this.username] = username
                it[releaseId] = release.id
                it[instanceId] = release.instanceId
                it[dateAdded] = release.dateAdded
                it[rating] = release.rating
                it[basicInformation] = json.encodeToString(release.basicInformation)
                it[cachedAt] = now
            }
        }

        logger.info { "Successfully cached ${releases.size} releases for user: [$username]." }
    }

    /**
     * Clear cache data for a specific user
     */
    fun clearCache(username: String) = transaction {
        logger.info { "Clearing collection cache for user: [$username]." }
        CachedCollections.deleteWhere { CachedCollections.username eq username }
        logger.info { "Successfully cleared collection cache for user: [$username]." }
    }

    /**
     * Clear expired cache entries
     */
    fun clearExpiredCache() = transaction {
        val cutoffTime = clock.now() - CACHE_EXPIRY_DURATION
        val deletedCount = CachedCollections.deleteWhere {
            CachedCollections.cachedAt less cutoffTime
        }
        logger.info { "Cleared $deletedCount expired collection cache entries." }
    }

    companion object {
        private val CACHE_EXPIRY_DURATION = 24.hours
    }
}
