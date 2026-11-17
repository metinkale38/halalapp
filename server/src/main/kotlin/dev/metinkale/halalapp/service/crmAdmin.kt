package dev.metinkale.halalapp.service

import dev.metinkale.halalapp.db.entity.Entity
import dev.metinkale.halalapp.db.table.CategoryMappingTable
import dev.metinkale.halalapp.db.table.CategoriesTable
import dev.metinkale.halalapp.db.table.ClassificationsTable
import dev.metinkale.halalapp.db.table.CrmTable
import dev.metinkale.halalapp.db.table.IngredientsTable
import dev.metinkale.halalapp.db.table.ProcessedProductsTable
import dev.metinkale.halalapp.db.table.ProductInsightsTable
import dev.metinkale.halalapp.db.table.ProductParentTable
import dev.metinkale.halalapp.db.table.ProductsCustomTable
import dev.metinkale.halalapp.db.table.ProductsOffTable
import dev.metinkale.halalapp.db.table.ProposalsTable
import dev.metinkale.halalapp.domain.Status
import dev.metinkale.halalapp.db.table.markProductsForReprocessing
import dev.metinkale.halalapp.ui.crm.CrmLens
import dev.metinkale.halalapp.ui.crm.getCrmFields
import dev.metinkale.halalapp.ui.crm.readValues
import org.jetbrains.exposed.v1.core.CustomFunction
import org.jetbrains.exposed.v1.core.IColumnType
import org.jetbrains.exposed.v1.core.IntegerColumnType
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.statements.StatementType
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.security.MessageDigest
import java.time.LocalDate
import kotlin.math.ceil

data class CrmPageData(
    val entries: List<Pair<String, Map<String, String>>>,
    val totalCount: Int,
    val page: Int,
    val totalPages: Int,
)

object CrmAdminService {

    fun <T : Entity> page(
        table: CrmTable<T>,
        query: String,
        requestedPage: Int,
        pageSize: Int,
    ): CrmPageData = transaction {
        ClassificationsTable.cacheAll()
        if (table === ProductParentTable) {
            return@transaction parentPage(query, requestedPage, pageSize)
        }

        val search = searchOp(table, query)
        var queryBuilder = table.selectAll()
        if (search != null) queryBuilder = queryBuilder.where { search }

        val totalCount = queryBuilder.count().toInt()
        val totalPages = if (totalCount == 0) 1 else ceil(totalCount.toDouble() / pageSize).toInt()
        val page = requestedPage.coerceIn(1, totalPages)
        val entries = queryBuilder
            .orderBy(table.idRaw to SortOrder.ASC)
            .limit(pageSize)
            .offset(((page - 1) * pageSize).toLong())
            .map { row -> row[table.id].value to table.readValues(row) }

        CrmPageData(entries, totalCount, page, totalPages)
    }

