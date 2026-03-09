package de.ezienecker.follow.service

data class FollowDto(
    val username: String,
    val tags: List<String>,
) {
    companion object {
        fun from(username: String, tags: List<String>): FollowDto = FollowDto(username, tags)
    }
}
