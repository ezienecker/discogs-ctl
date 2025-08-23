package de.ezienecker.shared.database.cache

import de.ezienecker.shared.database.schema.CachedCollections
import de.ezienecker.shared.discogs.collection.BasicInformation
import de.ezienecker.shared.discogs.collection.Release
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

class CollectionCacheService(val clock: Clock, val json: Json) {

    /**
     * Check if collection data exists in cache and is not expired
     */
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

        val cachedReleases = CachedCollections.selectAll().where {
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
        }

        logger.info { "Retrieved ${cachedReleases.size} cached releases for user: [$username]." }
        cachedReleases
    }

    /**
     * Cache collection data for a user
     */
    fun cache(username: String, releases: List<Release>) = transaction {
        logger.debug { "Caching ${releases.size} releases for user: [$username]." }

        // Clear existing cache for this user
        CachedCollections.deleteWhere { CachedCollections.username eq username }

        val now = clock.now()

        releases.forEach { release ->
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
