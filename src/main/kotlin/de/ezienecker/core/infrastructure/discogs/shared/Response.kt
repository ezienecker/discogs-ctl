package de.ezienecker.core.infrastructure.discogs.shared

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
abstract class Response {
    abstract val pagination: Pagination
}

@Serializable
data class Pagination(
    val page: Int = 0,
    val pages: Int = 0,
    @SerialName("per_page")
    val perPage: Int = 0,
    val items: Int = 0,
    val urls: Urls = Urls(),
) {
    fun hasNext(): Boolean = !urls.next.isNullOrEmpty()
}

@Serializable
data class Urls(
    val last: String? = null,
    val next: String? = null,
)
