package de.ezienecker.config.service

import de.ezienecker.config.service.ConfigService.Companion.ACCESS_TOKEN
import de.ezienecker.config.service.ConfigService.Companion.KEY
import de.ezienecker.config.service.ConfigService.Companion.SECRET
import de.ezienecker.config.service.ConfigService.Companion.USERNAME
import de.ezienecker.de.ezienecker.config.model.AccessToken
import de.ezienecker.de.ezienecker.config.model.Key
import de.ezienecker.de.ezienecker.config.model.Secret
import de.ezienecker.de.ezienecker.config.model.Username
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

interface ConfigService {
    fun configureDefaultUser(username: Username)

    fun configureKey(key: Key)

    fun configureSecret(secret: Secret)

    fun configureAccessToken(accessToken: AccessToken)

    fun getConfiguration(): Map<String, String>

    companion object {
        internal const val USERNAME: String = "username"

        internal const val KEY: String = "key"
        internal const val SECRET: String = "secret"

        internal const val ACCESS_TOKEN: String = "accessToken"
    }
}

class ConfigServiceImpl : ConfigService {

    private var propertiesFile: File
    private var properties: Properties = Properties()

    init {
        val userHome: String = System.getProperty("user.home")?.also {
            Files.createDirectories(Paths.get("$it/.discogsctl"))
        } ?: throw IllegalStateException("Could not access to the user's home folder.")

        propertiesFile = File("$userHome/.discogsctl/config.properties").also {
            it.createNewFile()
        }

        FileInputStream(propertiesFile).use {
            properties.load(it)
        }
    }

    override fun configureDefaultUser(username: Username) = configureProperty(USERNAME, username.value)

    override fun configureKey(key: Key) = configureProperty(KEY, key.value)

    override fun configureSecret(secret: Secret) = configureProperty(SECRET, secret.value)

    override fun configureAccessToken(accessToken: AccessToken) = configureProperty(ACCESS_TOKEN, accessToken.value)

    private fun configureProperty(key: String, value: String) = FileOutputStream(propertiesFile).use {
        properties.setProperty(key, value)
        properties.store(it, "Associate $key with $value and write to configuration file")
    }

    override fun getConfiguration() = properties.stringPropertyNames()
        .associateWith { properties.getProperty(it) }

}