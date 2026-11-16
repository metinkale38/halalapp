package dev.metinkale.halalapp.db.table

import dev.metinkale.halalapp.ui.crm.getCrmFields
import dev.metinkale.halalapp.ui.crm.paramsToValues
import dev.metinkale.halalapp.db.entity.Proposal
import dev.metinkale.halalapp.db.entity.Entity
import dev.metinkale.halalapp.domain.RequestStatus
import io.ktor.http.Parameters
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert
import org.jetbrains.exposed.v1.json.jsonb
import java.time.Instant
import kotlin.text.orEmpty


object ProposalsTable : EntityTable<Proposal>("proposals") {
    val entityType = varchar("entity_type", 255)

    val json = jsonb<Map<String, String>>("json", Json.Default)
    val oldJson = jsonb<Map<String, String>>("old_json", Json.Default).nullable()
    val note = text("note")
    val creator = text("creator")
    val status = enumerationByName("status", 50, RequestStatus::class)
    val date = timestamp("date")

    override fun map(row: ResultRow) = Proposal(
        id = row[id].value,
        entityType = row[entityType],
        json = row[json],
        oldJson = row[oldJson],
        note = row[note],
        creator = row[creator],
        status = row[status],
        date = row[date]
    )

    fun <T : Entity> approveRequest(crmTable: CrmTable<T>, proposalId: String) = transaction {
        val proposal = ProposalsTable.selectAll().where {
            (ProposalsTable.id eq proposalId) and
                    (status eq RequestStatus.PENDING) and
                    (entityType eq crmTable.tableName)
        }.singleOrNull() ?: throw IllegalArgumentException("Vorschlag nicht gefunden oder bereits bearbeitet.")

        val json = proposal[json]
        val entityId = (json["id"] ?: json["child_id"])?.takeIf { it.isNotBlank() }
        val oldValues = entityId?.let { id ->
            crmTable.selectAll().where { crmTable.id eq id }.singleOrNull()?.let { row ->
                crmTable.getCrmFields().associate { field -> field.name to field.read(row) }
            }
        }

        val claimed = ProposalsTable.update({
            (ProposalsTable.id eq proposalId) and (status eq RequestStatus.PENDING)
        }) {
            it[status] = RequestStatus.APPROVED
        }
        if (claimed == 0) throw IllegalArgumentException("Vorschlag wurde bereits bearbeitet.")

        if (entityId == null)
            crmTable.insert {
                crmTable.getCrmFields().forEach { field ->
                    field.write(it, json[field.name].orEmpty())
                }
            }
        else
            crmTable.upsert(crmTable.id) {
                crmTable.getCrmFields().forEach { field ->
                    field.write(it, json[field.name].orEmpty())
                }
            }

        ProposalsTable.update({ ProposalsTable.id eq proposalId }) {
            it[oldJson] = oldValues
        }
        markProductsForReprocessing(crmTable, json)
    }


    fun <T : Entity> newRequest(crmTable: CrmTable<T>, params: Parameters, creator: String) =
        transaction {
            ProposalsTable.insert {
                it[ProposalsTable.entityType] = crmTable.tableName
                it[ProposalsTable.json] = crmTable.paramsToValues(params)
                it[ProposalsTable.date] = Instant.now()
                it[ProposalsTable.note] = params["note"]!!
                it[ProposalsTable.status] = RequestStatus.PENDING
                it[ProposalsTable.creator] = creator
            }
        }

    fun recordDirectChange(
        entityType: String,
        oldValues: Map<String, String>?,
        newValues: Map<String, String>,
        creator: String,
        note: String,
    ) = transaction {
        ProposalsTable.insert {
            it[ProposalsTable.entityType] = entityType
            it[ProposalsTable.json] = newValues
            it[ProposalsTable.oldJson] = oldValues
            it[ProposalsTable.note] = note
            it[ProposalsTable.creator] = creator
            it[ProposalsTable.status] = RequestStatus.APPROVED
            it[ProposalsTable.date] = Instant.now()
        }
    }

    fun findHistory(entityType: String, limit: Int = 100) = transaction {
        ProposalsTable.selectAll().where {
            (ProposalsTable.entityType eq entityType) and
                    (status inList listOf(RequestStatus.APPROVED, RequestStatus.REJECTED))
        }.orderBy(date to SortOrder.DESC).limit(limit).map { map(it) }
    }

    fun <T : Entity> findHistory(crmTable: CrmTable<T>, limit: Int = 100) = findHistory(crmTable.tableName, limit)

    fun <T : Entity> findOpenRequests(crmTable: CrmTable<T>) = findOpenRequests(crmTable.tableName)

    fun findOpenRequests(entityType: String) = transaction {
        ProposalsTable.selectAll()
            .where { (ProposalsTable.entityType eq entityType).and(status eq RequestStatus.PENDING) }
            .orderBy(date to SortOrder.DESC)
            .map { map(it) }
    }

    fun <T : Entity> numberOfOpenRequests(crmTable: CrmTable<T>) = numberOfOpenRequests(crmTable.tableName)

    fun numberOfOpenRequests(entityType: String) = transaction {
        ProposalsTable.selectAll()
            .where { (ProposalsTable.entityType eq entityType).and(status eq RequestStatus.PENDING) }
            .count()
    }

    fun rejectRequest(id: String) = transaction {
        ProposalsTable.update({ ProposalsTable.id eq id }) {
            it[status] = RequestStatus.REJECTED
        }
    }
}