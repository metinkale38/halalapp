package dev.metinkale.halalapp.db.table

import org.jetbrains.exposed.v1.core.Table


object BrandBlacklistTable : Table("brand_blacklist") {
    val brand = text("brand")
}