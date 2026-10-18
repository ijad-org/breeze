package com.ijad.breeze.data

data class AcBrand(
    val id: String,
    val name: String,
    val letter: Char
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
    val state: RemoteState = RemoteState(),
    /** [Brands.CODE_LIST_VERSION] when this AC was paired; 0 for ACs saved before it existed. */
    val codeListVersion: Int = Brands.CODE_LIST_VERSION
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
    /**
     * Version of the pairing code lists in `BrandIr`. Bump it when a change makes stored
     * `configIndex` values pick a different protocol, so older pairings are asked to find
     * their code again. 1: the multi-protocol code lists of Daikin, Panasonic, Haier, Blue Star,
     * Carrier (with Coolix) and Other.
     */
    const val CODE_LIST_VERSION = 1

    val all: List<AcBrand> = listOf(
        AcBrand("samsung", "Samsung", 'S'),
        AcBrand("lg", "LG", 'L'),
        AcBrand("daikin", "Daikin", 'D'),
        AcBrand("mitsubishi", "Mitsubishi", 'M'),
        AcBrand("voltas", "Voltas", 'V'),
        AcBrand("bluestar", "Blue Star", 'B'),
        AcBrand("carrier", "Carrier", 'C'),
        AcBrand("haier", "Haier", 'H'),
        AcBrand("panasonic", "Panasonic", 'P'),
        AcBrand("other", "Other", '?')
    )

    fun byId(id: String): AcBrand? = all.firstOrNull { it.id == id }
}
