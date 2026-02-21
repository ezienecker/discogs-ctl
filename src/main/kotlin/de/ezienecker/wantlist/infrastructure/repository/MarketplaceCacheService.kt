@file:OptIn(ExperimentalTime::class)

package de.ezienecker.wantlist.infrastructure.repository

import de.ezienecker.core.infrastructure.discogs.marketplace.MarketplaceListing
import de.ezienecker.core.infrastructure.discogs.marketplace.MarketplaceSeller
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

class MarketplaceCacheService(val clock: Clock, val json: Json) {

    fun hasValidCache(releaseId: Long): Boolean = transaction {
        val cutoffTime = clock.now() - CACHE_EXPIRY_DURATION
        MarketplaceListings.select(MarketplaceListings.id).where {
            (MarketplaceListings.releaseId eq releaseId) and
                    (MarketplaceListings.cachedAt greater cutoffTime)
        }.count() > 0
    }

    fun getCached(releaseId: Long): List<MarketplaceListing> = transaction {
        logger.debug { "Retrieving cached marketplace listings for release with ID: [$releaseId]" }

        MarketplaceListings.selectAll().where {
            MarketplaceListings.releaseId eq releaseId
        }.map { row ->

            MarketplaceListing(
                releaseId = row[MarketplaceListings.releaseId],
                title = row[MarketplaceListings.title],
                resourceUrl = row[MarketplaceListings.resourceUrl],
                mediaCondition = row[MarketplaceListings.mediaCondition],
                sleeveCondition = row[MarketplaceListings.sleeveCondition],
                price = row[MarketplaceListings.priceWithCurrency],
                seller = MarketplaceSeller(row[MarketplaceListings.seller]),
                shippingLocation = row[MarketplaceListings.shippingLocation],
            )
        }.also {
            logger.info { "Retrieved ${it.size} cached marketplace listings for release with ID: [$releaseId]" }
        }
    }

    fun cache(releaseId: Long, marketplaceListings: List<MarketplaceListing>) = transaction {
        logger.debug { "Caching ${marketplaceListings.size} marketplace listings for release with ID: [$releaseId]" }

        clearCache(releaseId)

        val now = clock.now()

        marketplaceListings
            .distinctBy { Triple(it.releaseId, it.seller, it.price) }
            .forEach { marketplaceListing ->
                MarketplaceListings.insert {
                    it[this.releaseId] = releaseId
                    it[title] = marketplaceListing.title
                    it[resourceUrl] = marketplaceListing.resourceUrl
                    it[mediaCondition] = marketplaceListing.mediaCondition
                    it[sleeveCondition] = marketplaceListing.sleeveCondition
                    it[priceWithCurrency] = marketplaceListing.price
                    it[seller] = marketplaceListing.seller.name
                    it[shippingLocation] = marketplaceListing.shippingLocation
                    it[cachedAt] = now
                }
            }

        logger.info { "Successfully cached ${marketplaceListings.size} listings for release with ID: [$releaseId]" }
    }

    fun clearCache(releaseId: Long) = transaction {
        logger.info { "Clearing marketplace listings for release with ID: [$releaseId]" }
        MarketplaceListings.deleteWhere { MarketplaceListings.releaseId eq releaseId }
        logger.info { "Successfully cleared marketplace listings for release with ID: [$releaseId]" }
    }

    fun clearExpiredCache() = transaction {
        val cutoffTime = clock.now() - CACHE_EXPIRY_DURATION
        MarketplaceListings.deleteWhere {
            MarketplaceListings.cachedAt less cutoffTime
        }.also {
            logger.info { "Cleared [$it] expired marketplace listings cache entries" }
        }
    }

    companion object {
        private val CACHE_EXPIRY_DURATION = 24.hours
    }
}
