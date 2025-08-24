package de.ezienecker.core.infrastructure.discogs.collection

import de.ezienecker.core.infrastructure.discogs.shared.Pagination
import de.ezienecker.core.infrastructure.discogs.shared.Response
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class CollectionResponse(
    override val pagination: Pagination,
    @SerialName("releases")
    val result: List<Release>,
) : Response()