    private fun JdbcTransaction.parentPage(query: String, requestedPage: Int, pageSize: Int): CrmPageData {
        val trimmed = query.trim()
        val needle = "%${trimmed.lowercase()}%"
        val filter = if (trimmed.isNotBlank()) {
            """
            WHERE lower(pp.parent_id) LIKE ?
               OR lower(coalesce(p.name, '')) LIKE ?
               OR EXISTS (
                    SELECT 1
                    FROM product_parent child
                    WHERE child.parent_id = pp.parent_id
                      AND lower(child.child_id) LIKE ?
               )
            """.trimIndent()
        } else ""
        val from = """
            FROM product_parent pp
            LEFT JOIN (
                SELECT DISTINCT ON (id) id, name
                FROM (
                    SELECT id, name, 0 AS source_priority FROM products_custom
                    UNION ALL
                    SELECT id, name, 1 AS source_priority FROM products_off
                ) src
                ORDER BY id, source_priority
            ) p ON p.id = pp.parent_id
            $filter
            GROUP BY pp.parent_id, p.name
        """.trimIndent()
        val filterArgs = if (trimmed.isNotBlank()) {
            listOf<Pair<IColumnType<*>, Any>>(
                TextColumnType() to needle,
                TextColumnType() to needle,
                TextColumnType() to needle,
            )
        } else emptyList()

        val totalCount = exec(
            "SELECT count(*) FROM (SELECT pp.parent_id $from) grouped",
            filterArgs,
            StatementType.SELECT,
        ) { rs -> if (rs.next()) rs.getInt(1) else 0 } ?: 0
        val totalPages = if (totalCount == 0) 1 else ceil(totalCount.toDouble() / pageSize).toInt()
        val page = requestedPage.coerceIn(1, totalPages)
        val offset = (page - 1) * pageSize

        val entries = exec(
            """
            SELECT pp.parent_id,
                   p.name AS parent_name,
                   string_agg(pp.child_id, ', ' ORDER BY pp.child_id) AS child_ids
            $from
            ORDER BY pp.parent_id
            LIMIT ? OFFSET ?
            """.trimIndent(),
            filterArgs + listOf<Pair<IColumnType<*>, Any>>(
                IntegerColumnType() to pageSize,
                IntegerColumnType() to offset,
            ),
            StatementType.SELECT,
        ) { rs ->
            buildList {
                while (rs.next()) {
                    val parentId = rs.getString("parent_id")
                    add(parentId to mapOf(
                        "parent_id" to parentId,
                        "parent_name" to (rs.getString("parent_name") ?: "Unbekannt"),
                        "child_ids" to (rs.getString("child_ids") ?: ""),
                    ))
                }
            }
        } ?: emptyList()

        return CrmPageData(entries, totalCount, page, totalPages)
    }

    fun <T : Entity> findValues(table: CrmTable<T>, id: String): Map<String, String>? = transaction {
        table.selectAll().where { table.id eq id }.singleOrNull()?.let { table.readValues(it) }
    }

