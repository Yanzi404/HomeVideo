package art.ayachinene.homevideo.dto;

public record HistoryItem(
        String videoPath,
        String videoName,
        long position,
        long duration,
        long lastPlayed
) {
}
