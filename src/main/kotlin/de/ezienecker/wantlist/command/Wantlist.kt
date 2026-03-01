package de.ezienecker.wantlist.command

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.mordant.animation.progress.advance
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import de.ezienecker.core.command.InventorySubCommand
import de.ezienecker.core.command.OutputFormat
import de.ezienecker.core.configuration.service.ConfigurationService
import de.ezienecker.core.infrastructure.discogs.client.ApiException
import de.ezienecker.core.infrastructure.discogs.marketplace.MarketplaceListing
import de.ezienecker.core.infrastructure.discogs.marketplace.MarketplaceSeller
import de.ezienecker.core.infrastructure.discogs.wantlist.Want
import de.ezienecker.shop.service.ShopService
import de.ezienecker.wantlist.service.MarketplaceService
import de.ezienecker.wantlist.service.WantlistService
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.json.Json

class Wantlist(
    private val shopService: ShopService,
    private val wantListService: WantlistService,
    private val marketplaceService: MarketplaceService,
    configurationService: ConfigurationService,
    private val terminal: Terminal,
) : InventorySubCommand<Want>(
    name = COMMAND,
    configurationService = configurationService,
    terminal = terminal,
) {

    companion object {
        internal const val COMMAND = "wantlist"
    }

    override fun help(context: Context) = """
        Displays the wantlist inventory from a user.
        If no username has been specified, the tool looks for a username in the configuration file (see discogs-ctl config --help for more information).
        
        # List wantlist inventory for user John-Doe:
        discogs-ctl wantlist --username John-Doe
        
        # List wantlist inventory for user John-Doe with more information:
        discogs-ctl wantlist --username John-Doe --output wide
        
        # List wantlist inventory for user John-Doe filtered by shop entries from user Jane-Roe:
        discogs-ctl wantlist --username John-Doe --filtered-by-shop Jane-Roe
        
        # Show the 10 sellers (default) with the most listings for the wantlist entries, along with the details of their listings.
        discogs-ctl wantlist --group-by-seller
        
        # Only shows the top 5 sellers with the most listings for the wantlist entries, along with the details of their listings.
        discogs-ctl wantlist --group-by-seller --limit-group-by-seller 5
        """.trimIndent()

    private val fromShopUsername by option(
        "--filtered-by-shop", "-s",
        help = "The username for whose shop inventory you are fetching " +
                "If this option is set, only the entries that appear in the user's inventory are displayed."
    )

    private val groupBySeller by option(
        "--group-by-seller", "-g",
        help = "When set, the wantlist entries are grouped by seller."
    )
        .flag(default = false)

    private val limitGroupBySellerEntries by option(
        "--limit-group-by-seller", "-l",
        help = "Note this option is always applied when --group-by-seller is set, even if --limit-group-by-seller is not explicitly specified, in which case the default value of 10 is applied." +
                "This option limits the number of seller entries shown when the --group-by-seller option is set.",
        metavar = "<number>"
    )
        .int()
        .default(10)

    override fun run(): Unit = runBlocking {
        handleVerboseOption()

        runIfUsernameSpecified { username ->

            launch { progress.execute() }

            progress.update { total = 2 }

            supervisorScope {
                val wants = async { wantListService.listWantsByUser(username, sortBy, sortOrder, force) }
                    .also { it.invokeOnCompletion { progress.advance(1) } }

                if (groupBySeller) {

                    val releaseIds = wants.await().fold(
                        onSuccess = { it.map { want -> want.releaseId } },
                        onFailure = { emptyList() }
                    )

                    val marketplaceListingsGroupedBySeller =
                        marketplaceService.getMarketplaceListingsByReleaseIds(releaseIds, force)
                            .entries
                            .sortedByDescending { it.value.size }
                            .take(limitGroupBySellerEntries)
                            .associate { it.key to it.value }
                            .also { progress.advance(1) }

                    showWantlistEntriesGroupedBySeller(marketplaceListingsGroupedBySeller)
                } else {
                    val filterIds = async { shopService.getIdsFromInventoryReleasesByUser(fromShopUsername) }
                        .also { it.invokeOnCompletion { progress.advance(1) } }

                    showWantlistEntries(wants.await(), filterIds.await())
                }
            }
        }
    }

    fun showWantlistEntries(wants: Result<List<Want>>, filterIds: Set<Long>) {
        wants
            .onFailure { printError((it as ApiException).error.message) }
            .onSuccess { printListings(it, filterIds) }
    }

    fun showWantlistEntriesGroupedBySeller(marketplaceListingsGroupedBySeller: Map<MarketplaceSeller, List<MarketplaceListing>>) {
        if (marketplaceListingsGroupedBySeller.isEmpty()) {
            terminal.println("No marketplace listings found for the wantlist entries.")
        } else {
            printMarketplaceListingsGroupedBySeller(marketplaceListingsGroupedBySeller)
        }
    }

    override fun printListings(inventory: List<Want>, filteredIds: Set<Long>, outputFormat: OutputFormat) {
        val filteredInventory = inventory.filter {
            filterRelease(filteredIds, it.basicInformation.id)
        }

        when (outputFormat) {
            OutputFormat.Compact, OutputFormat.Wide -> printListingsAsTable(filteredInventory)
            OutputFormat.Json -> printListingsAsJson(filteredInventory)
            OutputFormat.Display -> printListingsAsDisplay(filteredInventory)
        }
    }

    private fun printListingsAsTable(inventory: List<Want>) {
        terminal.println(table {
            header {
                cellBorders = Borders.NONE
                row("Artist", "Title", "Format")
            }
            body {
                cellBorders = Borders.NONE
                inventory.map { it.basicInformation }
                    .forEach { want ->
                        row(
                            addLineBreak(want.artists.joinToString { it.name }),
                            addLineBreak(want.title),
                            want.formats.joinToString { it.formattedOutput() },
                        )
                    }
            }
        })
    }

    private fun printListingsAsJson(inventory: List<Want>) {
        val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        echo(json.encodeToString(inventory))
    }

    private fun printListingsAsDisplay(inventory: List<Want>) {
        terminal.println(table {
            body {
                cellBorders = Borders.NONE

                inventory.forEachIndexed { index, want ->
                    row {
                        cell("-[ Record ${(index + 1)} ]---------------------") {
                            columnSpan = 2
                            align = TextAlign.LEFT
                        }
                    }
                    row("Artist", "| ${want.basicInformation.artists.joinToString { it.name }}")
                    row("Title", "| ${want.basicInformation.title}")
                    row("Format", "| ${want.basicInformation.formats.joinToString { it.formattedOutput() }}")
                    row("Link", "| ${getReleaseLink(want.releaseId)}")
                }
            }
        })
    }

    private fun printMarketplaceListingsGroupedBySeller(
        marketplaceListingsGroupedBySeller: Map<MarketplaceSeller, List<MarketplaceListing>>
    ) {
        when (OutputFormat.from(format)) {
            OutputFormat.Compact, OutputFormat.Wide ->
                printMarketplaceListingsGroupedBySellerAsTable(marketplaceListingsGroupedBySeller)

            OutputFormat.Json ->
                echo("Currently not supported.")

            OutputFormat.Display ->
                printMarketplaceListingsGroupedBySellerAsDisplay(marketplaceListingsGroupedBySeller)
        }
    }

    private fun printMarketplaceListingsGroupedBySellerAsTable(
        marketplaceListingsGroupedBySeller: Map<MarketplaceSeller, List<MarketplaceListing>>
    ) {
        terminal.println(table {
            body {
                cellBorders = Borders.NONE

                marketplaceListingsGroupedBySeller.forEach { (seller, listings) ->
                    row {
                        cell("-[ Seller: ${seller.name} - ${listings.size} item(s)]---------------------") {
                            columnSpan = 2
                            align = TextAlign.LEFT
                        }
                    }

                    row("Title", "Media", "Sleeve", "Price", "Ships from")

                    listings
                        .sortedBy { it.title }
                        .forEach { listing ->
                        row(listing.title, listing.mediaCondition, listing.sleeveCondition, listing.price, listing.shippingLocation)
                    }

                    row()
                }
            }
        })
    }

    private fun printMarketplaceListingsGroupedBySellerAsDisplay(
        marketplaceListingsGroupedBySeller: Map<MarketplaceSeller, List<MarketplaceListing>>
    ) {
        terminal.println(table {
            body {
                cellBorders = Borders.NONE

                marketplaceListingsGroupedBySeller.forEach { (seller, listings) ->
                    row {
                        cell("-[ Seller: ${seller.name} - ${listings.size} item(s)]---------------------") {
                            columnSpan = 2
                            align = TextAlign.LEFT
                        }
                    }

                    listings
                        .sortedBy { it.title }
                        .forEach { listing ->
                        row("Title", "| ${listing.title}")
                        row("Media", "| ${listing.mediaCondition}")
                        row("Sleeve", "| ${listing.sleeveCondition}")
                        row("Price", "| ${listing.price}")
                        row("Ships from:", "| ${listing.shippingLocation}")
                        row()
                    }
                }
            }
        })
    }
}
