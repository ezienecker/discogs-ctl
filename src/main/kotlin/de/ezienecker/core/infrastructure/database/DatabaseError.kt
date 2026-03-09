package de.ezienecker.core.infrastructure.database

sealed class DatabaseError(val message: String) {
    data class Unknown(val exception: Throwable) : DatabaseError("An unknown error occurred: ${exception.message ?: "No message provided"}")
}

class DatabaseException(val error: DatabaseError) : Exception()
