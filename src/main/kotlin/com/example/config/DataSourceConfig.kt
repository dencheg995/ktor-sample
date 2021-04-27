package com.example.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jooq.CloseableDSLContext
import org.jooq.impl.DSL
import javax.sql.DataSource


data class ApplicationProperties(
    val databaseUrl: String,
    val dbUser: String,
    val dbPassword: String,
)


data class AppConfig(
    var properties: ApplicationProperties = ApplicationProperties(
        databaseUrl = "jdbc:postgresql://localhost:5432/ktorExample",
        dbUser = "root",
        dbPassword = "root"
    )
)

fun initDb(config: AppConfig): DataSource {
    val properties = config.properties;
    val dataSource: DataSource = HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = properties.databaseUrl
            username = properties.dbUser
            password = properties.dbPassword
        })
    val flyWay = Flyway.configure().dataSource(dataSource).load();
    flyWay.migrate();
    return dataSource
}

fun getDLS(config: AppConfig): CloseableDSLContext = DSL.using(
    config.properties.databaseUrl,
    config.properties.dbUser,
    config.properties.dbPassword
)