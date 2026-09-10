package art.ayachinene.homevideo.data.model

data class DirectoryItem(
    val name: String,
    val path: String,
    val type: String,
    val size: Long? = null,
    val modified: Long? = null,
    val width: Int? = null,
    val height: Int? = null
)
