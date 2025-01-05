package de.ezienecker.shared.discogs.shared

import de.ezienecker.shared.discogs.marketplace.Listing
import de.ezienecker.shared.discogs.wantlist.Want
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class Response {
    abstract val pagination: Pagination
}

@Serializable
class Listings(
    override val pagination: Pagination,
    @SerialName("listings")
    val result: List<Listing>,
) : Response()

fun emptyListing() = Listings(
    pagination = Pagination(),
    result = emptyList(),
)

@Serializable
class Wants(
    override val pagination: Pagination,
    @SerialName("wants")
    val result: List<Want>,
) : Response()

fun emptyWants() = Wants(
    pagination = Pagination(),
    result = emptyList(),
)

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
