package de.ezienecker.wantlist.command

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import de.ezienecker.shared.command.InventorySubCommand
import de.ezienecker.shared.configuration.service.ConfigurationService
import de.ezienecker.shared.discogs.wantlist.Want
import de.ezienecker.shop.service.ShopService
import de.ezienecker.wantlist.service.WantlistService
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

class Wantlist(
    private val shopService: ShopService,
    private val wantListService: WantlistService,
    configurationService: ConfigurationService,
    private val terminal: Terminal,
    private val json: Json,
) : InventorySubCommand<Want>(
    name = "wantlist",
    configurationService = configurationService
) {

    override fun help(context: Context) = """
        Displays the wantlist inventory from a user.
        If no username has been specified, the tool looks for a username in the configuration file (see discogs-ctl config --help for more information).
        
        # List wantlist inventory for user John-Doe:
        discogs-ctl wantlist --username John-Doe
        
        # List wantlist inventory for user John-Doe with more information:
        discogs-ctl wantlist --username John-Doe --output wide
        
        # List wantlist inventory for user John-Doe filtered by shop entries from user Jane-Roe:
        discogs-ctl wantlist --username John-Doe --filtered-by-shop-from Jane-Roe
        """.trimIndent()

    private val fromShopUsername by option(
        "--filtered-by-shop-from", "-s",
        help = "The username for whose shop inventory you are fetching " +
                "If this option is set, only the entries that appear in the user's inventory are displayed."
    )

    override fun run() {
        runBlocking {
            getUsernameForInventory(username)?.let {
                printListings(
                    inventory = wantListService.listInventoryByUser(it),
                    idsFromInventoryToFiltering = getIdsFromShopInventory(fromShopUsername),
                )
            } ?: echo("Please provide a valid username.")
        }
    }

    private suspend fun getIdsFromShopInventory(fromShopUsername: String?) = fromShopUsername?.let { user ->
        shopService.listInventoryByUser(user).map { it.release.id }.toSet()
    } ?: emptySet()

    override fun printListingsAsTable(inventory: List<Want>, filteredIds: Set<Long>) {
        terminal.println(table {
            header {
                cellBorders = Borders.NONE
                row("Artist", "Title", "Format")
            }
            body {
                cellBorders = Borders.NONE
                inventory.map { it.basicInformation }
                    .filter { filterRelease(filteredIds, it.id) }
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

    override fun printListingsAsJson(inventory: List<Want>, filteredIds: Set<Long>) {
        terminal.println(
            json.encodeToString(
                inventory.map { it.basicInformation }.filter {
                    filterRelease(filteredIds, it.id)
                }
            ))
    }
}
