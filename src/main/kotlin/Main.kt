package de.ezienecker

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.sources.PropertiesValueSource
import com.github.ajalt.mordant.terminal.Terminal
import de.ezienecker.config.command.Config
import de.ezienecker.config.service.ConfigServiceImpl

fun main(args: Array<String>) {
    val terminal = Terminal()

    val configService = ConfigServiceImpl()
    val setConfig = Config.Set(configService, terminal)
    val viewConfig = Config.View(configService, terminal)

    DiscogsCtl()
        .subcommands(
            Config()
                .subcommands(
                    setConfig,
                    viewConfig
                )
        )
        .main(args)
}

class DiscogsCtl : NoOpCliktCommand() {
    init {
        context {
            valueSource = PropertiesValueSource.from(
                "${System.getProperty("user.home")}/.discogsctl/config.properties"
            )
        }
    }
}