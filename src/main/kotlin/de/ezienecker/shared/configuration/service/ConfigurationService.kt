package de.ezienecker.shared.configuration.service

import de.ezienecker.shared.configuration.model.Configuration
import de.ezienecker.shared.configuration.model.Token
import de.ezienecker.shared.configuration.model.Username
import de.ezienecker.shared.discogs.client.auth.providers.DiscogsAuthentication
import de.ezienecker.shared.discogs.client.auth.providers.DiscogsClientConfiguration
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

class ConfigurationService {
    private var propertiesFile: File
    private var properties: Properties = Properties()
    private var configuration: Configuration

    init {
        val userHome: String = System.getProperty("user.home")?.also {
            Files.createDirectories(Paths.get("$it/$APPLICATION_CONFIGURATION_FOLDER_NAME"))
        } ?: throw IllegalStateException("Could not access the user's home folder.")

        propertiesFile = File("$userHome/$APPLICATION_CONFIGURATION_FOLDER_NAME/config.properties").also {
            it.createNewFile()
        }

        FileInputStream(propertiesFile).use {
            properties.load(it)
        }

        configuration = Configuration(
            username = properties[USERNAME]?.let { Username(it as String) },
            token = properties[TOKEN]?.let { Token(it as String) },
        )
    }

    fun configureDefaultUser(username: Username) = configureProperty(USERNAME, username.value)

    fun configureToken(token: Token) = configureProperty(TOKEN, token.value)

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

    fun getConfiguration() = properties.stringPropertyNames()
        .associateWith { properties.getProperty(it) }

    companion object {
        private const val APPLICATION_CONFIGURATION_FOLDER_NAME: String = ".discogsctl"

        internal const val USERNAME: String = "username"

        internal const val TOKEN: String = "token"
    }
}
