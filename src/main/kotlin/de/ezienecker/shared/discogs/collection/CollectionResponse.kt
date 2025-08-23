package de.ezienecker.shared.discogs.collection

import de.ezienecker.shared.discogs.shared.Pagination
import de.ezienecker.shared.discogs.shared.Response
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class CollectionResponse(
    override val pagination: Pagination,
    @SerialName("releases")
    val result: List<Release>,
) : Response()
