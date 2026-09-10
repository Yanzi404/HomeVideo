package art.ayachinene.homevideo.data.model

data class HistoryItem(
    val videoPath: String,
    val videoName: String,
    val position: Long,
    val duration: Long,
    val lastPlayed: Long
)
