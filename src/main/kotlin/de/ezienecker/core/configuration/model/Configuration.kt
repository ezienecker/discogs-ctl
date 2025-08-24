package de.ezienecker.core.configuration.model

data class Configuration(
    val username: Username?,
    val token: Token?,
)

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

@JvmInline
value class Token private constructor(
    val value: String,
) {
    init {
        require(value.isNotEmpty()) { "Token must not be empty" }
    }

    companion object {
        operator fun invoke(value: String?): Token {
            requireNotNull(value) { "Token cannot be null" }
            return Token(value)
        }
    }
}
