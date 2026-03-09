package de.ezienecker.follow.service

import de.ezienecker.core.configuration.model.Username
import de.ezienecker.core.infrastructure.database.DatabaseError
import de.ezienecker.core.infrastructure.database.DatabaseException
import de.ezienecker.follow.infrastructure.repository.FollowRepository
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class FollowService(private val followRepository: FollowRepository) {

    fun list(tag: String?): Result<List<FollowDto>> {
        logger.debug { "Listing follows ${tag?.let { " with tag: [$it]" } ?: ""}." }

        return try {
            val follows = followRepository
                .listByTag(tag)
                .also { follows ->
                    logger.info { "Retrieved [${follows.size}] follows ${tag?.let { "with tag: [$it]" } ?: ""}." }
                }

            Result.success(follows)
        } catch (exception: Exception) {
            logger.error(exception) { "Failed to list follows ${tag?.let { "with tag: [$it]" } ?: ""}." }
            Result.failure(DatabaseException(DatabaseError.Unknown(exception)))
        }
    }

    fun add(username: Username, tags: List<String>) {
        logger.debug { "Adding follow for username: [${username.value}] with tags: $tags." }
        followRepository.add(username.value, tags)
    }

    fun remove(username: Username) {
        logger.debug { "Removing follow for username: [${username.value}]." }
        followRepository.remove(username.value)
    }
}
