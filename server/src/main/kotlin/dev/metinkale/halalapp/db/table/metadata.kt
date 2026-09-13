package dev.metinkale.halalapp.db.table

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert


object MetadataTable : IdTable<String>("metadata") {
    val key = text("key")
    val value = text("value")
    override val id: Column<EntityID<String>> = key.entityId()


    fun getValue(key: String) =
        transaction { select(value).where { this@MetadataTable.key eq key }.firstOrNull()?.get(value) }

    fun setValue(key: String, value: String) = transaction {
        upsert(this@MetadataTable.key) {
            it[this.key] = key
            it[this.value] = value
        }
    }
}

