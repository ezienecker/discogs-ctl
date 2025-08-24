package de.ezienecker.core.infrastructure.discogs.wantlist

import de.ezienecker.core.infrastructure.discogs.shared.Pagination
import de.ezienecker.core.infrastructure.discogs.shared.Response
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class WantsResponse(
    override val pagination: Pagination,
    @SerialName("wants")
    val result: List<Want>,
) : Response()
