package de.ezienecker.shared.discogs.wantlist

import de.ezienecker.shared.discogs.shared.Pagination
import de.ezienecker.shared.discogs.shared.Response
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class WantsResponse(
    override val pagination: Pagination,
    @SerialName("wants")
    val result: List<Want>,
) : Response()
