package dev.metinkale.halalapp.ui.crm

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import java.time.LocalDate

sealed interface CrmLens<T> {

    val label: String
    val name: String
    val col: Column<T>

    fun read(row: ResultRow): String
    fun write(stmt: InsertStatement<*>, value: String)

    open class StaticText(
        override val label: String,
        override val name: String,
        override val col: Column<String>,
    ) : CrmLens<String> {
        override fun read(row: ResultRow): String = row[col]
        override fun write(stmt: InsertStatement<*>, value: String) {
            stmt[col] = value
        }
    }

    open class Text(
        override val label: String,
        override val name: String,
        override val col: Column<String>,
    ) : CrmLens<String> {
        override fun read(row: ResultRow): String = row[col]
        override fun write(stmt: InsertStatement<*>, value: String) {
            stmt[col] = value
        }
    }


    open class Date(
        override val label: String,
        override val name: String,
        override val col: Column<LocalDate>,
    ) : CrmLens<LocalDate> {
        override fun read(row: ResultRow): String = row[col].toString()
        override fun write(stmt: InsertStatement<*>, value: String) {
            stmt[col] = LocalDate.parse(value)
        }
    }


    open class Markdown(
        override val label: String,
        override val name: String,
        override val col: Column<String>,
    ) : CrmLens<String> {
        override fun read(row: ResultRow): String = row[col]
        override fun write(stmt: InsertStatement<*>, value: String) {
            stmt[col] = value
        }
    }

    open class CheckBox(
        override val label: String,
        override val name: String,
        override val col: Column<Boolean>,
    ) : CrmLens<Boolean> {
        override fun read(row: ResultRow): String = row[col].toString()
        override fun write(stmt: InsertStatement<*>, value: String) {
            stmt[col] = value == "true"
        }
    }

    open class Select<T>(
        override val label: String,
        override val name: String,
        val options: List<Pair<String, String>>,
        override val col: Column<T>,
        val _read: ResultRow.() -> String,
        val _write: InsertStatement<*>.(String) -> Unit,
    ) : CrmLens<T> {
        override fun read(row: ResultRow): String = _read(row)
        override fun write(stmt: InsertStatement<*>, value: String) {
            _write(stmt, value)
        }
    }

    open class MultiSelect<T>(
        override val label: String,
        override val name: String,
        val options: List<Pair<String, String>>,
        override val col: Column<List<T>>,
        val _read: ResultRow.() -> List<String>,
        val _write: InsertStatement<*>.(List<String>) -> Unit,
    ) : CrmLens<List<T>> {
        override fun read(row: ResultRow): String = _read(row).joinToString(";")

        override fun write(
            stmt: InsertStatement<*>,
            value: String,
        ) {
            _write(stmt, value.split(";"))
        }
    }
}
