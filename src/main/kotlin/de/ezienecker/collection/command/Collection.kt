package de.ezienecker.collection.command

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import de.ezienecker.collection.service.CollectionService
import de.ezienecker.shared.command.InventorySubCommand
import de.ezienecker.shared.configuration.service.ConfigurationService
import de.ezienecker.shared.discogs.client.ApiException
import de.ezienecker.shared.discogs.collection.Release
import de.ezienecker.shop.service.ShopService
import de.ezienecker.wantlist.service.WantlistService
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

class Collection(
    private val collectionService: CollectionService,
    private val shopService: ShopService,
    private val wantListService: WantlistService,
    configurationService: ConfigurationService,
    private val terminal: Terminal,
    private val json: Json,
) : InventorySubCommand<Release>(configurationService = configurationService, terminal = terminal) {

    override fun help(context: Context) = """
            Displays the collection from a user. 
            If no username has been specified, the tool looks for a username in the configuration file (see discogs-ctl config --help for more information).
            
            # List collection for user John-Doe:
            discogs-ctl collection --username John-Doe
            
            # List collection for user John-Doe with more information:
            discogs-ctl collection --username John-Doe --output wide
            
            # List collection for user John-Doe filtered by wantlist entries from user Jane-Roe:
            discogs-ctl collection --username John-Doe --filtered-by-wantlist-from Jane-Roe
            
            # List collection for user John-Doe filtered by shop entries from user Jane-Roe:
            discogs-ctl collection --username John-Doe --filtered-by-shop-from Jane-Roe
        """.trimIndent()

    private val fromWantListUsername by option(
        "--filtered-by-wantlist-from", "-w",
        help = "The username for whose wantlist inventory you are fetching. " +
                "If this option is set, only the entries that appear in the user's wantlist are displayed."
    )

    private val fromShopUsername by option(
        "--filtered-by-shop-from", "-s",
        help = "The username for whose shop inventory you are fetching " +
                "If this option is set, only the entries that appear in the user's inventory are displayed."
    )

    override fun run() {
        runBlocking {
            getUsernameForInventory(username)?.let { user ->
                collectionService.listCollectionByUser(user)
                    .onFailure { printError((it as ApiException).error.message) }
                    .onSuccess {
                        printListings(
                            entries = it,
                            filteredIds =
                                shopService.getIdsFromInventoryReleasesByUser(fromShopUsername) +
                                        wantListService.getIdsFromWantlistReleasesByUser(fromWantListUsername)
                        )
                    }


            } ?: echo("Please provide a valid username.")
        }
    }

    override fun printListingsAsTable(inventory: List<Release>, filteredIds: Set<Long>) {
        terminal.println(table {
            header {
                cellBorders = Borders.NONE

                row("Artist", "Title", "Format", "Link")
            }
            body {
                cellBorders = Borders.NONE

                inventory
                    .filter {
                        filterRelease(filteredIds, it.basicInformation.id)
                    }
                    .forEach {
                        row(
                            addLineBreak(it.basicInformation.artists.joinToString(", ") { artist -> artist.name }),
                            addLineBreak(it.basicInformation.title),
                            it.basicInformation.formats.map { format ->
                                "${format.name}, ${format.text}"
                            },
                            addReleaseLink(it.id)
                        )
                    }
            }
        })
    }

    override fun printListingsAsJson(inventory: List<Release>, filteredIds: Set<Long>) {
        TODO("Not yet implemented")
    }

    private fun addReleaseLink(id: Long): String =
        "https://www.discogs.com/release/$id"
}
