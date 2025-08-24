package de.ezienecker.wantlist.command

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import de.ezienecker.core.command.InventorySubCommand
import de.ezienecker.core.configuration.service.ConfigurationService
import de.ezienecker.core.infrastructure.discogs.client.ApiException
import de.ezienecker.core.infrastructure.discogs.wantlist.Want
import de.ezienecker.shop.service.ShopService
import de.ezienecker.wantlist.service.WantlistService
import kotlinx.coroutines.runBlocking

class Wantlist(
    private val shopService: ShopService,
    private val wantListService: WantlistService,
    configurationService: ConfigurationService,
    private val terminal: Terminal,
) : InventorySubCommand<Want>(
    name = "wantlist",
    configurationService = configurationService,
    terminal = terminal,
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
            getUsernameForInventory(username)?.let { user ->
                wantListService.listWantsByUser(user)
                    .onFailure { printError((it as ApiException).error.message) }
                    .onSuccess { wants ->
                        printListings(
                            entries = wants,
                            filteredIds = shopService.getIdsFromInventoryReleasesByUser(
                                fromShopUsername
                            )
                        )
                    }
            } ?: echo("Please provide a valid username.")
        }
    }

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
}
