package com.ijad.breeze.data

data class AcBrand(
    val id: String,
    val name: String,
    val letter: Char,
    /** How many IR config probes this brand exposes during pairing. */
    val codeCount: Int
)

data class AcDevice(
    val id: String,
    val name: String,
    val brandId: String,
    val brandName: String,
    val configIndex: Int,
    val createdAt: Long = System.currentTimeMillis()
)

enum class AcMode(val label: String) {
    Cool("Cool"),
    Heat("Heat"),
    Dry("Dry"),
    Fan("Fan"),
    Auto("Auto")
}

enum class FanSpeed(val label: String) {
    Low("Low"),
    Medium("Mid"),
    High("High"),
    Auto("Auto")
}

enum class ThemeMode(val label: String) {
    Light("Light"),
    Dark("Dark"),
    System("System")
}

object Brands {
    val all: List<AcBrand> = listOf(
        AcBrand("samsung", "Samsung", 'S', 12),
        AcBrand("lg", "LG", 'L', 5),
        AcBrand("daikin", "Daikin", 'D', 12),
        AcBrand("mitsubishi", "Mitsubishi", 'M', 12),
        AcBrand("voltas", "Voltas", 'V', 10),
        AcBrand("bluestar", "Blue Star", 'B', 10),
        AcBrand("carrier", "Carrier", 'C', 10),
        AcBrand("haier", "Haier", 'H', 10),
        AcBrand("panasonic", "Panasonic", 'P', 10),
        AcBrand("other", "Other", 'O', 8)
    )

    fun byId(id: String): AcBrand? = all.firstOrNull { it.id == id }
}
