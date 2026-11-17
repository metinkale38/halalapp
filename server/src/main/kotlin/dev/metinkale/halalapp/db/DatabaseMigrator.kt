package dev.metinkale.halalapp.db

import dev.metinkale.halalapp.db.table.MetadataTable
import org.jetbrains.exposed.v1.core.statements.StatementType
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory
import java.io.File
import java.net.JarURLConnection
import java.net.URL

object DatabaseMigrator {
    private const val VERSION_KEY = "DB_VERSION"
    private const val LOCK_KEY = "halalapp_db_migration"
    private val fileNamePattern = Regex("""^(\d+)_.*\.sql$""")

    private val logger = LoggerFactory.getLogger("DB_MIGRATION")

    private data class Migration(val version: Int, val name: String, val sql: String)

    fun migrate() {
        val migrations = loadMigrations()
        if (migrations.isEmpty()) return

        transaction {
            exec(
                """
                DO $$
                BEGIN
                    IF to_regclass('public.metadata') IS NULL THEN
                        CREATE TABLE public.metadata (key text PRIMARY KEY, value text NOT NULL);
                    END IF;
                END $$;
                """.trimIndent(),
                explicitStatementType = StatementType.MULTI,
            )
            exec("SELECT pg_advisory_xact_lock(hashtext('$LOCK_KEY')::bigint)")

            val current = MetadataTable.getValue(VERSION_KEY)?.toIntOrNull() ?: 0
            migrations.filter { it.version > current }.forEach { migration ->
                logger.info("Applying database migration ${migration.name}")
                exec(migration.sql, explicitStatementType = StatementType.MULTI)
                MetadataTable.setValue(VERSION_KEY, migration.version.toString())
            }
        }
    }

    private fun loadMigrations(): List<Migration> {
        val loader = DatabaseMigrator::class.java.classLoader
        val names = mutableSetOf<String>()
        loader.getResources("db/migration").toList().forEach { collectNames(it, names) }

        val migrations = names.mapNotNull { name ->
            val match = fileNamePattern.matchEntire(name) ?: return@mapNotNull null
            val sql = loader.getResourceAsStream("db/migration/$name")
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                ?: return@mapNotNull null
            Migration(match.groupValues[1].toInt(), name, sql)
        }

        migrations.groupBy { it.version }.forEach { (version, scripts) ->
            require(scripts.size == 1) { "Doppelte Datenbank-Migrationsversion: $version" }
        }
        return migrations.sortedBy { it.version }
    }

    private fun collectNames(url: URL, target: MutableSet<String>) {
        when (url.protocol) {
            "file" -> File(url.toURI()).listFiles()
                ?.filter { it.isFile }
                ?.forEach { target += it.name }

            "jar" -> {
                val connection = url.openConnection() as JarURLConnection
                connection.useCaches = false
                val prefix = connection.entryName.trimEnd('/') + "/"
                connection.jarFile.use { jar ->
                    jar.entries().asSequence()
                        .map { it.name }
                        .filter { it.startsWith(prefix) && !it.removePrefix(prefix).contains('/') }
                        .forEach { target += it.substringAfterLast('/') }
                }
            }
        }
    }
}
