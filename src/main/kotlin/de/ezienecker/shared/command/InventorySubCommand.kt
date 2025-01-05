package de.ezienecker.de.ezienecker.shared.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import de.ezienecker.shared.configuration.service.ConfigurationService
import de.ezienecker.shared.discogs.marketplace.Condition
import de.ezienecker.shared.discogs.marketplace.Price

abstract class InventorySubCommand<T>(
    name: String? = null,
    private val configurationService: ConfigurationService
) : CliktCommand(name = name) {
    val username by option(
        names = arrayOf("--username", "-u"),
        help = "The username for whose resources you are fetching",
    )

    val format by option(
        names = arrayOf("--output", "-o"),
        help = "Output format. One of: 'compact', 'json', 'wide'. 'compact' is default",
    )
        .choice("compact", "json", "wide")
        .default("compact")

    private val force by option(
        names = arrayOf("--force", "-f"),
        help = ""
    )
        .flag(default = false)

    fun getUsernameForInventory(username: String?): String? {
        if (username != null) {
            return username
        }

        return configurationService.getUsername()?.value
    }

    fun isWideFormatActive(format: String): Boolean = "wide" == format.lowercase()

    private fun isJsonFormatActive(format: String): Boolean = "json" == format.lowercase()

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

    fun printListings(inventory: List<T>, idsFromInventoryToFiltering: Set<Long>) {
        if (isJsonFormatActive(format)) {
            printListingsAsJson(inventory, idsFromInventoryToFiltering)
        } else {
            printListingsAsTable(inventory, idsFromInventoryToFiltering)
        }
    }

    abstract fun printListingsAsTable(inventory: List<T>, idsFromExternalInventoryToFiltering: Set<Long>)

    abstract fun printListingsAsJson(inventory: List<T>, idsFromExternalInventoryToFiltering: Set<Long>)

    fun filterRelease(idsFromInventory: Set<Long>, id: Long) =
        if (idsFromInventory.isEmpty()) {
            true
        } else {
            idsFromInventory.contains(id)
        }
}