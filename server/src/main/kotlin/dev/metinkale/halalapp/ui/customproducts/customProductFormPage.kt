package dev.metinkale.halalapp.ui.customproducts

import dev.metinkale.halalapp.html.formGroup
import dev.metinkale.halalapp.html.loadEasyMDE
import dev.metinkale.halalapp.html.markdownField
import dev.metinkale.halalapp.ui.component.card
import dev.metinkale.halalapp.ui.component.template
import kotlinx.html.*
import java.time.LocalDate

data class CustomProductFormData(
    val barcode: String = "",
    val name: String = "",
    val brand: String = "",
    val category: String = "",
    val insightContent: String = "",
    val insightDate: String = LocalDate.now().toString(),
    val animal: Set<String> = emptySet(),
    val alcohol: Set<String> = emptySet(),
    val note: String = "",
)

fun HTML.customProductFormPage(
    data: CustomProductFormData,
    animalOptions: List<Pair<String, String>>,
    alcoholOptions: List<Pair<String, String>>,
    editing: Boolean = false,
    existingFrontUrl: String? = null,
    existingIngredientsUrl: String? = null,
    error: String? = null,
    barcodeReadOnly: Boolean = false,
) = template(
    if (editing) "Custom-Produkt bearbeiten - HalalApp" else "Produkt vorschlagen - HalalApp",
    head = { loadEasyMDE() },
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
                        readonly = barcodeReadOnly || editing
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

                label {
                    span(classes = "text-sm font-medium text-gray-700 mb-1") { +"Kategorie (optional)" }
                    input(classes = "w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500") {
                        name = "category"
                        value = data.category
                    }
                }

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

                    div(classes = "flex flex-col gap-2") {
                        span(classes = "text-sm font-medium text-gray-700") { +"Tierische Bestandteile" }
                        p(classes = "text-xs text-gray-500") {
                            +"Zutaten, die laut Produktinformation tierisch sind oder sein können. Mehrfachauswahl möglich."
                        }
                        checkBoxList("animal", animalOptions, data.animal)
                    }

                    div(classes = "flex flex-col gap-2") {
                        span(classes = "text-sm font-medium text-gray-700") { +"Berauschende Bestandteile" }
                        p(classes = "text-xs text-gray-500") {
                            +"Zutaten, die laut Produktinformation Alkohol enthalten oder enthalten können."
                        }
                        checkBoxList("alcohol", alcoholOptions, data.alcohol)
                    }
                }

                photoField(
                    label = "Frontfoto (optional)",
                    name = "front",
                    previewId = "front-preview",
                    existingUrl = existingFrontUrl,
                    removalName = if (editing && existingFrontUrl != null) "removeFront" else null,
                )

                photoField(
                    label = "Zutatenfoto (optional)",
                    name = "ingredients",
                    previewId = "ingredients-preview",
                    existingUrl = existingIngredientsUrl,
                    removalName = if (editing && existingIngredientsUrl != null) "removeIngredients" else null,
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
    script {
        unsafe {
            +"""
                async function normalizePhoto(input, previewId) {
                    const file = input.files && input.files[0];
                    const preview = document.getElementById(previewId);
                    if (!file || !preview) return;
                    preview.src = URL.createObjectURL(file);
                    preview.classList.remove('hidden');
                    try {
                        const bitmap = await createImageBitmap(file, { imageOrientation: 'from-image' });
                        const max = 2000;
                        let width = bitmap.width;
                        let height = bitmap.height;
                        if (width > max || height > max) {
                            const factor = Math.min(max / width, max / height);
                            width = Math.round(width * factor);
                            height = Math.round(height * factor);
                        }
                        const canvas = document.createElement('canvas');
                        canvas.width = width;
                        canvas.height = height;
                        canvas.getContext('2d').drawImage(bitmap, 0, 0, width, height);
                        const blob = await new Promise(resolve => canvas.toBlob(resolve, 'image/jpeg', 0.9));
                        if (!blob) return;
                        const parts = file.name.split('.');
                        parts.pop();
                        const name = (parts.join('.') || 'foto') + '.jpg';
                        const transfer = new DataTransfer();
                        transfer.items.add(new File([blob], name, { type: 'image/jpeg' }));
                        input.files = transfer.files;
                        preview.src = URL.createObjectURL(blob);
                    } catch (e) {
                        console.warn('Bild konnte nicht optimiert werden', e);
                    }
                }
            """.trimIndent()
        }
    }
}

private fun FlowContent.checkBoxList(
    name: String,
    options: List<Pair<String, String>>,
    selected: Set<String>,
) {
    if (options.isEmpty()) {
        p(classes = "text-sm text-gray-500") { +"Keine Optionen verfügbar." }
        return
    }
    div(classes = "grid grid-cols-1 gap-2 md:grid-cols-2") {
        options.forEach { (id, labelText) ->
            label(classes = "flex items-start gap-2 rounded-lg border border-gray-200 bg-white p-2 text-sm") {
                input(type = InputType.checkBox, classes = "mt-0.5 rounded border-gray-300 text-indigo-600 focus:ring-indigo-500") {
                    this.name = name
                    value = id
                    checked = id in selected
                }
                span { +labelText }
            }
        }
    }
}

private fun FlowContent.photoField(
    label: String,
    name: String,
    previewId: String,
    existingUrl: String?,
    removalName: String?,
) {
    div(classes = "flex flex-col gap-2") {
        span(classes = "text-sm font-medium text-gray-700") { +label }
        input(classes = "w-full rounded-md border-gray-300 shadow-sm text-sm") {
            type = InputType.file
            this.name = name
            accept = "image/jpeg,image/png"
            onChange = "normalizePhoto(this, '$previewId')"
        }
        if (existingUrl != null) {
            img(classes = "max-h-48 w-auto rounded-lg border border-gray-200 object-contain") {
                src = existingUrl
                alt = label
            }
            if (removalName != null) {
                label(classes = "flex items-center gap-2 text-sm text-red-700") {
                    input(type = InputType.checkBox, classes = "rounded border-gray-300 text-red-600 focus:ring-red-500") {
                        this.name = removalName
                        value = "true"
                    }
                    +"Vorhandenes Bild entfernen"
                }
            }
        }
        img(classes = "hidden max-h-48 w-auto rounded-lg border border-gray-200 object-contain") {
            id = previewId
            alt = "Vorschau"
        }
    }
}
