package com.fantasy.football.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.jooq.impl.DefaultConfiguration

class DSLFactory(db: DatabaseConfig) {
    private var config = HikariConfig()

    //    val a  = DSLFactory(config.db).build()
//
//   val record = a.newRecord(PLAYERS, PlayersRecord("test", OffsetDateTime.now()))

    init {
        config.jdbcUrl = db.url
        config.username = db.user
        config.password = db.password
    }

    fun build(): DSLContext {
        val dataSource = HikariDataSource(config)
        val configuration = DefaultConfiguration()
            .set(dataSource)
            .set(SQLDialect.POSTGRES)

        return DSL.using(configuration)
    }
}
