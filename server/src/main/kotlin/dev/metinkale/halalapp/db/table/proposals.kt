package dev.metinkale.halalapp.db.table

import dev.metinkale.halalapp.ui.crm.getCrmFields
import dev.metinkale.halalapp.db.entity.Proposal
import dev.metinkale.halalapp.db.entity.Entity
import dev.metinkale.halalapp.domain.RequestStatus
import io.ktor.http.Parameters
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.updateReturning
import org.jetbrains.exposed.v1.jdbc.upsert
import org.jetbrains.exposed.v1.json.jsonb
import java.time.Instant
import kotlin.text.orEmpty


object ProposalsTable : EntityTable<Proposal>("proposals") {
    val entityType = varchar("entity_type", 255)

    val json = jsonb<Map<String, String>>("json", Json.Default)
    val note = text("note")
    val creator = text("creator")
    val status = enumerationByName("status", 50, RequestStatus::class)
    val date = timestamp("date")

    override fun map(row: ResultRow) = Proposal(
        id = row[id].value,
        entityType = row[entityType],
        json = row[json],
        note = row[note],
        creator = row[creator],
        status = row[status],
        date = row[date]
    )

    fun <T : Entity> approveRequest(crmTable: CrmTable<T>, proposalId: String) = transaction {
        updateReturning(
            returning = listOf(json),
            where = { (ProposalsTable.id eq proposalId) and (status eq RequestStatus.PENDING) and (entityType eq crmTable.tableName) }) {
            it[ProposalsTable.id] = proposalId
            it[ProposalsTable.status] = RequestStatus.APPROVED
        }.first().let { cr ->
            val json = cr[json]
            if (json["id"] == null)
                crmTable.insert {
                    crmTable.getCrmFields().forEach { field ->
                        field.write(it, json[field.name]!!)
                    }
                }
            else
                crmTable.upsert(crmTable.id) {
                    crmTable.getCrmFields().forEach { field ->
                        field.write(it, json[field.name]!!)
                    }
                }
        }
    }


    fun <T : Entity> newRequest(crmTable: CrmTable<T>, params: Parameters, creator: String) =
        transaction {
            ProposalsTable.insert {
                it[ProposalsTable.entityType] = crmTable.tableName
                it[ProposalsTable.json] = crmTable.getCrmFields().associate { it.name to params[it.name].orEmpty() }
                it[ProposalsTable.date] = Instant.now()
                it[ProposalsTable.note] = params["note"]!!
                it[ProposalsTable.status] = RequestStatus.PENDING
                it[ProposalsTable.creator] = creator
            }
        }

    fun <T : Entity> findOpenRequests(crmTable: CrmTable<T>) = transaction {
        ProposalsTable.selectAll()
            .where { (entityType eq crmTable.tableName).and(status eq RequestStatus.PENDING) }
            .map { map(it) }
    }

    fun <T : Entity> numberOfOpenRequests(crmTable: CrmTable<T>) = transaction {
        ProposalsTable.selectAll()
            .where { (entityType eq crmTable.tableName).and(status eq RequestStatus.PENDING) }
            .count()
    }

    fun rejectRequest(id: String) = transaction {
        ProposalsTable.update({ ProposalsTable.id eq id }) {
            it[status] = RequestStatus.REJECTED
        }
    }
}