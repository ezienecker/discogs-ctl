package de.ezienecker.shop.infrastructure.repository

import de.ezienecker.core.infrastructure.discogs.marketplace.Comments
import de.ezienecker.core.infrastructure.discogs.marketplace.Condition
import de.ezienecker.core.infrastructure.discogs.marketplace.Currency
import de.ezienecker.core.infrastructure.discogs.marketplace.Listing
import de.ezienecker.core.infrastructure.discogs.marketplace.Price
import de.ezienecker.core.infrastructure.discogs.marketplace.Release
import de.ezienecker.core.infrastructure.discogs.marketplace.Seller
import de.ezienecker.core.infrastructure.discogs.marketplace.Status
import de.ezienecker.core.infrastructure.discogs.marketplace.Uri
import de.ezienecker.core.infrastructure.discogs.marketplace.Url
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

class ShopCacheService(val clock: Clock, val json: Json) {

    /**
     * Check if inventory data exists in cache and is not expired
     */
    fun hasValidCache(username: String): Boolean = transaction {
        val cutoffTime = clock.now() - CACHE_EXPIRY_DURATION
        Listings.select(Listings.id).where {
            (Listings.username eq username) and
                    (Listings.cachedAt greater cutoffTime)
        }.count() > 0
    }

    /**
     * Get cached inventory data for a user
     */
    fun getCached(username: String): List<Listing> = transaction {
        logger.debug { "Retrieving cached inventory for user: [$username]" }

        val cachedListings = Listings.selectAll().where {
            Listings.username eq username
        }.map { row ->
            val seller =
                json.decodeFromString<Seller>(row[Listings.seller])
            val release =
                json.decodeFromString<Release>(row[Listings.release])

            Listing(
                id = row[Listings.listingId],
                resourceUrl = Url(row[Listings.resourceUrl]),
                uri = Uri(row[Listings.uri]),
                status = Status.entries.first { it.name == row[Listings.status] },
                mediaCondition = Condition.entries.first { it.name == row[Listings.mediaCondition] },
                sleeveCondition = Condition.entries.first { it.name == row[Listings.sleeveCondition] },
                comments = Comments(row[Listings.comments]),
                price = Price(
                    value = row[Listings.priceValue],
                    currency = Currency(row[Listings.priceCurrency])
                ),
                seller = seller,
                release = release
            )
        }

        logger.info { "Retrieved ${cachedListings.size} cached listings for user: [$username]" }
        cachedListings
    }

    /**
     * Cache inventory data for a user
     */
    fun cache(username: String, listings: List<Listing>) = transaction {
        logger.debug { "Caching ${listings.size} listings for user: [$username]" }

        clearCache(username)

        val now = clock.now()

        listings
            .distinctBy { it.id }
            .forEach { listing ->
            Listings.insert {
                it[this.username] = username
                it[listingId] = listing.id
                it[resourceUrl] = listing.resourceUrl.value
                it[uri] = listing.uri.value
                it[status] = listing.status.name
                it[mediaCondition] = listing.mediaCondition.name
                it[sleeveCondition] = listing.sleeveCondition.name
                it[comments] = listing.comments.value
                it[priceValue] = listing.price.value
                it[priceCurrency] = listing.price.currency.value
                it[seller] = json.encodeToString(listing.seller)
                it[release] = json.encodeToString(listing.release)
                it[cachedAt] = now
            }
        }

        logger.info { "Successfully cached ${listings.size} listings for user: [$username]" }
    }

    /**
     * Clear cache data for a specific user
     */
    fun clearCache(username: String) = transaction {
        logger.info { "Clearing inventory cache for user: [$username]" }
        Listings.deleteWhere { Listings.username eq username }
        logger.info { "Successfully cleared inventory cache for user: [$username]" }
    }

    /**
     * Clear expired cache entries
     */
    fun clearExpiredCache() = transaction {
        val cutoffTime = clock.now() - CACHE_EXPIRY_DURATION
        val deletedCount = Listings.deleteWhere {
            Listings.cachedAt less cutoffTime
        }
        logger.info { "Cleared $deletedCount expired inventory cache entries" }
    }

    companion object {
        private val CACHE_EXPIRY_DURATION = 24.hours
    }
}
