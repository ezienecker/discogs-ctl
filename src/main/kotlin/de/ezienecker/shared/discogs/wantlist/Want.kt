package de.ezienecker.shared.discogs.wantlist

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("wants")
data class Want(
    val rating: Int,
    @SerialName("basic_information")
    val basicInformation: BasicInformation,
    @SerialName("resource_url")
    val resourceUrl: Url,
    val id: Long,
)

@Serializable
data class BasicInformation(
    val formats: List<Format>,
    val thumb: String,
    @SerialName("cover_image")
    val coverImage: String,
    val title: String,
    val labels: List<Label>,
    val year: Int,
    val artists: List<Artist>,
    @SerialName("resource_url")
    val resourceUrl: Url,
    val id: Long,
)

@Serializable
data class Format(
    val text: String? = null,
    val qty: String,
    val descriptions: List<String>,
    val name: String,
) {
    fun formattedOutput(): String {
        return "$name, ${descriptions.joinToString { it }}"
    }
}

@Serializable
data class Label(
    @SerialName("resource_url")
    val resourceUrl: Url,
    @SerialName("entity_type")
    val entityType: String,
    @SerialName("catno")
    val catalogNumber: String,
    val id: Int,
    val name: String,
)

@Serializable
data class Artist(
    val join: String,
    val name: String,
    val anv: String,
    val tracks: String,
    val role: String,
    @SerialName("resource_url")
    val resourceUrl: Url,
    val id: Int,
)

@Serializable
@JvmInline
value class Url(val value: String)
