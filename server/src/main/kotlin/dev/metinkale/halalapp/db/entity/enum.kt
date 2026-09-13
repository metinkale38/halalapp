package dev.metinkale.halalapp.domain

enum class Confidence(val value: String) {
    INGREDIENTS("laut Zutatenliste"),
    MANUFACTURER("laut Hersteller"),
    VERIFIED("Verifiziert")
}

enum class Status(
    val value: String,
    val desc: String,
    val bgColor: String,
    val textColor: String,
    val borderColor: String,
    val selectorColor: String,
) {
    HALAL(
        "Unbedenklich",
        "Enthält keine bedenklichen Zutaten",
        "bg-emerald-50",
        "text-emerald-700",
        "border-emerald-200",
        "bg-emerald-700"
    ),
    DOUBTFUL(
        "Zweifelhaft",
        "Es gibt unterschiedliche Meinungen",
        "bg-amber-50",
        "text-amber-700",
        "border-amber-200",
        "bg-amber-700"
    ),
    UNKNOWN(
        "Unbekannt",
        "Die Herkunft der Zutaten ist unklar",
        "bg-violet-50",
        "text-violet-700",
        "border-violet-200",
        "bg-violet-700"
    ),
    HARAM(
        "Haram",
        "Enthält tierische oder Alkoholische Bestandteile",
        "bg-red-50",
        "text-red-700",
        "border-red-200",
        "bg-red-700"
    ),
    INCOMPLETE(
        "Unvollständig",
        "Es fehlen Informationen zu den Zutaten",
        "bg-orange-50",
        "text-orange-700",
        "border-orange-300 border-dashed",
        "bg-orange-700"
    );

    val colorTags = "$textColor $bgColor $borderColor"
}

enum class RequestStatus {
    PENDING,
    APPROVED,
    REJECTED
}