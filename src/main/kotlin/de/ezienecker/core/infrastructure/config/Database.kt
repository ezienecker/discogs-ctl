package de.ezienecker.core.infrastructure.config

import de.ezienecker.collection.infrastructure.repository.CachedBasicInformation
import de.ezienecker.collection.infrastructure.repository.CachedCollections
import de.ezienecker.core.configuration.service.ConfigurationService
import de.ezienecker.shop.infrastructure.repository.Listings
import de.ezienecker.wantlist.infrastructure.repository.WantBasicInformation
import de.ezienecker.wantlist.infrastructure.repository.Wants
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

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
