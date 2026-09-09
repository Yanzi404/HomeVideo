package art.ayachinene.homevideo.dto;

public record DirectoryItem(
        String name,
        String path,
        String type,
        Long size,
        Long modified,
        Integer width,
        Integer height
) {
}
