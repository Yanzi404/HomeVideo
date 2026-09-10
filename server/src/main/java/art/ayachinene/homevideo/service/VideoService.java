package art.ayachinene.homevideo.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import art.ayachinene.homevideo.config.HomeVideoProperties;
import art.ayachinene.homevideo.dto.AudioTrack;
import art.ayachinene.homevideo.dto.SubtitleInfo;
import art.ayachinene.homevideo.dto.VideoInfoResponse;

@Service
public class VideoService {

    private static final String THUMBNAIL_DIR = "/tmp/homevideo-thumbnails";
    private static final Set<String> SUBTITLE_EXTENSIONS = Set.of("srt", "ass", "ssa", "vtt");

    private final Path mediaRoot;
    private final Gson gson = new Gson();

    public VideoService(HomeVideoProperties properties) {
        this.mediaRoot = Path.of(properties.getMediaDir()).normalize();
    }

    public VideoInfoResponse getVideoInfo(String path) {
        Path videoFile = resolveAndValidate(path);
        if (!Files.exists(videoFile)) {
            throw new IllegalArgumentException("Video not found: " + path);
        }

        JsonObject probeData = runFfprobe(videoFile);

        long duration = extractDuration(probeData);
        int width = 0, height = 0;
        String codec = "";
        String format = getExtension(videoFile.getFileName().toString());
        List<AudioTrack> audioTracks = new ArrayList<>();

        JsonArray streams = probeData.has("streams")
                ? probeData.getAsJsonArray("streams")
                : new JsonArray();

        for (int i = 0; i < streams.size(); i++) {
            JsonObject stream = streams.get(i).getAsJsonObject();
            String codecType = str(stream, "codec_type");

            if ("video".equals(codecType) && width == 0) {
                width = integer(stream, "width");
                height = integer(stream, "height");
                codec = str(stream, "codec_name");
            } else if ("audio".equals(codecType)) {
                String lang = tag(stream, "language");
                audioTracks.add(new AudioTrack(
                        audioTracks.size(),
                        str(stream, "codec_name"),
                        lang.isEmpty() ? "und" : lang,
                        str(stream, "channels")
                ));
            }
        }

        List<SubtitleInfo> subtitles = findSubtitles(videoFile);

        return new VideoInfoResponse(path, duration, width, height, codec, format, audioTracks, subtitles);
    }

    public Resource getVideoResource(String path) {
        Path videoFile = resolveAndValidate(path);
        if (!Files.exists(videoFile)) {
            throw new IllegalArgumentException("Video not found: " + path);
        }
        return new FileSystemResource(videoFile);
    }

