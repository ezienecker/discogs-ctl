package de.ezienecker.core.command

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.mordant.animation.coroutines.animateInCoroutine
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.Spinner
import com.github.ajalt.mordant.widgets.progress.progressBarContextLayout
import com.github.ajalt.mordant.widgets.progress.spinner
import com.github.ajalt.mordant.widgets.progress.text
import de.ezienecker.core.configuration.service.ConfigurationService
import de.ezienecker.core.infrastructure.discogs.shop.Condition
import de.ezienecker.core.infrastructure.discogs.shop.Price
import io.github.oshai.kotlinlogging.KotlinLogging
import org.slf4j.LoggerFactory

private val logger = KotlinLogging.logger {}

sealed interface OutputFormat {
    data object Compact : OutputFormat
    data object Wide : OutputFormat
    data object Json : OutputFormat
    data object Display : OutputFormat

    companion object {
        fun from(value: String): OutputFormat = when (value.lowercase()) {
            "wide" -> Wide
            "json" -> Json
            "display" -> Display
            else -> Compact
        }
    }
}

abstract class InventorySubCommand<T>(
    name: String? = null,
    private val configurationService: ConfigurationService,
    private val terminal: Terminal,
) : CliktCommand(name = name) {
    private val username by option(
        names = arrayOf("--username", "-u"),
        help = "The username for whose resources you are fetching",
    )

    val format by option(
        names = arrayOf("--output", "-o"),
        help = "Output format. One of: 'compact', 'wide', 'json', 'display'. 'compact' is default",
    )
        .choice("compact", "wide", "json", "display")
        .default("compact")

    val force by option(
        names = arrayOf("--force-update", "-f"),
        help = "Force update the cache",
    )
        .flag(default = false)

    private val verbose by option(
        names = arrayOf("--verbose", "-v"),
        help = "Make the operation more talkative",
    )
        .flag(default = false)

    val sortBy by option(
        names = arrayOf("--sort-by"),
        help = "Sort by field. One of: 'item' (title of the release), 'artist', 'price'. 'artist' is default",
    )
        .choice("item", "artist")
        .default("artist")

    val sortOrder by option(
        names = arrayOf("--sort-order"),
        help = "Sort order. One of: 'asc' (ascending), 'desc' (descending). 'asc' is default",
    )
        .choice("asc", "desc")
        .default("asc")

    val progress = progressBarContextLayout {
        text { context }
        spinner(Spinner.Lines())
    }.animateInCoroutine(terminal, context = "Fetching data")

    fun handleVerboseOption() {
        if (verbose) {
            (LoggerFactory.getILoggerFactory() as LoggerContext).getLogger("de.ezienecker").apply {
                level = Level.INFO
            }
        }
    }

    suspend fun runIfUsernameSpecified(block: suspend (username: String) -> Unit) =
        getUsernameForInventory()?.let { user ->
            block(user)
        } ?: echo("Please provide a valid username.")

    private fun getUsernameForInventory(): String? {
        if (username != null) {
            logger.info { "Username specified: [$username]" }
            return username
        }

        return configurationService.getUsername()?.value.also {
            logger.info { "Username not specified, using default from configuration: [$it]" }
        }
    }

    fun isWideFormatActive(format: String): Boolean = "wide" == format.lowercase()

    fun addLineBreak(text: String): String {
        val sb = StringBuilder()
        var counter = 0
        text.split(" ").forEach {
            counter += it.length
            if (counter >= 30) {
                counter = 0
                sb.append(System.lineSeparator())
            }

            sb.append("$it ")
        }

        return sb.toString()
    }

    fun Condition.format(wideOutput: Boolean) =
        if (wideOutput) {
            "${this.full} (${this.shortcut})"
        } else {
            this.shortcut
        }

    fun Price.format(wideOutput: Boolean) =
        if (wideOutput) {
            "${this.value} ${this.currency.value}"
        } else {
            val currencyShort = when (this.currency.value) {
                "EUR" -> "€"
                else -> "N/A"
            }
            "${this.value} $currencyShort"
        }

    fun printError(message: String) {
        terminal.println(message)
    }

    fun printListings(entries: List<T>, filteredIds: Set<Long>) {
        val outputFormat = OutputFormat.from(format)
        printListings(entries, filteredIds, outputFormat)
    }

    abstract fun printListings(inventory: List<T>, filteredIds: Set<Long>, outputFormat: OutputFormat)

    fun filterRelease(idsFromInventory: Set<Long>, id: Long) =
        if (idsFromInventory.isEmpty()) {
            true
        } else {
            idsFromInventory.contains(id)
        }

    fun getReleaseLink(id: Long): String = "https://www.discogs.com/release/$id"
}