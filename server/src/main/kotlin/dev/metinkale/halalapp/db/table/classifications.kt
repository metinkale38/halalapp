package dev.metinkale.halalapp.db.table

import dev.metinkale.halalapp.domain.Status
import dev.metinkale.halalapp.db.entity.Classification
import org.jetbrains.exposed.v1.core.ResultRow


object ClassificationsTable : CrmTable<Classification>("classifications") {

    val status = enumerationByName("status", 255, Status::class)
    val animal = bool("animal")
    val alcohol = bool("alcohol")
    val name = varchar("name", 255)
    val url = text("url")


    override fun map(row: ResultRow) = Classification(
        id = row[id].value,
        status = row[status],
        animal = row[animal],
        alcohol = row[alcohol],
        name = row[name],
        url = row[url]
    )

}