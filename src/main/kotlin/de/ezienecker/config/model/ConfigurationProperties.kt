package de.ezienecker.de.ezienecker.config.model

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
value class Key private constructor(
    val value: String,
) {
    init {
        require(value.isNotEmpty()) { "Key must not be empty" }
    }

    companion object {
        operator fun invoke(value: String?): Key {
            requireNotNull(value) { "Key cannot be null" }
            return Key(value)
        }
    }
}

@JvmInline
value class Secret private constructor(
    val value: String,
) {
    init {
        require(value.isNotEmpty()) { "Secret must not be empty" }
    }

    companion object {
        operator fun invoke(value: String?): Secret {
            requireNotNull(value) { "Secret cannot be null" }
            return Secret(value)
        }
    }
}

@JvmInline
value class AccessToken private constructor(
    val value: String,
) {
    init {
        require(value.isNotEmpty()) { "AccessToken must not be empty" }
    }

    companion object {
        operator fun invoke(value: String?): AccessToken {
            requireNotNull(value) { "AccessToken cannot be null" }
            return AccessToken(value)
        }
    }
}