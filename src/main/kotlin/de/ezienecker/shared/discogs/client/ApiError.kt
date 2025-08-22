package de.ezienecker.shared.discogs.client

sealed class ApiError(val message: String) {
    object Network : ApiError("Network error occurred.")
    object Unauthorized : ApiError("Unauthorized access. Please check your credentials.")
    object NoAccessToCollection : ApiError("No access to the collection. Maybe the collection is private or the user does not exist.")
    object NotFound : ApiError("Resource not found.")
    data class Server(val code: Int) : ApiError("Server error occurred with code: $code")
    data class Unknown(val exception: Throwable) : ApiError("An unknown error occurred: ${exception.message ?: "No message provided"}")
}

class ApiException(val error: ApiError) : Exception()
