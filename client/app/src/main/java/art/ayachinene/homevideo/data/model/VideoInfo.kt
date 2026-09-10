package art.ayachinene.homevideo.data.model

data class VideoInfo(
    val path: String,
    val duration: Long,
    val width: Int,
    val height: Int,
    val codec: String,
    val format: String,
    val audioTracks: List<AudioTrack>,
    val subtitles: List<SubtitleInfo>
)

data class AudioTrack(
    val index: Int,
    val codec: String,
    val language: String,
    val channels: String
)

data class SubtitleInfo(
    val name: String,
    val language: String
)
