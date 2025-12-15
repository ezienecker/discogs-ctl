package de.ezienecker.wantlist.command

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.animation.progress.advance
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import de.ezienecker.core.command.InventorySubCommand
import de.ezienecker.core.command.OutputFormat
import de.ezienecker.core.configuration.service.ConfigurationService
import de.ezienecker.core.infrastructure.discogs.client.ApiException
import de.ezienecker.core.infrastructure.discogs.wantlist.Want
import de.ezienecker.shop.service.ShopService
import de.ezienecker.wantlist.service.WantlistService
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.json.Json

class Wantlist(
    private val shopService: ShopService,
    private val wantListService: WantlistService,
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
        """.trimIndent()

    private val fromShopUsername by option(
        "--filtered-by-shop", "-s",
        help = "The username for whose shop inventory you are fetching " +
                "If this option is set, only the entries that appear in the user's inventory are displayed."
    )

    override fun run(): Unit = runBlocking {
        handleVerboseOption()

        runIfUsernameSpecified { username ->

            launch { progress.execute() }

            progress.update { total = 2 }

            supervisorScope {
                val wants = async { wantListService.listWantsByUser(username, sortBy, sortOrder) }
                    .also { it.invokeOnCompletion { progress.advance(1) } }
                val filterIds = async { shopService.getIdsFromInventoryReleasesByUser(fromShopUsername) }
                    .also { it.invokeOnCompletion { progress.advance(1) } }

                process(wants.await(), filterIds.await())
            }
        }
    }

    fun process(wants: Result<List<Want>>, filterIds: Set<Long>) {
        wants
            .onFailure { printError((it as ApiException).error.message) }
            .onSuccess { printListings(it, filterIds) }
    }

    override fun printListings(inventory: List<Want>, filteredIds: Set<Long>, outputFormat: OutputFormat) {
        val filteredInventory = inventory.filter {
            filterRelease(filteredIds, it.basicInformation.id)
        }

        when (outputFormat) {
            OutputFormat.Compact, OutputFormat.Wide -> printListingsAsTable(filteredInventory)
            OutputFormat.Json -> printListingsAsJson(filteredInventory)
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
}
