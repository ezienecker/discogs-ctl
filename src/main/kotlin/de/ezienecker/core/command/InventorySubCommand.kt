package de.ezienecker.core.command

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.mordant.terminal.Terminal
import de.ezienecker.core.configuration.service.ConfigurationService
import de.ezienecker.core.infrastructure.discogs.marketplace.Condition
import de.ezienecker.core.infrastructure.discogs.marketplace.Price
import io.github.oshai.kotlinlogging.KotlinLogging
import org.slf4j.LoggerFactory

private val logger = KotlinLogging.logger {}

abstract class InventorySubCommand<T>(
    name: String? = null,
    private val configurationService: ConfigurationService,
    private val terminal: Terminal,
) : CliktCommand(name = name) {
    val username by option(
        names = arrayOf("--username", "-u"),
        help = "The username for whose resources you are fetching",
    )

    val format by option(
        names = arrayOf("--output", "-o"),
        help = "Output format. One of: 'compact', 'wide'. 'compact' is default",
    )
        .choice("compact", "wide")
        .default("compact")

    private val force by option(
        names = arrayOf("--force-update", "-f"),
        help = "Force update the cache",
    )
        .flag(default = false)

    private val verbose by option(
        names = arrayOf("--verbose", "-v"),
        help = "Make the operation more talkative",
    )
        .flag(default = false)

    fun handleVerboseOption() {
        if (verbose) {
            (LoggerFactory.getILoggerFactory() as LoggerContext).getLogger("de.ezienecker").apply {
                level = Level.INFO
            }
        }
    }

    fun getUsernameForInventory(username: String?): String? {
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
            printListingsAsTable(entries, filteredIds)
    }

    abstract fun printListingsAsTable(inventory: List<T>, filteredIds: Set<Long>)

    fun filterRelease(idsFromInventory: Set<Long>, id: Long) =
        if (idsFromInventory.isEmpty()) {
            true
        } else {
            idsFromInventory.contains(id)
        }
}