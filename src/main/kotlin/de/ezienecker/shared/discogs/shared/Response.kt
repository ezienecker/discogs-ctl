package de.ezienecker.shared.discogs.shared

import de.ezienecker.shared.discogs.collection.Release
import de.ezienecker.shared.discogs.marketplace.Listing
import de.ezienecker.shared.discogs.wantlist.Want
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class Response {
    abstract val pagination: Pagination
}

@Serializable
class CollectionReleases(
    override val pagination: Pagination,
    @SerialName("releases")
    val result: List<Release>,
) : Response()

inline fun <reified T : Response> emptyResponse(factory: (Pagination, List<Any>) -> T): T =
    factory(Pagination(), emptyList())

@Serializable
class Listings(
    override val pagination: Pagination,
    @SerialName("listings")
    val result: List<Listing>,
) : Response()

@Serializable
class Wants(
    override val pagination: Pagination,
    @SerialName("wants")
    val result: List<Want>,
) : Response()

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
