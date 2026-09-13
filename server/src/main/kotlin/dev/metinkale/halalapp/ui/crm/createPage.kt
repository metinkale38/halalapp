package dev.metinkale.halalapp.crm

import dev.metinkale.halalapp.db.entity.Entity
import dev.metinkale.halalapp.db.table.CrmTable
import dev.metinkale.halalapp.html.formGroup
import dev.metinkale.halalapp.html.loadEasyMDE
import dev.metinkale.halalapp.html.markdownField
import dev.metinkale.halalapp.ui.component.card
import dev.metinkale.halalapp.ui.component.template
import dev.metinkale.halalapp.ui.crm.CrmLens
import dev.metinkale.halalapp.ui.crm.germanName
import dev.metinkale.halalapp.ui.crm.getCrmFields
import kotlinx.html.*

fun <T : Entity> HTML.createRequestPage(table: CrmTable<T>, map: Map<String, String>, readOnlyFields: Set<String>) {


    template("Vorschlag erstellen - ${table.germanName}", head = { loadEasyMDE() }, {
        h2 { +"Vorschlag erstellen" }
        h4 { +table.germanName }
    }) {
        card("m-auto w-full") {


            form(
                action = "/crm/${table.tableName}/new",
                method = FormMethod.post
            ) {

                formGroup {
                    table.getCrmFields().forEach { field ->
                        if(field.name in readOnlyFields) {
                            staticField(field.label, field.name, map[field.name] ?: "")
                        }else
                        when (field) {
                            is CrmLens.CheckBox -> checkBoxField(field.label, field.name, map[field.name] == "true")
                            is CrmLens.Markdown -> markdownField(field.label, field.name, map[field.name])
                            is CrmLens.Select<*> -> selectField(
                                field.label,
                                field.name,
                                field.options,
                                map[field.name]
                            )

                            is CrmLens.Text -> inputField(field.label, field.name, map[field.name] ?: "")
                            is CrmLens.Date -> inputField(field.label, field.name, map[field.name] ?: "") {
                                type = InputType.date
                            }

                            is CrmLens.MultiSelect<*> -> multiSelect(
                                field.label,
                                field.name,
                                field.options,
                                map[field.name]?.split(";") ?: emptyList()
                            )

                            is CrmLens.StaticText -> staticField(field.label, field.name, map[field.name] ?: "")
                        }
                    }



                    textAreaField("Notiz", "note") {
                        placeholder = "Notiz eingeben"
                        +(map["note"] ?: "")
                    }

                    submitButton()
                }
            }
        }
    }
}