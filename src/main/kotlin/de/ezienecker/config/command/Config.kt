package de.ezienecker.config.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.pair
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import de.ezienecker.config.service.ConfigService
import de.ezienecker.config.service.ConfigService.Companion.ACCESS_TOKEN
import de.ezienecker.config.service.ConfigService.Companion.KEY
import de.ezienecker.config.service.ConfigService.Companion.SECRET
import de.ezienecker.config.service.ConfigService.Companion.USERNAME
import de.ezienecker.de.ezienecker.config.model.AccessToken
import de.ezienecker.de.ezienecker.config.model.Key
import de.ezienecker.de.ezienecker.config.model.Secret
import de.ezienecker.de.ezienecker.config.model.Username

class Config : NoOpCliktCommand(
    help = """
    Modify discogsconfig file using subcommands like "discogsctl config set username my-username".
    
    The configuration file is located under \$\{HOME}/.discogsctl/config.
""".trimIndent()
) {

    class Set(
        private val service: ConfigService,
        private val terminal: Terminal
    ) : CliktCommand(
        help = """
            discogsctl config set PROPERTY_NAME PROPERTY_VALUE
            
            Properties: 
            - `username` (Default username whose collection, inventory and want list is used.), 
            - `key` (Credentials used for authentication and must be used in combination with `secret`.),
            - `secret` (Credentials used for authentication and must be used in combination with `key`.) , 
            - `accessToken` (Credentials used for authentication.) 
        """.trimIndent()
    ) {

        private val propertyPair by argument().pair()

        override fun run() = when (propertyPair.first) {
            USERNAME -> service.configureDefaultUser(Username(propertyPair.second))
            KEY -> service.configureKey(Key(propertyPair.second))
            SECRET -> service.configureSecret(Secret(propertyPair.second))
            ACCESS_TOKEN -> service.configureAccessToken(AccessToken(propertyPair.second))
            else -> terminal.println(TextColors.brightRed("${propertyPair.first} is an unknown property"))
        }
    }

    class View(
        private val service: ConfigService,
        private val terminal: Terminal
    ) : CliktCommand(
        help = """
        Display discogsconfig settings.
    """.trimIndent()
    ) {

        override fun run() {
            val properties = service.getConfiguration()

            if (properties.isEmpty()) {
                terminal.println("Nothing is currently configured")
            } else {
                terminal.println(table {
                    header { row("Key", "Value") }
                    body {
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