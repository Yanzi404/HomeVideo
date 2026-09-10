package art.ayachinene.homevideo.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import art.ayachinene.homevideo.dto.HistoryItem;
import art.ayachinene.homevideo.dto.HistoryRequest;
import art.ayachinene.homevideo.service.HistoryService;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    public ResponseEntity<List<HistoryItem>> getHistory() {
        return ResponseEntity.ok(historyService.getHistory());
    }

    @PostMapping
    public ResponseEntity<HistoryItem> saveHistory(@RequestBody HistoryRequest request) {
        return ResponseEntity.ok(historyService.saveOrUpdate(
                request.videoPath(), request.position(), request.duration()));
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteHistory(
            @RequestParam(required = false) String videoPath) {
        if (videoPath != null && !videoPath.isBlank()) {
            historyService.deleteByVideoPath(videoPath);
        } else {
            historyService.clearAll();
        }
        return ResponseEntity.ok().build();
    }
}
