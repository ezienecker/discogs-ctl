package de.ezienecker.core.infrastructure.discogs.marketplace

data class MarketplaceListing(
    val releaseId: Long,
    val title: String,
    val resourceUrl: String,
    val mediaCondition: String,
    val sleeveCondition: String,
    val price: String,
    val seller: MarketplaceSeller,
    val shippingLocation: String,
)


@JvmInline
value class MarketplaceSeller(val name: String)
