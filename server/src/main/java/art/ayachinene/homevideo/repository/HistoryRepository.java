package art.ayachinene.homevideo.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import art.ayachinene.homevideo.entity.PlaybackHistory;

public interface HistoryRepository extends JpaRepository<PlaybackHistory, String> {

    List<PlaybackHistory> findAllByOrderByLastPlayedDesc();

    long count();

    void deleteByVideoPath(String videoPath);
}
