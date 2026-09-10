package art.ayachinene.homevideo.data.model

data class DirectoryResponse(
    val items: List<DirectoryItem>,
    val path: String,
    val total: Long
)
