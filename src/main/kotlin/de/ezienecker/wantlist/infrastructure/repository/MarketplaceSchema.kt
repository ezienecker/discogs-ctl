package de.ezienecker.wantlist.infrastructure.repository

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
object MarketplaceListings : LongIdTable("marketplace_listings") {
    val releaseId: Column<Long> = long("release_id")
    val title: Column<String> = varchar("title", 500)
    val resourceUrl: Column<String> = varchar("resource_url", 500)
    val mediaCondition: Column<String> = varchar("media_condition", 50)
    val sleeveCondition: Column<String> = varchar("sleeve_condition", 50)
    val priceWithCurrency: Column<String> = varchar("price_with_currency", 50)
    val seller: Column<String> = varchar("seller", 500)
    val shippingLocation: Column<String> = varchar("shipping_location", 500)

    val cachedAt: Column<Instant> = timestamp("cached_at")

    init {
        uniqueIndex(releaseId, seller, priceWithCurrency)
    }
}