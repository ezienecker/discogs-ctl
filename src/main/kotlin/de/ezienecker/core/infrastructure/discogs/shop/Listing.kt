package de.ezienecker.core.infrastructure.discogs.shop

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Listing(
    val id: Long,
    @SerialName("resource_url")
    val resourceUrl: Url,
    val uri: Uri,
    val status: Status,
    @SerialName("condition")
    val mediaCondition: Condition,
    @SerialName("sleeve_condition")
    val sleeveCondition: Condition,
    val comments: Comments,
    val price: Price,
    val seller: Seller,
    val release: Release,
)

@Serializable
@JvmInline
value class Uri(val value: String)

enum class Status {
    @SerialName("Draft")
    DRAFT,
    @SerialName("Sold")
    SOLD,
    @SerialName("For Sale")
    FOR_SALE,
}

@Serializable
@JvmInline
value class Comments(val value: String)

enum class Condition(val full: String, val shortcut: String) {
    @SerialName("Mint (M)")
    MINT("Mint", "M"),

    @SerialName("Near Mint (NM or M-)")
    NEAR_MINT("Near Mint", "NM"),

    @SerialName("Very Good Plus (VG+)")
    VERY_GOOD_PLUS("Very Good Plus", "VG+"),

    @SerialName("Very Good (VG)")
    VERY_GOOD("Very Good", "VG"),

    @SerialName("Good Plus (G+)")
    GOOD_PLUS("Good Plus", "G+"),

    @SerialName("Good (G)")
    GOOD("Good", "G"),

    @SerialName("Poor (P)")
    POOR("Poor", "P"),

    @SerialName("Fair (F)")
    FAIR("Fair", "F"),

    @SerialName("Generic")
    GENERIC("Generic", "NA"),

    @SerialName("Not Graded")
    NOT_GRADED("Not Graded", "NG"),

    @SerialName("No Cover")
    NO_COVER("No Cover", "NC"),
}

@Serializable
data class Price(
    val value: Double,
    val currency: Currency,
)

@Serializable
@JvmInline
value class Currency(val value: String)

@Serializable
data class Seller(
    val username: Username,
    val htmlUrl: Url? = null,
    val resourceUrl: Url? = null,
)

@Serializable
@JvmInline
value class Username(val value: String)

@Serializable
@JvmInline
value class Url(val value: String)

@Serializable
data class Release(
    val description: Description,
    val artist: Artist,
    val format: Format,
    val title: Title,
    val id: Long,
)

@Serializable
@JvmInline
value class Description(val value: String)

@Serializable
@JvmInline
value class Artist(val value: String)

@Serializable
@JvmInline
value class Format(val value: String)

@Serializable
@JvmInline
value class Title(val value: String)
