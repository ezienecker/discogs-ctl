package de.ezienecker.core.configuration.service

import de.ezienecker.core.configuration.model.Configuration
import de.ezienecker.core.configuration.model.Token
import de.ezienecker.core.configuration.model.Username
import de.ezienecker.core.infrastructure.discogs.client.auth.providers.DiscogsAuthentication
import de.ezienecker.core.infrastructure.discogs.client.auth.providers.DiscogsClientConfiguration
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

class ConfigurationService {
    private var propertiesFile: File =
        File("$USER_HOME/$APPLICATION_CONFIGURATION_FOLDER_NAME/config.properties").also {
            it.createNewFile()
        }
    private var properties: Properties = Properties()
    private var configuration: Configuration

    init {

        FileInputStream(propertiesFile).use {
            properties.load(it)
        }

        configuration = Configuration(
            username = properties[USERNAME]?.let { Username(it as String) },
            token = properties[TOKEN]?.let { Token(it as String) },
            collectionCacheDuration = properties[COLLECTION_CACHE_DURATION]?.let { parseDurationWithDefault(it as String) },
            shopCacheDuration = properties[SHOP_CACHE_DURATION]?.let { parseDurationWithDefault(it as String) },
            wantlistCacheDuration = properties[WANTLIST_CACHE_DURATION]?.let { parseDurationWithDefault(it as String) },
            marketplaceListingsCacheDuration = properties[MARKETPLACE_LISTINGS_CACHE_DURATION]?.let { parseDurationWithDefault(it as String) },
        )
    }

    fun configureDefaultUser(username: Username) = configureProperty(USERNAME, username.value)

    fun configureToken(token: Token) = configureProperty(TOKEN, token.value)

    fun configureDuration(property: String, duration: Duration) = configureProperty(property, duration.toString())

    private fun configureProperty(key: String, value: String) = FileOutputStream(propertiesFile).use {
        properties.setProperty(key, value)
        properties.store(it, "Associate $key with $value and write to configuration file")
    }

    fun getDiscogsClientConfiguration(): DiscogsClientConfiguration = DiscogsClientConfiguration(
        authentication = DiscogsAuthentication(
            token = getDiscogsAuthenticationToken(),
        )
    )

    private fun getDiscogsAuthenticationToken(): String? =
        configuration.token?.value

    fun getUsername(): Username? = configuration.username

    fun getCollectionCacheDuration(): Duration = configuration.collectionCacheDuration ?: 7.days

    fun getShopCacheDuration(): Duration = configuration.shopCacheDuration ?: 7.days

    fun getWantlistCacheDuration(): Duration = configuration.wantlistCacheDuration ?: 7.days

    fun getMarketplaceListingsCacheDuration(): Duration = configuration.marketplaceListingsCacheDuration ?: 7.days

    fun getConfiguration() = properties.stringPropertyNames()
        .associateWith { properties.getProperty(it) }

    private fun parseDurationWithDefault(value: String?, default: Duration = 7.days): Duration =
        value?.let {
            try {
                Duration.parse(it)
            } catch (_: IllegalArgumentException) {
                default
            }
        } ?: default

    companion object {
        private const val APPLICATION_CONFIGURATION_FOLDER_NAME: String = ".discogsctl"

        internal const val USERNAME: String = "username"

        internal const val TOKEN: String = "token"

        internal const val COLLECTION_CACHE_DURATION: String = "collection_cache_duration"

        internal const val SHOP_CACHE_DURATION: String = "shop_cache_duration"

        internal const val WANTLIST_CACHE_DURATION: String = "wantlist_cache_duration"

        internal const val MARKETPLACE_LISTINGS_CACHE_DURATION: String = "marketplace_listings_cache_duration"

        val USER_HOME: String =
            System.getProperty("user.home") ?: throw IllegalStateException("Could not access the user's home folder.")

        val APPLICATION_FOLDER: Path =
            Files.createDirectories(Paths.get("$USER_HOME/$APPLICATION_CONFIGURATION_FOLDER_NAME"))
    }
}
