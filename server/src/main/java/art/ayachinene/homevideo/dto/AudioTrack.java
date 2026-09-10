package art.ayachinene.homevideo.dto;

public record AudioTrack(
        int index,
        String codec,
        String language,
        String channels
) {
}
