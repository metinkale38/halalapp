package dev.metinkale.halalapp.worker.productprocessor

import dev.metinkale.halalapp.db.entity.Ingredient
import dev.metinkale.halalapp.domain.Status

fun buildStatus(
    animalIngredients: List<Ingredient>,
    alcoholIngredients: List<Ingredient>,
    unknownIngredients: List<Ingredient>,
) = buildList {

    if (
        animalIngredients.any { it.classification.status == Status.HARAM } ||
        alcoholIngredients.any { it.classification.status == Status.HARAM }
    ) {
        add(Status.HARAM)
    } else if (
        unknownIngredients.isEmpty() &&
        animalIngredients.all { it.classification.status == Status.HALAL } &&
        alcoholIngredients.all { it.classification.status == Status.HALAL }
    ) {
        add(Status.HALAL)
    } else {
        if (unknownIngredients.isNotEmpty()) {
            add(Status.INCOMPLETE)
        }
        if (animalIngredients.any { it.classification.status == Status.DOUBTFUL } ||
            alcoholIngredients.any { it.classification.status == Status.DOUBTFUL }
        ) {
            add(Status.DOUBTFUL)
        }
        if (animalIngredients.any { it.classification.status == Status.UNKNOWN } ||
            alcoholIngredients.any { it.classification.status == Status.UNKNOWN }
        ) {
            add(Status.UNKNOWN)
        }
    }

}