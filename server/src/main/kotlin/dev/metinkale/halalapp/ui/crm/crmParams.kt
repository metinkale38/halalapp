package dev.metinkale.halalapp.ui.crm

import dev.metinkale.halalapp.db.entity.Entity
import dev.metinkale.halalapp.db.table.CrmTable
import io.ktor.http.Parameters

fun <T : Entity> CrmTable<T>.paramsToValues(params: Parameters): Map<String, String> =
    getCrmFields().associate { field ->
        field.name to when (field) {
            is CrmLens.MultiSelect<*> ->
                params.getAll(field.name)?.filter { it.isNotBlank() }?.joinToString(";").orEmpty()

            is CrmLens.CheckBox -> (params[field.name] == "true").toString()
            else -> params[field.name].orEmpty()
        }
    }
