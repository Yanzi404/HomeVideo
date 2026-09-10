package art.ayachinene.homevideo.controller;

import java.util.List;

import java.io.InputStream;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import art.ayachinene.homevideo.dto.VideoInfoResponse;
import art.ayachinene.homevideo.service.VideoService;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @GetMapping("/info")
    public ResponseEntity<VideoInfoResponse> getVideoInfo(@RequestParam String path) {
        return ResponseEntity.ok(videoService.getVideoInfo(path));
    }

    @GetMapping("/stream")
    public ResponseEntity<InputStreamResource> streamVideo(
            @RequestParam String path,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) {
        Resource resource = videoService.getVideoResource(path);
        try {
            long contentLength = resource.contentLength();
            MediaType contentType = MediaType.APPLICATION_OCTET_STREAM;

            if (rangeHeader != null && !rangeHeader.isBlank()) {
                List<HttpRange> ranges = HttpRange.parseRanges(rangeHeader);
                if (!ranges.isEmpty()) {
                    HttpRange range = ranges.get(0);
                    long start = range.getRangeStart(contentLength);
                    long end = range.getRangeEnd(contentLength);
                    long actualEnd = (end >= contentLength) ? contentLength - 1 : end;
                    long length = actualEnd - start + 1;

                    InputStream is = resource.getInputStream();
                    long skipped = is.skip(start);
                    InputStream bounded = new java.io.InputStream() {
                        long remaining = length;
                        @Override public int read() throws java.io.IOException {
                            if (remaining <= 0) return -1;
                            int b = is.read();
                            if (b >= 0) remaining--;
                            return b;
                        }
                        @Override public int read(byte[] buf, int off, int len) throws java.io.IOException {
                            if (remaining <= 0) return -1;
                            int toRead = (int) Math.min(len, remaining);
                            int n = is.read(buf, off, toRead);
                            if (n > 0) remaining -= n;
                            return n;
                        }
                    };

                    return ResponseEntity.status(206)
                            .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                            .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + actualEnd + "/" + contentLength)
                            .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(length))
                            .contentType(contentType)
                            .body(new InputStreamResource(bounded));
                }
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength))
                    .contentType(contentType)
                    .body(new InputStreamResource(resource.getInputStream()));
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to stream video", e);
        }
    }

    @GetMapping("/thumbnail")
    public ResponseEntity<byte[]> getThumbnail(@RequestParam String path) {
        byte[] data = videoService.getThumbnail(path);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(data);
    }

    @GetMapping("/subtitles")
    public ResponseEntity<String> getSubtitle(
            @RequestParam String path,
            @RequestParam String name) {
        String content = videoService.getSubtitleContent(path, name);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "plain", java.nio.charset.StandardCharsets.UTF_8))
                .body(content);
    }
}
