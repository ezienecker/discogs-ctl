package de.ezienecker.core.command

sealed interface OutputFormat {
    data object Compact : OutputFormat
    data object Wide : OutputFormat
    data object Json : OutputFormat
    data object Display : OutputFormat

    companion object {
        fun from(value: String): OutputFormat = when (value.lowercase()) {
            "wide" -> Wide
            "json" -> Json
            "display" -> Display
            else -> Compact
        }
    }
}
