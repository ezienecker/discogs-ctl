package de.ezienecker.shop.command

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import de.ezienecker.de.ezienecker.shared.command.InventorySubCommand
import de.ezienecker.de.ezienecker.wantlist.service.WantlistService
import de.ezienecker.shared.configuration.service.ConfigurationService
import de.ezienecker.shared.discogs.marketplace.Listing
import de.ezienecker.shared.discogs.marketplace.Status
import de.ezienecker.shop.service.ShopService
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Shop(
    private val shopService: ShopService,
    private val wantListService: WantlistService,
    configurationService: ConfigurationService,
    private val terminal: Terminal,
    private val json: Json,
) : InventorySubCommand<Listing>(configurationService = configurationService) {

    override fun help(context: Context) = """
            Displays the shop inventory from a user. 
            If no username has been specified, the tool looks for a username in the configuration file (see discogs-ctl config --help for more information).
            
            # List shop inventory for user John-Doe:
            discogs-ctl shop --username John-Doe
            
            # List shop inventory for user John-Doe with more information:
            discogs-ctl shop --username John-Doe --output wide
            
            # List shop inventory for user John-Doe filtered by wantlist entries from user Jane-Roe:
            discogs-ctl shop --username John-Doe --from-wantlist Jane-Roe
        """.trimIndent()

    private val fromWantListUsername by option(
        "--from-wantlist", "-w",
        help = "The username for whose wantlist inventory you are fetching. " +
                "If this option is set, only the entries that appear in the user's wantlist are displayed."
    )

    override fun run() {
        runBlocking {
            getUsernameForInventory(username)?.let { user ->
                printListings(
                    inventory = shopService.listInventoryByUser(user).filter { it.status == Status.FOR_SALE },
                    idsFromInventoryToFiltering = getIdsFromWantListInventory(fromWantListUsername)
                )
            } ?: echo("Please provide a valid username.")
        }
    }

    private suspend fun getIdsFromWantListInventory(fromWantListUsername: String?) =
        fromWantListUsername?.let { wantListUsername ->
            wantListService.listInventoryByUser(wantListUsername).map { it.basicInformation.id }.toSet()
        } ?: emptySet()

    override fun printListingsAsTable(inventory: List<Listing>, idsFromExternalInventoryToFiltering: Set<Long>) {
        terminal.println(table {
            header {
                cellBorders = Borders.NONE

                row("Artist", "Title", "Media", "Sleeve", "Format", "Price")
            }
            body {
                cellBorders = Borders.NONE

                inventory
                    .filter {
                        filterRelease(idsFromExternalInventoryToFiltering, it.release.id)
                    }
                    .forEach {
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

    override fun printListingsAsJson(inventory: List<Listing>, idsFromExternalInventoryToFiltering: Set<Long>) {
        terminal.println(
            json.encodeToString(
                inventory.filter {
                    filterRelease(idsFromExternalInventoryToFiltering, it.release.id)
                }
            ))
    }
}
