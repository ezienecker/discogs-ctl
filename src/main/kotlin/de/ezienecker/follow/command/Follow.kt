@file:OptIn(DelicateCoroutinesApi::class)

package de.ezienecker.follow.command

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.table
import com.github.ajalt.mordant.terminal.Terminal
import de.ezienecker.core.command.OutputFormat
import de.ezienecker.core.configuration.model.Username
import de.ezienecker.core.infrastructure.database.DatabaseException
import de.ezienecker.follow.service.FollowDto
import de.ezienecker.follow.service.FollowService
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

class Follow(
    private val followService: FollowService,
    private val terminal: Terminal,
) : CliktCommand() {

    val verbose by option(
        names = arrayOf("--verbose", "-v"),
        help = "Make the operation more talkative",
    )
        .flag(default = false)

    val format by option(
        names = arrayOf("--output", "-o"),
        help = "Output format. One of: 'compact', 'display'. 'compact' is default",
    )
        .choice("compact", "display")
        .default("compact")

    @Suppress("unused")
    val addInstance: Add by lazy { this.Add() }

    @Suppress("unused")
    val removeInstance: Remove by lazy { this.Remove() }

    override fun help(context: Context) = """
        This command allows you to manage the list of followed users.${"\u0085"}
        You can add or remove users from the list of followed users, and you can also list all followed users.
        
        # List followed users:${"\u0085"}
        discogs-ctl follow
        
        # List followed users in display format:${"\u0085"}
        discogs-ctl follow --output display
        
        # Add user John-Doe to the list of followed users with tags 'shop' and 'wantlist':${"\u0085"}
        discogs-ctl follow add John-Doe --tags shop --tags wantlist
        
        # Remove user John-Doe from the list of followed users:${"\u0085"}
        discogs-ctl follow remove John-Doe
    """.trimIndent()

    override val invokeWithoutSubcommand = true

    override fun run(): Unit = runBlocking {
        handleVerboseOption(verbose)

        if (currentContext.invokedSubcommand == null) {
            process(followService.list(null))
        }
    }

    private fun process(follows: Result<List<FollowDto>>) {
        follows
            .onFailure { printError((it as DatabaseException).error.message) }
            .onSuccess { printFollows(it) }
    }

    fun printError(message: String) {
        echo(message)
    }

    private fun printFollows(follows: List<FollowDto>) {
        val outputFormat = OutputFormat.from(format)
        when (outputFormat) {
            OutputFormat.Compact -> {
                printFollowsAsTable(follows)
            }

            OutputFormat.Display -> {
                printFollowsAsDisplay(follows)
            }

            else -> {
                echo(
                    "Format: [$format] currently not supported for follows. " +
                            "Supported formats are: 'compact', 'display'."
                )
            }
        }
    }

    private fun printFollowsAsTable(follows: List<FollowDto>) {
        terminal.println(table {
            header {
                cellBorders = Borders.NONE

                row("Username", "Tags")
            }
            body {
                cellBorders = Borders.NONE

                follows.forEach {
                    row(it.username, it.tags.joinToString(", "))
                }
            }
        })
    }

    private fun printFollowsAsDisplay(follows: List<FollowDto>) {
        terminal.println(table {
            body {
                cellBorders = Borders.NONE

                follows.forEachIndexed { index, follow ->
                    row {
                        cell("-[ Record ${(index + 1)} ]---------------------") {
                            columnSpan = 2
                            align = TextAlign.LEFT
                        }
                    }
                    row("Username", "| ${follow.username}")
                    row("Tags", "| ${follow.tags.joinToString(", ")}")
                }
            }
        })
    }

    inner class Add : CliktCommand() {

        private val allowedTags = listOf("seller", "collection", "wantlist")

        val verbose by option(
            names = arrayOf("--verbose", "-v"),
            help = "Make the operation more talkative",
        )
            .flag(default = false)

        val username by argument(
            help = "The username of the user to follow"
        )

        val tags by option(
            names = arrayOf("--tags", "-t"),
            help = """
                Optional tags to categorize the followed user. 
                Multiple tags can be specified by repeating the option multiple times, e.g. --tags shop --tags wantlist.
                Allowed tags are: 'shop', 'collection', 'wantlist'.
            """.trimIndent(),
        )
            .multiple(allowedTags)

        override fun help(context: Context) = """
            This command allows you to add a user to the list of followed users.
        
            # Add user John-Doe to the list of followed users. If no tags are specified, the user is added with all tags:${"\u0085"}
            discogs-ctl follow add John-Doe
            
            # Add user John-Doe to the list of followed users with tags 'shop' and 'wantlist':${"\u0085"}
            discogs-ctl follow add John-Doe --tags shop --tags wantlist
        """.trimIndent()

        override fun run(): Unit = runBlocking {
            this@Follow.handleVerboseOption(verbose)
            // validate tags
            this@Follow.followService.add(
                username = Username(username),
                tags = tags.ifEmpty { allowedTags },
            )
        }
    }

    inner class Remove : CliktCommand() {

        val verbose by option(
            names = arrayOf("--verbose", "-v"),
            help = "Make the operation more talkative",
        )
            .flag(default = false)

        val username by argument(
            help = "The username of the user to unfollow"
        )

        override fun help(context: Context) = """
            This command allows you to remove a user from the list of followed users.
        
            # Remove user John-Doe from the list of followed users:${"\u0085"}
            discogs-ctl follow remove John-Doe
        """.trimIndent()

        override fun run(): Unit = runBlocking {
            this@Follow.handleVerboseOption(verbose)
            this@Follow.followService.remove(Username(username))
        }
    }

    fun handleVerboseOption(verbose: Boolean) {
        if (verbose) {
            (LoggerFactory.getILoggerFactory() as LoggerContext).getLogger("de.ezienecker").apply {
                level = Level.TRACE
            }
        }
    }
}
