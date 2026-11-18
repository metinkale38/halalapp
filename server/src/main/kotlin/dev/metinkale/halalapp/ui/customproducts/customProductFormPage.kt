package dev.metinkale.halalapp.ui.customproducts

import dev.metinkale.halalapp.html.formGroup
import dev.metinkale.halalapp.html.loadEasyMDE
import dev.metinkale.halalapp.html.markdownField
import dev.metinkale.halalapp.ui.component.card
import dev.metinkale.halalapp.ui.component.loadPhotoNormalizer
import dev.metinkale.halalapp.ui.component.photoField
import dev.metinkale.halalapp.ui.component.template
import kotlinx.html.*
import java.time.LocalDate

data class CustomProductFormData(
    val barcode: String = "",
    val name: String = "",
    val brand: String = "",
    val insightContent: String = "",
    val insightDate: String = LocalDate.now().toString(),
    val animal: String = "",
    val alcohol: String = "",
    val note: String = "",
    val categoryId: String = "",
)

fun HTML.customProductFormPage(
    data: CustomProductFormData,
    animalOptions: List<Pair<String, String>>,
    alcoholOptions: List<Pair<String, String>>,
    categoryOptions: List<Pair<String, String>> = emptyList(),
    editing: Boolean = false,
    existingFrontUrl: String? = null,
    error: String? = null,
) = template(
    if (editing) "Custom-Produkt bearbeiten - HalalApp" else "Produkt vorschlagen - HalalApp",
    head = {
        loadEasyMDE()
        loadPhotoNormalizer()
    },
    titleBlock = {
        h2 { +if (editing) "Custom-Produkt bearbeiten" else "Produkt vorschlagen" }
        h4 {
            +if (editing) data.barcode else "Produktinformationen und Bilder einreichen"
        }
    },
) {
    card("m-auto w-full") {
        error?.let {
            div(classes = "mb-4 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700") {
                +it
            }
        }

        form(
            action = if (editing) "/crm/products_custom/${data.barcode}/edit" else "/crm/products_custom/new",
            method = FormMethod.post,
            encType = FormEncType.multipartFormData,
        ) {
            formGroup {
                label {
                    span(classes = "text-sm font-medium text-gray-700 mb-1") { +"Barcode" }
                    input(classes = "w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500") {
                        name = "id"
                        value = data.barcode
                        required = true
                        readonly = true
                        placeholder = "z. B. 4000521033736"
                    }
                }

                label {
                    span(classes = "text-sm font-medium text-gray-700 mb-1") { +"Produktname" }
                    input(classes = "w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500") {
                        name = "name"
                        value = data.name
                        required = true
                    }
                }

                label {
                    span(classes = "text-sm font-medium text-gray-700 mb-1") { +"Marke" }
                    input(classes = "w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500") {
                        name = "brand"
                        value = data.brand
                        required = true
                    }
                }

                selectField(
                    "Kategorie (optional)",
                    "categoryId",
                    listOf("" to "Keine Kategorie") + categoryOptions,
                    data.categoryId,
                )

                if (!editing) {
                    markdownField(
                        "Produktinformation / Quelle (Pflicht)",
                        "insightContent",
                        data.insightContent,
                    ) {
                        placeholder = "Herstellerbestätigung, Quelle oder sonstige Produktinformation..."
                    }
                } else {
                    div(classes = "rounded-xl border border-blue-100 bg-blue-50 p-4 text-sm text-blue-800") {
                        +"Neue Produktinformationen können nach dem Speichern über die Produktseite ergänzt werden."
                        a(classes = "ml-2 font-semibold underline") {
                            href = "/crm/product_insights/new?product_id=${data.barcode}"
                            +"Produktinformation hinzufügen"
                        }
                    }
                }

                if (!editing) {
                    label {
                        span(classes = "text-sm font-medium text-gray-700 mb-1") { +"Datum der Information" }
                        input(classes = "w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500") {
                            name = "insightDate"
                            type = InputType.date
                            value = data.insightDate
                            required = true
                        }
                    }

                    selectField(
                        "Tierische Bestandteile",
                        "animal",
                        listOf("" to "Keine Angabe") + animalOptions,
                        data.animal,
                    )

                    selectField(
                        "Berauschende Bestandteile",
                        "alcohol",
                        listOf("" to "Keine Angabe") + alcoholOptions,
                        data.alcohol,
                    )
                }

                photoField(
                    label = "Frontfoto (optional)",
                    name = "front",
                    previewId = "front-preview",
                    existingUrl = existingFrontUrl,
                    removalName = if (editing && existingFrontUrl != null) "removeFront" else null,
                )

                if (!editing) {
                    label {
                        span(classes = "text-sm font-medium text-gray-700 mb-1") { +"Notiz an die Admins" }
                        textArea(classes = "w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500") {
                            this.name = "note"
                            rows = "4"
                            +(data.note)
                        }
                    }
                }

                input(
                    type = InputType.submit,
                    classes = "px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600 cursor-pointer max-w-[14rem]",
                ) {
                    value = if (editing) "Speichern" else "Vorschlag senden"
                }
            }
        }
    }
}
