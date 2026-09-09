package art.ayachinene.homevideo.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import art.ayachinene.homevideo.config.HomeVideoProperties;
import art.ayachinene.homevideo.dto.DirectoryItem;
import art.ayachinene.homevideo.dto.DirectoryResponse;

@Service
public class FileService {

    private static final Set<String> VIDEO_EXTENSIONS = Set.of(
            "mp4", "mkv", "avi", "mov", "ts", "flv", "wmv", "rmvb"
    );

    private final Path mediaRoot;

    public FileService(HomeVideoProperties properties) {
        this.mediaRoot = Paths.get(properties.getMediaDir()).normalize();
    }

    public DirectoryResponse listFiles(String path, int page, int size, String sort, String order) {
        Path dir = resolveAndValidate(path);

        if (!Files.exists(dir)) {
            throw new IllegalArgumentException("Path not found: " + path);
        }
        if (!Files.isDirectory(dir)) {
            throw new IllegalArgumentException("Not a directory: " + path);
        }

        List<DirectoryItem> items;
        try (Stream<Path> stream = Files.list(dir)) {
            items = stream
                    .filter(p -> Files.isDirectory(p) || isVideoFile(p))
                    .map(this::toItem)
                    .sorted(comparator(sort, order))
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Failed to list directory: " + path, e);
        }

        long total = items.size();
        List<DirectoryItem> paged = items.stream()
                .skip((long) page * size)
                .limit(size)
                .toList();

        return new DirectoryResponse(paged, normalizePath(path), total);
    }

    public List<DirectoryItem> searchFiles(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        String lowerKeyword = keyword.toLowerCase();

        try (Stream<Path> stream = Files.walk(mediaRoot)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(this::isVideoFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().contains(lowerKeyword))
                    .map(this::toItem)
                    .sorted(Comparator.comparing(DirectoryItem::name))
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Failed to search files", e);
        }
    }

    private DirectoryItem toItem(Path path) {
        boolean isDir = Files.isDirectory(path);
        String relativePath = mediaRoot.relativize(path).toString();

        try {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            return new DirectoryItem(
                    path.getFileName().toString(),
                    relativePath,
                    isDir ? "folder" : "file",
                    isDir ? null : attrs.size(),
                    attrs.lastModifiedTime().toMillis(),
                    null,
                    null
            );
        } catch (IOException e) {
            return new DirectoryItem(
                    path.getFileName().toString(),
                    relativePath,
                    isDir ? "folder" : "file",
                    null, null, null, null
            );
        }
    }

    private boolean isVideoFile(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0) return false;
        return VIDEO_EXTENSIONS.contains(name.substring(dot + 1).toLowerCase());
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank() || path.equals("/")) return "/";
        String normalized = path.replaceAll("/+$", "");
        return normalized.isEmpty() ? "/" : normalized;
    }

    private Path resolveAndValidate(String path) {
        String normalized = normalizePath(path);
        Path resolved = normalized.equals("/")
                ? mediaRoot
                : mediaRoot.resolve(normalized.substring(1)).normalize();

        if (!resolved.startsWith(mediaRoot)) {
            throw new IllegalArgumentException("Invalid path: " + path);
        }
        return resolved;
    }

    private Comparator<DirectoryItem> comparator(String sort, String order) {
        Comparator<DirectoryItem> base = "modified".equals(sort)
                ? Comparator.comparing(DirectoryItem::modified, Comparator.nullsLast(Comparator.naturalOrder()))
                : Comparator.comparing(DirectoryItem::name, String.CASE_INSENSITIVE_ORDER);
        return "desc".equalsIgnoreCase(order) ? base.reversed() : base;
    }
}
