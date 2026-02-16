package de.ezienecker.wantlist.infrastructure.repository

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
object Wants : LongIdTable("wants") {
    val username: Column<String> = varchar("username", 255)
    val wantId: Column<Long> = long("want_id")
    val rating: Column<Int> = integer("rating")
    val resourceUrl: Column<String> = varchar("resource_url", 500)
    val basicInformation: Column<String> = text("basic_information")
    val cachedAt: Column<Instant> = timestamp("cached_at")
    
    init {
        uniqueIndex(username, wantId)
    }
}

object WantBasicInformation : LongIdTable("want_basic_information") {
    val wantId: Column<Long> = long("want_id")
    val releaseId: Column<Long> = long("release_id")
    val formats: Column<String> = text("formats") // JSON array
    val thumb: Column<String> = varchar("thumb", 500)
    val coverImage: Column<String> = varchar("cover_image", 500)
    val title: Column<String> = varchar("title", 500)
    val labels: Column<String> = text("labels") // JSON array
    val year: Column<Int> = integer("year")
    val artists: Column<String> = text("artists") // JSON array
    val resourceUrl: Column<String> = varchar("resource_url", 500)
    
    init {
        uniqueIndex(wantId)
    }
}
