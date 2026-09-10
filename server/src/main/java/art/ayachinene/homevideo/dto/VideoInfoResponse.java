package art.ayachinene.homevideo.dto;

import java.util.List;

public record VideoInfoResponse(
        String path,
        long duration,
        int width,
        int height,
        String codec,
        String format,
        List<AudioTrack> audioTracks,
        List<SubtitleInfo> subtitles
) {
}
