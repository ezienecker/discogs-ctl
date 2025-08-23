package de.ezienecker.shared.discogs.marketplace

import de.ezienecker.shared.discogs.shared.Pagination
import de.ezienecker.shared.discogs.shared.Response
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class InventoryResponse(
    override val pagination: Pagination,
    @SerialName("listings")
    val result: List<Listing>,
) : Response()
