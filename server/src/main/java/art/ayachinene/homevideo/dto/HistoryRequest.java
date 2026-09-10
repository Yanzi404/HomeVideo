package art.ayachinene.homevideo.dto;

public record HistoryRequest(
        String videoPath,
        long position,
        long duration
) {
}
