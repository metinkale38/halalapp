package dev.metinkale.halalapp.service

import dev.metinkale.halalapp.db.table.ProcessedProductsTable
import org.jetbrains.exposed.v1.core.*

internal fun statusRankExpression(): Expression<Int> = Case()
    .When(stringLiteral("HALAL") eq anyFrom(ProcessedProductsTable.status), intLiteral(0))
    .When(stringLiteral("DOUBTFUL") eq anyFrom(ProcessedProductsTable.status), intLiteral(1))
    .When(stringLiteral("UNKNOWN") eq anyFrom(ProcessedProductsTable.status), intLiteral(2))
    .When(stringLiteral("HARAM") eq anyFrom(ProcessedProductsTable.status), intLiteral(3))
    .Else(intLiteral(4))
