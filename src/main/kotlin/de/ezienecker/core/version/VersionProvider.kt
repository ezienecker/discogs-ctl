package de.ezienecker.core.version

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.*

private val logger = KotlinLogging.logger {}

object VersionProvider {

    private const val DEFAULT_VERSION = "unknown"
    private const val PROPERTIES_FILE = "application.properties"
    private const val VERSION_PROPERTY = "version"
    
    val version: String get() {
        return loadVersionFromProperties().ifBlank {
            DEFAULT_VERSION
        }
    }

    fun getPropertiesFile(): String = PROPERTIES_FILE

    private fun loadVersionFromProperties(): String {
        return runCatching {
            val properties = Properties()
            val inputStream = VersionProvider::class.java.classLoader
                .getResourceAsStream(getPropertiesFile())
                ?: return@runCatching null
            
            inputStream.use { stream ->
                properties.load(stream)
                properties.getProperty(VERSION_PROPERTY)
            }
        }.onFailure { exception ->
            logger.warn(exception) { "Failed to load version from ${getPropertiesFile()}" }
        }.getOrNull() ?: run {
            logger.info { "Version not found in properties, using default: $DEFAULT_VERSION" }
            DEFAULT_VERSION
        }
    }
}
