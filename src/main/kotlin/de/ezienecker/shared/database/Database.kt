package de.ezienecker.shared.database

import de.ezienecker.shared.configuration.service.ConfigurationService
import de.ezienecker.shared.database.schema.CachedBasicInformation
import de.ezienecker.shared.database.schema.CachedCollections
import de.ezienecker.shared.database.schema.Listings
import de.ezienecker.shared.database.schema.WantBasicInformation
import de.ezienecker.shared.database.schema.Wants
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun configureDatabaseConnection() {
    Database.connect(
        url = "jdbc:sqlite:${ConfigurationService.APPLICATION_FOLDER.toAbsolutePath()}/data.db",
        driver = "org.sqlite.JDBC",
    )
}

fun setupSchema() = transaction {
    SchemaUtils.create(
        CachedCollections,
        CachedBasicInformation,
        Wants,
        WantBasicInformation,
        Listings
    )
}
