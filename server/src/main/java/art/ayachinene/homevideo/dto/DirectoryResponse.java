package art.ayachinene.homevideo.dto;

import java.util.List;

public record DirectoryResponse(
        List<DirectoryItem> items,
        String path,
        long total
) {
}
