package de.ezienecker.config.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.pair
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import de.ezienecker.core.configuration.model.Token
import de.ezienecker.core.configuration.model.Username
import de.ezienecker.core.configuration.service.ConfigurationService
import de.ezienecker.core.configuration.service.ConfigurationService.Companion.TOKEN
import de.ezienecker.core.configuration.service.ConfigurationService.Companion.USERNAME

class Config : NoOpCliktCommand() {

    override fun help(context: Context) = """
        Display or modify discogs-ctl config file.
        
        The configuration file is located under ${'$'}{HOME}/.discogsctl/
        """.trimIndent()

    class Set(
        private val service: ConfigurationService,
        private val terminal: Terminal
    ) : CliktCommand() {

        override fun help(context: Context) = """
            Set an individual value in a discogs-ctl config file.
            
            # Set default username to John-Doe:
            discogs-ctl config set username John-Doe
            
            # Set token so that the requests to the discogs server are authenticated:
            discogs-ctl config set username John-Doe
            
            Properties:
            
            * `$USERNAME` (Default username whose collection, inventory and wantlist is used.), 
            
            * `$TOKEN` (Token used for authentication.),
            """.trimIndent()

        private val propertyPair by argument().pair()

        override fun run() = when (propertyPair.first) {
            USERNAME -> service.configureDefaultUser(Username(propertyPair.second))
            TOKEN -> service.configureToken(Token(propertyPair.second))
            else -> terminal.println(TextColors.brightRed("${propertyPair.first} is an unknown property"))
        }
    }

    class View(
        private val service: ConfigurationService,
        private val terminal: Terminal
    ) : CliktCommand() {

        override fun help(context: Context) = """
            Displays the discogs-ctl config file.
            
            # Show all configured properties:
            discogs-ctl config view
            """.trimIndent()

        override fun run() {
            val properties = service.getConfiguration()

            if (properties.isEmpty()) {
                terminal.println("Nothing is currently configured")
            } else {
                terminal.println(table {
                    header {
                        cellBorders = Borders.NONE

                        row("Key", "Value")
                    }
                    body {
                        cellBorders = Borders.NONE

                        properties
                            .toSortedMap()
                            .forEach { (key, value) ->
                                row(key, value)
                            }
                    }
                })
            }
        }
    }
}
