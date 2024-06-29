package model

data class InstrumentEvent(
    val type: Type,
    val data: Instrument,
) {
    enum class Type {
        ADD,
        DELETE,
    }
}

data class Instrument(
    val isin: ISIN,
    val description: String,
)
typealias ISIN = String
