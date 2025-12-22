package de.ezienecker.shop.command

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.animation.progress.advance
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import de.ezienecker.core.command.InventorySubCommand
import de.ezienecker.core.command.OutputFormat
import de.ezienecker.core.configuration.service.ConfigurationService
import de.ezienecker.core.infrastructure.discogs.client.ApiException
import de.ezienecker.core.infrastructure.discogs.marketplace.Listing
import de.ezienecker.core.infrastructure.discogs.marketplace.Status
import de.ezienecker.shop.service.ShopService
import de.ezienecker.wantlist.service.WantlistService
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.json.Json

class Shop(
    private val shopService: ShopService,
    private val wantListService: WantlistService,
    configurationService: ConfigurationService,
    private val terminal: Terminal,
) : InventorySubCommand<Listing>(configurationService = configurationService, terminal = terminal) {

    override fun help(context: Context) = """
            Displays the shop inventory from a user. 
            If no username has been specified, the tool looks for a username in the configuration file (see discogs-ctl config --help for more information).
            
            # List shop inventory for user John-Doe:
            discogs-ctl shop --username John-Doe
            
            # List shop inventory for user John-Doe with more information:
            discogs-ctl shop --username John-Doe --output wide
            
            # List shop inventory for user John-Doe filtered by wantlist entries from user Jane-Roe:
            discogs-ctl shop --username John-Doe --filtered-by-wantlist Jane-Roe
        """.trimIndent()

    private val fromWantListUsername by option(
        "--filtered-by-wantlist", "-w",
        help = "The username for whose wantlist inventory you are fetching. " +
                "If this option is set, only the entries that appear in the user's wantlist are displayed."
    )

    override fun run(): Unit = runBlocking {
        handleVerboseOption()

        runIfUsernameSpecified { username ->

            launch { progress.execute() }

            progress.update { total = 2 }

            supervisorScope {
                val listings = async { shopService.listInventoryByUser(username, sortBy, sortOrder) }
                    .also { it.invokeOnCompletion { progress.advance(1) } }
                val filterIds = async { wantListService.getIdsFromWantlistReleasesByUser(fromWantListUsername) }
                    .also { it.invokeOnCompletion { progress.advance(1) } }

                process(listings.await(), filterIds.await())
            }
        }
    }

    fun process(listings: Result<List<Listing>>, filterIds: Set<Long>) {
        listings
            .onFailure { printError((it as ApiException).error.message) }
            .onSuccess { printListings(it.filter { listing -> listing.status == Status.FOR_SALE }, filterIds) }
    }

    override fun printListings(inventory: List<Listing>, filteredIds: Set<Long>, outputFormat: OutputFormat) {
        val filteredInventory = inventory.filter {
            filterRelease(filteredIds, it.release.id)
        }

        when (outputFormat) {
            OutputFormat.Compact, OutputFormat.Wide -> printListingsAsTable(filteredInventory)
            OutputFormat.Json -> printListingsAsJson(filteredInventory)
            OutputFormat.Display -> printListingsAsDisplay(filteredInventory)
        }
    }

    private fun printListingsAsTable(inventory: List<Listing>) {
        terminal.println(table {
            header {
                cellBorders = Borders.NONE

                row("Artist", "Title", "Media", "Sleeve", "Format", "Price")
            }
            body {
                cellBorders = Borders.NONE

                inventory.forEach {
                    row(
                        addLineBreak(it.release.artist.value),
                        addLineBreak(it.release.title.value),
                        it.mediaCondition.format(isWideFormatActive(format)),
                        it.sleeveCondition.format(isWideFormatActive(format)),
                        it.release.format.value,
                        it.price.format(isWideFormatActive(format))
                    )
                }
            }
        })
    }

    private fun printListingsAsJson(inventory: List<Listing>) {
        val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        echo(json.encodeToString(inventory))
    }

    private fun printListingsAsDisplay(inventory: List<Listing>) {
        terminal.println(table {
            body {
                cellBorders = Borders.NONE

                inventory.forEachIndexed { index, listing ->
                    row {
                        cell("-[ Record ${(index + 1)} ]---------------------") {
                            columnSpan = 2
                            align = TextAlign.LEFT
                        }
                    }
                    row("Artist", "| ${listing.release.artist.value}")
                    row("Title", "| ${listing.release.title.value}")
                    row("Media Condition", "| ${listing.mediaCondition.format(isWideFormatActive(format))}")
                    row("Sleeve Condition", "| ${listing.sleeveCondition.format(isWideFormatActive(format))}")
                    row("Format", "| ${listing.release.format.value}")
                    row("Price", "| ${listing.price.format(isWideFormatActive(format))}")
                }
            }
        })
    }
}