    fun version(values: Map<String, String>): String {
        val payload = values.toSortedMap().entries.joinToString("\u0000") { "${it.key}=${it.value}" }
        return MessageDigest.getInstance("SHA-256").digest(payload.toByteArray())
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    fun <T : Entity> validate(table: CrmTable<T>, values: Map<String, String>, creating: Boolean): List<String> {
        val errors = mutableListOf<String>()

        fun required(name: String, label: String) {
            if (values[name].isNullOrBlank()) errors += "$label ist erforderlich."
        }

        when (table) {
            ClassificationsTable -> {
                required("id", "ID")
                required("name", "Name")
                required("status", "Status")
                if (values["status"].orEmpty().isNotBlank() && Status.entries.none { it.name == values["status"] }) {
                    errors += "Der ausgewählte Status ist ungültig."
                }
            }

            IngredientsTable -> {
                required("id", "ID")
                required("name", "Name")
                required("classification", "Einstufung")
                val classification = values["classification"].orEmpty()
                if (classification.isNotBlank() && ClassificationsTable.findById(classification) == null) {
                    errors += "Die ausgewählte Einstufung existiert nicht."
                }
            }

            ProductInsightsTable -> {
                required("product_id", "Barcode")
                required("date", "Datum")
                val productId = values["product_id"].orEmpty()
                if (productId.isNotBlank() && !productExists(productId)) {
                    errors += "Für diesen Barcode existiert kein Produkt."
                }
                values["animal"].orEmpty().split(";").filter { it.isNotBlank() }.forEach { id ->
                    if (ClassificationsTable.findById(id) == null) {
                        errors += "Eine tierische Klassifizierung existiert nicht."
                    }
                }
                values["alcohol"].orEmpty().split(";").filter { it.isNotBlank() }.forEach { id ->
                    if (ClassificationsTable.findById(id) == null) {
                        errors += "Eine berauschende Klassifizierung existiert nicht."
                    }
                }
            }

            ProductParentTable -> {
                required("child_id", "Barcode-Dublette")
                required("parent_id", "Barcode-Hauptprodukt")
                if (values["child_id"] == values["parent_id"]) {
                    errors += "Hauptprodukt und Dublette dürfen nicht identisch sein."
                }
                if (values["child_id"].orEmpty().isNotBlank() && !productExists(values["child_id"].orEmpty())) {
                    errors += "Das Dubletten-Produkt existiert nicht."
                }
                if (values["parent_id"].orEmpty().isNotBlank() && !productExists(values["parent_id"].orEmpty())) {
                    errors += "Das Hauptprodukt existiert nicht."
                }
            }

            CategoriesTable -> {
                required("id", "ID")
                required("name", "Name")
                val parentId = values["parent_id"].orEmpty()
                if (parentId.isNotBlank()) {
                    if (parentId == values["id"]) {
                        errors += "Eine Kategorie kann nicht ihre eigene Überkategorie sein."
                    } else {
                        val parent = CategoriesTable.findById(parentId)
                        if (parent == null) {
                            errors += "Die Überkategorie existiert nicht."
                        } else if (parent.parentId != null) {
                            errors += "Es sind nur zwei Kategorie-Ebenen erlaubt."
                        }
                    }
                }
            }

            CategoryMappingTable -> {
                required("id", "OFF-Kategorie")
                required("category_id", "Kategorie")
                val categoryId = values["category_id"].orEmpty()
                if (categoryId.isNotBlank()) {
                    val category = CategoriesTable.findById(categoryId)
                    if (category == null) {
                        errors += "Die ausgewählte Kategorie existiert nicht."
                    } else if (CategoriesTable.children(category.id).isNotEmpty()) {
                        errors += "Bitte eine Unterkategorie auswählen."
                    }
                }
            }
        }

        table.getCrmFields().forEach { field ->
            val value = values[field.name].orEmpty()
            if (field is CrmLens.Date && value.isNotBlank()) {
                runCatching { LocalDate.parse(value) }.onFailure {
                    errors += "${field.label} ist kein gültiges Datum."
                }
            }
            if (creating && field.name == table.id.name && value.isBlank() && table !== ProductInsightsTable) {
                // already covered above; kept for future id fields
            }
        }

        return errors.distinct()
    }

    fun <T : Entity> update(
        table: CrmTable<T>,
        id: String,
        values: Map<String, String>,
        expectedVersion: String?,
        actor: String,
        note: String,
    ): List<String> {
        val errors = validate(table, values, creating = false).toMutableList()
        val submittedId = values[table.id.name]
        if (submittedId != null && submittedId != id) {
            errors += "Die ID kann nicht geändert werden."
        }
        if (errors.isNotEmpty()) return errors

        transaction {
            val row = table.selectAll().where { table.id eq id }.singleOrNull()
                ?: throw IllegalArgumentException("Eintrag nicht gefunden.")
            val oldValues = table.readValues(row)
            if (!expectedVersion.isNullOrBlank() && expectedVersion != version(oldValues)) {
                throw CrmConflictException("Der Eintrag wurde zwischenzeitlich geändert. Bitte neu laden.")
            }

            val updated = table.update({ table.id eq id }) {
                table.getCrmFields().forEach { field ->
                    field.write(it, values[field.name].orEmpty())
                }
            }
            if (updated == 0) throw IllegalArgumentException("Eintrag nicht gefunden.")

            ProposalsTable.recordDirectChange(
                entityType = table.tableName,
                oldValues = oldValues,
                newValues = values,
                creator = actor,
                note = note.ifBlank { "Direkte Admin-Änderung" },
            )
            markProductsForReprocessing(table, values)
        }
        return emptyList()
    }

    fun <T : Entity> create(
        table: CrmTable<T>,
        values: Map<String, String>,
        actor: String,
        note: String,
    ): List<String> {
        val errors = validate(table, values, creating = true)
        if (errors.isNotEmpty()) return errors

        transaction {
            if (table !== ProductInsightsTable) {
                val id = values[table.id.name].orEmpty()
                if (id.isNotBlank() && table.selectAll().where { table.id eq id }.limit(1).any()) {
                    throw IllegalArgumentException("Ein Eintrag mit dieser ID existiert bereits.")
                }
            }

            table.insert {
                table.getCrmFields().forEach { field ->
                    field.write(it, values[field.name].orEmpty())
                }
            }

            ProposalsTable.recordDirectChange(
                entityType = table.tableName,
                oldValues = null,
                newValues = values,
                creator = actor,
                note = note.ifBlank { "Direktes Anlegen durch Admin" },
            )
            markProductsForReprocessing(table, values)
        }
        return emptyList()
    }

    fun <T : Entity> impactText(table: CrmTable<T>, values: Map<String, String>): String? = transaction {
        when (table) {
            ClassificationsTable -> {
                val id = values["id"].orEmpty()
                val ingredientCount = IngredientsTable.selectAll()
                    .where { IngredientsTable.classification eq id }.count()
                val insightCount = ProductInsightsTable.all().count {
                    it.animal.any { classification -> classification.id == id } ||
                            it.alcohol.any { classification -> classification.id == id }
                }
                "$ingredientCount Zutaten und $insightCount Produktinformationen verweisen auf diese Einstufung."
            }

            IngredientsTable -> "Änderungen an dieser Zutat können Produkte betreffen, die sie enthalten."
            ProductInsightsTable -> values["product_id"]?.takeIf { it.isNotBlank() }
                ?.let { "Änderungen betreffen das Produkt $it." }

            ProductParentTable -> "Änderungen betreffen die Barcode-Verknüpfung dieser Dublette."

            CategoriesTable -> {
                val id = values["id"].orEmpty()
                val count = if (id.isBlank()) 0 else {
                    ProcessedProductsTable.selectAll()
                        .where { ProcessedProductsTable.categoryId eq id }.count().toInt()
                }
                "$count Produkte sind dieser Kategorie zugeordnet."
            }

            CategoryMappingTable ->
                "Änderungen greifen beim nächsten Reprocessing der betroffenen Produkte."
        }
    }

    private fun productExists(id: String): Boolean = transaction {
        ProductsOffTable.selectAll().where { ProductsOffTable.id eq id }.limit(1).any() ||
                ProductsCustomTable.selectAll().where { ProductsCustomTable.id eq id }.limit(1).any()
    }

    private fun lower(expression: Expression<String>): CustomFunction<String> =
        CustomFunction("lower", TextColumnType(), expression)

    private fun <T : Entity> searchOp(table: CrmTable<T>, query: String): Op<Boolean>? {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return null
        val needle = "%${trimmed.lowercase()}%"
        return when (table) {
            ClassificationsTable ->
                (lower(ClassificationsTable.idRaw) like needle) or
                        (lower(ClassificationsTable.name) like needle)

            IngredientsTable ->
                (lower(IngredientsTable.idRaw) like needle) or
                        (lower(IngredientsTable.name) like needle)

            ProductInsightsTable ->
                (lower(ProductInsightsTable.idRaw) like needle) or
                        (lower(ProductInsightsTable.product_id) like needle) or
                        (lower(ProductInsightsTable.content) like needle)

            ProductParentTable ->
                (lower(ProductParentTable.idRaw) like needle) or
                        (lower(ProductParentTable.parentId) like needle)

            CategoriesTable ->
                (lower(CategoriesTable.idRaw) like needle) or
                        (lower(CategoriesTable.name) like needle)

            CategoryMappingTable ->
                (lower(CategoryMappingTable.idRaw) like needle) or
                        (lower(CategoryMappingTable.categoryId) like needle)
        }
    }
}

class CrmConflictException(message: String) : IllegalArgumentException(message)
