@file:OptIn(ExperimentalTime::class)

package de.ezienecker.wantlist.infrastructure.repository

import de.ezienecker.core.configuration.service.ConfigurationService
import de.ezienecker.core.infrastructure.discogs.wantlist.BasicInformation
import de.ezienecker.core.infrastructure.discogs.wantlist.Url
import de.ezienecker.core.infrastructure.discogs.wantlist.Want
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
import kotlin.time.ExperimentalTime

private val logger = KotlinLogging.logger {}

class WantlistCacheService(
    val clock: Clock,
    val json: Json,
    val configurationService: ConfigurationService,
) {

    /**
     * Check if wantlist data exists in cache and is not expired
     */
    fun hasValidCache(username: String): Boolean = transaction {
        val cutoffTime = clock.now() - getCacheExpiryDuration()
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

        Wants.selectAll().where {
            Wants.username eq username
        }.map { row ->
            val basicInfo = json.decodeFromString<BasicInformation>(row[Wants.basicInformation])
            Want(
                releaseId = row[Wants.wantId],
                rating = row[Wants.rating],
                resourceUrl = Url(row[Wants.resourceUrl]),
                basicInformation = basicInfo
            )
        }.also {
            logger.info { "Retrieved ${it.size} cached wants for user: [$username]" }
        }
    }

    /**
     * Cache wantlist data for a user
     */
    fun cache(username: String, wants: List<Want>) = transaction {
        logger.debug { "Caching ${wants.size} wants for user: [$username]" }

        clearCache(username)

        val now = clock.now()

        wants
            .distinctBy { it.releaseId }
            .forEach { want ->
                Wants.insert {
                    it[this.username] = username
                    it[wantId] = want.releaseId
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
        val cutoffTime = clock.now() - getCacheExpiryDuration()
        val deletedCount = Wants.deleteWhere {
            Wants.cachedAt less cutoffTime
        }
        logger.info { "Cleared $deletedCount expired wantlist cache entries" }
    }

    private fun getCacheExpiryDuration() = configurationService.getWantlistCacheDuration()
}
