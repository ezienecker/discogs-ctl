package de.ezienecker.shop.infrastructure.repository

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
object Listings : LongIdTable("listings") {
    val username: Column<String> = varchar("username", 255)
    val listingId: Column<Long> = long("listing_id")
    val resourceUrl: Column<String> = varchar("resource_url", 500)
    val uri: Column<String> = varchar("uri", 500)
    val status: Column<String> = varchar("status", 50)
    val mediaCondition: Column<String> = varchar("media_condition", 50)
    val sleeveCondition: Column<String> = varchar("sleeve_condition", 50)
    val comments: Column<String> = text("comments")
    val priceValue: Column<Double> = double("price_value")
    val priceCurrency: Column<String> = varchar("price_currency", 10)
    val seller: Column<String> = text("seller") // JSON object
    val release: Column<String> = text("release") // JSON object
    val cachedAt: Column<Instant> = timestamp("cached_at")
    
    init {
        uniqueIndex(username, listingId)
    }
}
