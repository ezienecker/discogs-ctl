package de.ezienecker.core.infrastructure.discogs.shop

import de.ezienecker.core.infrastructure.discogs.shared.Pagination
import de.ezienecker.core.infrastructure.discogs.shared.Response
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class ShopResponse(
    override val pagination: Pagination,
    @SerialName("listings")
    val result: List<Listing>,
) : Response()
