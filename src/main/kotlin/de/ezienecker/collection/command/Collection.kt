package de.ezienecker.collection.command

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.animation.progress.advance
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import de.ezienecker.collection.service.CollectionService
import de.ezienecker.core.command.InventorySubCommand
import de.ezienecker.core.command.OutputFormat
import de.ezienecker.core.infrastructure.discogs.client.ApiException
import de.ezienecker.core.infrastructure.discogs.collection.Release
import de.ezienecker.shop.service.ShopService
import de.ezienecker.wantlist.service.WantlistService
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.json.Json

class Collection(
    private val collectionService: CollectionService,
    private val shopService: ShopService,
    private val wantListService: WantlistService,
    configurationService: de.ezienecker.core.configuration.service.ConfigurationService,
    private val terminal: Terminal,
) : InventorySubCommand<Release>(configurationService = configurationService, terminal = terminal) {

    override fun help(context: Context) = """
            Displays the collection from a user. 
            If no username has been specified, the tool looks for a username in the configuration file (see discogs-ctl config --help for more information).
            
            # List collection for user John-Doe:
            discogs-ctl collection --username John-Doe
            
            # List collection for user John-Doe with more information:
            discogs-ctl collection --username John-Doe --output wide
            
            # List collection for user John-Doe filtered by wantlist entries from user Jane-Roe:
            discogs-ctl collection --username John-Doe --filtered-by-wantlist Jane-Roe
            
            # List collection for user John-Doe filtered by shop entries from user Jane-Roe:
            discogs-ctl collection --username John-Doe --filtered-by-shop Jane-Roe
        """.trimIndent()

    private val fromWantListUsername by option(
        "--filtered-by-wantlist", "-w",
        help = "The username for whose wantlist inventory you are fetching. " +
                "If this option is set, only the entries that appear in the user's wantlist are displayed."
    )

    private val fromShopUsername by option(
        "--filtered-by-shop", "-s",
        help = "The username for whose shop inventory you are fetching " +
                "If this option is set, only the entries that appear in the user's inventory are displayed."
    )

    override fun run(): Unit = runBlocking {
        handleVerboseOption()

        runIfUsernameSpecified { username ->

            launch { progress.execute() }

            progress.update { total = 3 }

            supervisorScope {

                val collection = async { collectionService.listCollectionByUser(username, sortBy, sortOrder) }
                    .also { it.invokeOnCompletion { progress.advance(1) } }
                val filteredIdsFromShop = async { shopService.getIdsFromInventoryReleasesByUser(fromShopUsername) }
                    .also { it.invokeOnCompletion { progress.advance(1) } }
                val filteredIdsFromWantList =
                    async { wantListService.getIdsFromWantlistReleasesByUser(fromWantListUsername) }
                        .also { it.invokeOnCompletion { progress.advance(1) } }

                process(collection.await(), filteredIdsFromShop.await(), filteredIdsFromWantList.await())
            }
        }
    }

    fun process(releases: Result<List<Release>>, filterIdsFromShop: Set<Long>, filterIdsFromWantList: Set<Long>) {
        releases
            .onFailure { printError((it as ApiException).error.message) }
            .onSuccess { printListings(it, filterIdsFromShop + filterIdsFromWantList) }
    }

    override fun printListings(inventory: List<Release>, filteredIds: Set<Long>, outputFormat: OutputFormat) {
        val filteredInventory = inventory.filter {
            filterRelease(filteredIds, it.basicInformation.id)
        }

        when (outputFormat) {
            OutputFormat.Compact, OutputFormat.Wide -> printListingsAsTable(filteredInventory)
            OutputFormat.Json -> printListingsAsJson(filteredInventory)
        }
    }

    private fun printListingsAsTable(inventory: List<Release>) {
        terminal.println(table {
            header {
                cellBorders = Borders.NONE

                row("Artist", "Title", "Format", "Link")
            }
            body {
                cellBorders = Borders.NONE

                inventory.forEach {
                    row(
                        addLineBreak(it.basicInformation.artists.joinToString(", ") { artist -> artist.name }),
                        addLineBreak(it.basicInformation.title),
                        it.basicInformation.formats.map { format ->
                            format.name
                        },
                        addReleaseLink(it.id)
                    )
                }
            }
        })
    }

    private fun printListingsAsJson(inventory: List<Release>) {
        val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        echo(json.encodeToString(inventory))
    }

    private fun addReleaseLink(id: Long): String =
        "https://www.discogs.com/release/$id"
}
