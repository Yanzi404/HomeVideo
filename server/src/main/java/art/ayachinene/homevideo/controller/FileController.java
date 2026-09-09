package art.ayachinene.homevideo.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import art.ayachinene.homevideo.dto.DirectoryItem;
import art.ayachinene.homevideo.dto.DirectoryResponse;
import art.ayachinene.homevideo.service.FileService;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping
    public ResponseEntity<DirectoryResponse> listFiles(
            @RequestParam(defaultValue = "/") String path,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "asc") String order) {
        return ResponseEntity.ok(fileService.listFiles(path, page, size, sort, order));
    }

    @GetMapping("/search")
    public ResponseEntity<List<DirectoryItem>> searchFiles(@RequestParam String keyword) {
        return ResponseEntity.ok(fileService.searchFiles(keyword));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
