package com.ijad.breeze.data

data class AcBrand(
    val id: String,
    val name: String,
    val letter: Char,
    /** How many IR config probes this brand exposes during pairing. */
    val codeCount: Int
)

/** Last known remote state for one AC; persisted so each room restores where it left off. */
data class RemoteState(
    val poweredOn: Boolean = true,
    val mode: AcMode = AcMode.Cool,
    val temperatureC: Int = 24,
    val fan: FanSpeed = FanSpeed.Medium,
    val swingOn: Boolean = true
)

data class AcDevice(
    val id: String,
    val name: String,
    val brandId: String,
    val brandName: String,
    val configIndex: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val state: RemoteState = RemoteState()
)

enum class AcMode(val label: String) {
    Cool("Cool"),
    Heat("Heat"),
    Dry("Dry"),
    Fan("Fan"),
    Auto("Auto")
}

/** Order matches the four fan bars on the remote (1 = Low … 4 = Auto). */
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

enum class TimerAction { TurnOff, TurnOn }

/** One pending timer per device. [triggerAtMillis] is wall-clock time. */
data class AcTimer(
    val deviceId: String,
    val action: TimerAction,
    val triggerAtMillis: Long
)

object Brands {
    val all: List<AcBrand> = listOf(
        AcBrand("samsung", "Samsung", 'S', 12),
        AcBrand("lg", "LG", 'L', 5),
        AcBrand("daikin", "Daikin", 'D', 4),
        AcBrand("mitsubishi", "Mitsubishi", 'M', 12),
        AcBrand("voltas", "Voltas", 'V', 10),
        AcBrand("bluestar", "Blue Star", 'B', 10),
        AcBrand("carrier", "Carrier", 'C', 5),
        AcBrand("haier", "Haier", 'H', 10),
        AcBrand("panasonic", "Panasonic", 'P', 10),
        AcBrand("other", "Other", '?', 8)
    )

    fun byId(id: String): AcBrand? = all.firstOrNull { it.id == id }
}
