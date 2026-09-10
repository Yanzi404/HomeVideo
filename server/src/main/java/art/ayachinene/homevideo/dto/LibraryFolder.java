package art.ayachinene.homevideo.dto;

import java.util.List;

public record LibraryFolder(
        String name,
        String path,
        List<DirectoryItem> videos
) {
}