    public byte[] getThumbnail(String path) {
        Path videoFile = resolveAndValidate(path);
        if (!Files.exists(videoFile)) {
            throw new IllegalArgumentException("Video not found: " + path);
        }

        String hash = videoFile.toString().hashCode() + "_" + videoFile.toFile().lastModified();
        Path thumbnailPath = Path.of(THUMBNAIL_DIR, hash + ".jpg");

        if (Files.exists(thumbnailPath)) {
            try {
                return Files.readAllBytes(thumbnailPath);
            } catch (IOException e) {
                // regenerate
            }
        }

        try {
            Files.createDirectories(Path.of(THUMBNAIL_DIR));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create thumbnail directory", e);
        }

        JsonObject probeData = runFfprobe(videoFile);
        long durationMs = extractDuration(probeData);
        long seekMs = (long) (durationMs * 0.1);
        String seekTime = String.format("%02d:%02d:%02d.%03d",
                seekMs / 3600000, (seekMs % 3600000) / 60000,
                (seekMs % 60000) / 1000, seekMs % 1000);

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-ss", seekTime, "-i", videoFile.toString(),
                    "-vframes", "1", "-q:v", "2",
                    "-y", thumbnailPath.toString()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.getInputStream().readAllBytes();
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new RuntimeException("ffmpeg thumbnail timed out");
            }
            if (process.exitValue() != 0) {
                throw new RuntimeException("ffmpeg thumbnail failed: " + process.exitValue());
            }
            return Files.readAllBytes(thumbnailPath);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to generate thumbnail", e);
        }
    }

    public String getSubtitleContent(String videoPath, String subtitleName) {
        Path videoFile = resolveAndValidate(videoPath);
        Path subtitleFile = videoFile.getParent().resolve(subtitleName);

        if (!Files.exists(subtitleFile)) {
            throw new IllegalArgumentException("Subtitle not found: " + subtitleName);
        }

        try {
            byte[] raw = Files.readAllBytes(subtitleFile);
            return decodeWithDetectedEncoding(raw);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read subtitle: " + subtitleName, e);
        }
    }

    private List<SubtitleInfo> findSubtitles(Path videoFile) {
        List<SubtitleInfo> result = new ArrayList<>();
        Path videoDir = videoFile.getParent();
        String videoBase = baseName(videoFile);

        try (var stream = Files.list(videoDir)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> baseName(p).equals(videoBase)
                            && SUBTITLE_EXTENSIONS.contains(getExtension(p.getFileName().toString())))
                    .forEach(p -> {
                        String lang = extractLangFromSubtitleName(p.getFileName().toString(), videoBase);
                        result.add(new SubtitleInfo(p.getFileName().toString(), lang));
                    });
        } catch (IOException e) {
            // ignore
        }
        return result;
    }

    private JsonObject runFfprobe(Path videoFile) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffprobe", "-v", "quiet",
                    "-print_format", "json",
                    "-show_format", "-show_streams",
                    videoFile.toString()
            );
            pb.redirectErrorStream(false);
            Process process = pb.start();

            String output;
            try (InputStream is = process.getInputStream()) {
                output = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new RuntimeException("ffprobe timed out");
            }
            if (process.exitValue() != 0) {
                throw new RuntimeException("ffprobe failed: " + process.exitValue());
            }

            return JsonParser.parseString(output).getAsJsonObject();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to run ffprobe", e);
        }
    }

    private long extractDuration(JsonObject probeData) {
        try {
            if (probeData.has("format")) {
                JsonObject format = probeData.getAsJsonObject("format");
                if (format.has("duration")) {
                    return (long) (Double.parseDouble(format.get("duration").getAsString()) * 1000);
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return 0;
    }

    private Path resolveAndValidate(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Path is required");
        }
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        Path resolved = mediaRoot.resolve(normalized).normalize();
        if (!resolved.startsWith(mediaRoot)) {
            throw new IllegalArgumentException("Invalid path: " + path);
        }
        return resolved;
    }

    private String getExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toLowerCase() : "";
    }

    private String baseName(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(0, dot) : name;
    }

    private String extractLangFromSubtitleName(String subName, String videoBase) {
        String remaining = subName.substring(videoBase.length());
        if (remaining.startsWith(".")) remaining = remaining.substring(1);
        int dot = remaining.lastIndexOf('.');
        if (dot >= 0) remaining = remaining.substring(0, dot);
        if (remaining.length() == 2 || remaining.length() == 3) {
            return remaining.toLowerCase();
        }
        return "und";
    }

    private String decodeWithDetectedEncoding(byte[] bytes) {
        if (isValidUtf8(bytes)) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        try {
            return new String(bytes, "GBK");
        } catch (Exception e) {
            return new String(bytes, StandardCharsets.ISO_8859_1);
        }
    }

    private boolean isValidUtf8(byte[] bytes) {
        int i = 0;
        while (i < bytes.length) {
            int b = bytes[i] & 0xFF;
            int needed;
            if (b < 0x80) { needed = 0; }
            else if (b < 0xC0) { return false; }
            else if (b < 0xE0) { needed = 1; }
            else if (b < 0xF0) { needed = 2; }
            else if (b < 0xF8) { needed = 3; }
            else { return false; }
            for (int j = 1; j <= needed; j++) {
                if (i + j >= bytes.length) return false;
                if ((bytes[i + j] & 0xC0) != 0x80) return false;
            }
            i += needed + 1;
        }
        return true;
    }

    private String str(JsonObject obj, String field) {
        return obj.has(field) && !obj.get(field).isJsonNull() ? obj.get(field).getAsString() : "";
    }

    private int integer(JsonObject obj, String field) {
        return obj.has(field) && !obj.get(field).isJsonNull() ? obj.get(field).getAsInt() : 0;
    }

    private String tag(JsonObject stream, String key) {
        if (!stream.has("tags") || stream.get("tags").isJsonNull()) return "";
        return str(stream.getAsJsonObject("tags"), key);
    }
}
