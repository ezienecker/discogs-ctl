package de.ezienecker.core.configuration.model

@JvmInline
value class Username private constructor(
    val value: String,
) {
    init {
        require(value.isNotEmpty()) { "Username must not be empty" }
    }

    companion object {
        operator fun invoke(value: String?): Username {
            requireNotNull(value) { "Username cannot be null" }
            return Username(value)
        }
    }
}
