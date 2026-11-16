package dev.metinkale.halalapp.feature.product


import dev.metinkale.halalapp.domain.Confidence
import dev.metinkale.halalapp.domain.Status
import dev.metinkale.halalapp.db.entity.Classification
import dev.metinkale.halalapp.db.entity.Ingredient
import dev.metinkale.halalapp.db.entity.Product
import dev.metinkale.halalapp.db.table.ProductInsightsTable
import dev.metinkale.halalapp.html.*
import dev.metinkale.halalapp.ui.component.card
import dev.metinkale.halalapp.ui.component.statusBadgeComponent
import dev.metinkale.halalapp.ui.component.template
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.html.*


fun HTML.productPage(product: Product) = template(
    product.brand + " " + product.name + " - HalalApp",
    titleBlock = {
        span(classes = "text-lg font-semibold uppercase text-primary-100") {
            +(product.brand.ifBlank { "Unbekannte Marke" })
        }
        h1 {
            +product.name
        }
        div(classes = "text-lg font-mono text-primary-200") {
            +"Barcode: ${product.id}"
        }


    }) {
    card("flex flex-col gap-2") {

        div(classes = "flex flex-wrap gap-4 w-full justify-center") {

            val imgClasses =
                "max-h-[300px] w-full md:w-[calc(50%-8px)] bg-gray-100 object-contain rounded-lg cursor-pointer transition-transform hover:scale-[1.01]"

            img(classes = imgClasses) {
                attributes.put("referrerpolicy", "no-referrer")
                src = product.images.getFrontPhoto(300)
                onClick = "classList.toggle('imagefullscreen')"
            }

//                product.images.getIngredientPhoto(300)?.let { ingredientUrl ->
//                    img(classes = imgClasses) {
//                        attributes.put("referrerpolicy", "no-referrer")
//                        src = ingredientUrl
//                        onClick = "classList.toggle('imagefullscreen')"
//                    }
//                }
        }
        div(classes = "space-y-6") {


            div(classes = "border-t border-gray-100") {

                val ingredients =
                    product.ingredientsText.ifBlank { product.ingredients.joinToString(", ") { it.name } }

                if (ingredients.isNotBlank()) {
                    details(classes = "group") {
                        // Der Header fungiert als Klick-Bereich
                        summary(classes = "cursor-pointer list-none flex items-center justify-between outline-none") {
                            h2(classes = "text-lg font-bold text-gray-900") { +"Zutaten" }
                            span(classes = "transition-transform group-open:rotate-180 text-gray-500") { +"▼" }
                        }

                        // Der Inhalt mit der Begrenzung auf 3 Zeilen
                        div(classes = "text-gray-600 leading-relaxed text-base line-clamp-3 group-open:line-clamp-none transition-all") {
                            markdownComponent(ingredients)

                            if (product.allergens.isNotEmpty()) {
                                div(classes = "mt-4 flex flex-wrap gap-2") {
                                    span(classes = "text-sm font-medium text-gray-500 self-center mr-1") { +"Allergene:" }
                                    product.allergens.forEach { allergen ->
                                        span(classes = "inline-flex items-center px-3 py-1 rounded-xl text-xs font-semibold bg-amber-50 text-amber-800 border border-amber-100") {
                                            +allergen.name
                                        }
                                    }
                                }
                            }
                            if (product.traces.isNotEmpty()) {
                                div(classes = "mt-3 flex flex-wrap gap-2") {
                                    span(classes = "text-sm font-medium text-gray-500 self-center mr-1") { +"Kann Spuren enthalten von:" }
                                    product.traces.forEach { trace ->
                                        span(classes = "inline-flex items-center px-3 py-1 rounded-xl text-xs font-semibold bg-slate-100 text-slate-700 border border-slate-200") {
                                            +trace.name
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }


            if (product.confidence == Confidence.INGREDIENTS) {
                warningComponent {
                    +"Diese Klassifizierung basiert auf den vorliegenden OpenFoodFacts-Daten. "
                    +"Diese können unvollständig oder inkorrekt sein. Bitte prüfe immer das Etikett auf der Verpackung."
                }
            }

            if (product.status.contains(Status.INCOMPLETE)) {
                errorComponent {
                    +"Die Zutatenliste ist unvollständig oder enthält unbekannte Zutaten. Es kann daher keine Klassifizierung vorgenommen werden."
                }
            }


            val unknown: List<Ingredient> = product.unknownIngredients
            if (unknown.isNotEmpty()) {
                div(classes = "flex flex-col justify-between") {
                    div {
                        h2(classes = "text-base font-bold text-gray-900 capitalize mb-4 flex items-center justify-between") {
                            +"Unbekannte Bestandteile"
                        }
                        div(classes = "space-y-3") {
                            div(classes = "bg-white p-3.5 rounded-xl border border-gray-100 shadow-sm space-y-1.5 flex gap-1 flex-wrap") {
                                unknown.sortedByDescending { it.classification.status }.forEach { it ->
                                    statusBadgeComponent(it.name, Status.INCOMPLETE)
                                }
                            }
                        }
                    }
                }
            }


            div(classes = "grid grid-cols-1 md:grid-cols-2 gap-6") {
                listOf(
                    Classification::animal to product.animalIngredients,
                    Classification::alcohol to product.alcoholIngredients
                ).forEach { (scope, ingredients) ->
                    div(classes = "flex flex-col justify-between") {
                        div {
                            h2(classes = "text-base font-bold text-gray-900 capitalize mb-4 flex items-center justify-between") {
                                +when (scope.name) {
                                    "animal" -> "Tierische Bestandteile"
                                    "alcohol" -> "Berauschende Bestandteile"
                                    else -> scope.name
                                }
                            }


                            if (ingredients.isEmpty()) {
                                if (!product.status.contains(Status.INCOMPLETE)) {
                                    div(classes = "flex items-center gap-2 text-emerald-600 font-medium text-sm") {
                                        span { +"✔ Keine gefunden" }
                                    }
                                } else {
                                    div(classes = "flex items-center gap-2 text-gray-600 font-medium text-sm") {
                                        span { +"❓ Keine gefunden" }
                                    }
                                }
                            } else {
                                div(classes = "space-y-3") {
                                    ingredients.sortedByDescending { it.classification.status }
                                        .forEach { it ->
                                            a {
                                                it.classification.url?.takeIf { it.isNotBlank() }?.let { url ->
                                                    if (url.contains("forum.halalapp.de")) {
                                                        href = url + "?backTo=https://halalapp.de/product/${product.id}"
                                                    }
                                                }

                                                div(classes = "bg-white p-3.5 rounded-xl border border-gray-100 shadow-sm space-y-1.5") {
                                                    div(classes = "flex items-center justify-between") {
                                                        statusBadgeComponent(it.classification.status)

                                                        span(classes = "text-xs font-semibold text-gray-400") {
                                                            +it.classification.confidence.value
                                                        }
                                                    }
                                                    div(classes = "text-sm font-semibold text-gray-900") { +it.name }
                                                    div(classes = "text-xs text-gray-500 leading-relaxed") { +it.classification.name }
                                                }
                                            }
                                        }
                                }
                            }
                        }
                    }
                }
            }

            val insights = ProductInsightsTable.findByBarcode(product.id)
            insights.sortedByDescending { it.date }.forEachIndexed { idx, it ->
                div(classes = "bg-white p-3.5 rounded-xl border border-gray-100 shadow-sm space-y-1.5 text-sm") {
                    p("italic text-gray-400") {
                        "Stand: "
                        +it.date.format(GermanDateTimeFormat)
                        if (idx > 0) +" (älterer Stand)"
                    }

                    it.content?.let { markdownComponent(it.replace("\n", "<br/>")) }

                }
            }
        }
        a(classes = "m-auto") {
            href = "/product/${product.id}/proposal"
            h6 ("text-primary hover:bg-primary-100 p-2 rounded"){ +"✎ Änderung vorschlagen" }
        }
    }
}


private val GermanDateTimeFormat = LocalDate.Format {
    day(Padding.ZERO)
    char('.')
    monthNumber(Padding.ZERO)
    char('.')
    year()
}