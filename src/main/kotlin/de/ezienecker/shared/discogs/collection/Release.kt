package de.ezienecker.shared.discogs.collection

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Release(
    val id: Long,
    @SerialName("instance_id")
    val instanceId: Long,
    @SerialName("date_added")
    val dateAdded: String,
    val rating: Int,
    @SerialName("basic_information")
    val basicInformation: BasicInformation
)

@Serializable
data class BasicInformation(
    val id: Long,
    @SerialName("master_id")
    val masterId: Long,
    @SerialName("master_url")
    val masterUrl: String? = null,
    @SerialName("resource_url")
    val resourceUrl: String,
    val thumb: String,
    val title: String,
    val year: Int,
    val formats: List<Format>,
    val labels: List<Label>,
    val artists: List<Artist>,
    val genres: List<String>,
    val styles: List<String>,
)

@Serializable
data class Format(
    val name: String,
    val qty: String,
    val text: String? = null,
    val descriptions: List<String>? = emptyList(),
)

@Serializable
data class Label(
    val name: String,
    @SerialName("catno")
    val catNo: String,
    @SerialName("entity_type")
    val entityType: String,
    @SerialName("entity_type_name")
    val entityTypeName: String,
    val id: Long,
    @SerialName("resource_url")
    val resourceUrl: String,
)

@Serializable
data class Artist(
    val id: Long,
    val name: String,
    val anv: String,
    val join: String,
    val role: String,
    val tracks: String,
    @SerialName("resource_url")
    val resourceUrl: String,
)
