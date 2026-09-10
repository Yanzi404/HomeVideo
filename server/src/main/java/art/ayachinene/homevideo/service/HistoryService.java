package art.ayachinene.homevideo.service;

import java.nio.file.Path;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import art.ayachinene.homevideo.dto.HistoryItem;
import art.ayachinene.homevideo.entity.PlaybackHistory;
import art.ayachinene.homevideo.repository.HistoryRepository;

@Service
public class HistoryService {

    private static final int MAX_HISTORY = 50;

    private final HistoryRepository repository;

    public HistoryService(HistoryRepository repository) {
        this.repository = repository;
    }

    public List<HistoryItem> getHistory() {
        return repository.findAllByOrderByLastPlayedDesc()
                .stream()
                .map(this::toItem)
                .toList();
    }

    @Transactional
    public HistoryItem saveOrUpdate(String videoPath, long position, long duration) {
        PlaybackHistory entity = repository.findById(videoPath)
                .orElse(new PlaybackHistory());
        entity.setVideoPath(videoPath);
        entity.setPosition(position);
        entity.setDuration(duration);
        entity.setLastPlayed(System.currentTimeMillis());
        repository.save(entity);

        long count = repository.count();
        if (count > MAX_HISTORY) {
            List<PlaybackHistory> all = repository.findAllByOrderByLastPlayedDesc();
            List<PlaybackHistory> toRemove = all.subList(MAX_HISTORY, all.size());
            repository.deleteAll(toRemove);
        }

        return toItem(entity);
    }

    @Transactional
    public void deleteByVideoPath(String videoPath) {
        repository.deleteById(videoPath);
    }

    @Transactional
    public void clearAll() {
        repository.deleteAll();
    }

    private HistoryItem toItem(PlaybackHistory entity) {
        String videoName = Path.of(entity.getVideoPath()).getFileName().toString();
        return new HistoryItem(
                entity.getVideoPath(),
                videoName,
                entity.getPosition(),
                entity.getDuration(),
                entity.getLastPlayed()
        );
    }
}
