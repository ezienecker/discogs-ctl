package de.ezienecker.collection.infrastructure.repository

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
object CachedCollections : LongIdTable("collections") {
    val username: Column<String> = varchar("username", 255)
    val releaseId: Column<Long> = long("release_id")
    val instanceId: Column<Long> = long("instance_id")
    val dateAdded: Column<String> = varchar("date_added", 50)
    val rating: Column<Int> = integer("rating")
    val basicInformation: Column<String> = text("basic_information")
    val cachedAt: Column<Instant> = timestamp("cached_at")

    init {
        uniqueIndex(username, releaseId, instanceId)
    }
}

object CachedBasicInformation : LongIdTable("collection_basic_information") {
    val releaseId: Column<Long> = long("release_id")
    val masterId: Column<Long> = long("master_id")
    val masterUrl: Column<String?> = varchar("master_url", 500).nullable()
    val resourceUrl: Column<String> = varchar("resource_url", 500)
    val thumb: Column<String> = varchar("thumb", 500)
    val coverImage: Column<String> = varchar("cover_image", 500)
    val title: Column<String> = varchar("title", 500)
    val year: Column<Int> = integer("year")
    val formats: Column<String> = text("formats") // JSON array
    val labels: Column<String> = text("labels") // JSON array
    val artists: Column<String> = text("artists") // JSON array
    val genres: Column<String> = text("genres") // JSON array
    val styles: Column<String> = text("styles") // JSON array

    init {
        uniqueIndex(releaseId)
    }
}
