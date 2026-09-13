package dev.metinkale.halalapp.db.table


import dev.metinkale.halalapp.db.entity.Entity
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

sealed class EntityTable<T : Entity>(tableName: String, idName: String = "id") : IdTable<String>(tableName) {
    val idRaw = text(idName)
    override val id = idRaw.entityId()
    override val primaryKey = PrimaryKey(id)

    abstract fun map(row: ResultRow): T
    private val cacheKey = Key<Map<String, T?>>()

    context(transaction: JdbcTransaction)
    fun cacheAll() {
        transaction.putUserData(cacheKey, all().associateBy { it.id })
    }

    open val filter: Op<Boolean>? = null


    open fun findById(id: String): T? = transaction {
        val cache = getUserData(cacheKey)
        val getter: () -> T? =
            {
                selectAll().where {
                    filter?.let { this@EntityTable.id eq id and it } ?: (this@EntityTable.id eq id)
                }.limit(1).map { map(it) }.firstOrNull()
            }

        if (cache == null) getter()
        else cache[id]
    }

    fun all() = transaction { selectAll().map { map(it) } }
}


sealed class CrmTable<T : Entity>(tableName: String, idName: String = "id") : EntityTable<T>(tableName,idName)
