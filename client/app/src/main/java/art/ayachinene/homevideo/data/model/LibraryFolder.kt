package art.ayachinene.homevideo.data.model

data class LibraryFolder(
    val name: String,
    val path: String,
    val videos: List<DirectoryItem>
)
