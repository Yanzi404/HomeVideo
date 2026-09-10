package art.ayachinene.homevideo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "playback_history")
public class PlaybackHistory {

    @Id
    @Column(length = 500)
    private String videoPath;

    private long position;
    private long duration;
    private long lastPlayed;

    public PlaybackHistory() {
    }

    public PlaybackHistory(String videoPath, long position, long duration, long lastPlayed) {
        this.videoPath = videoPath;
        this.position = position;
        this.duration = duration;
        this.lastPlayed = lastPlayed;
    }

    public String getVideoPath() { return videoPath; }
    public void setVideoPath(String videoPath) { this.videoPath = videoPath; }
    public long getPosition() { return position; }
    public void setPosition(long position) { this.position = position; }
    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }
    public long getLastPlayed() { return lastPlayed; }
    public void setLastPlayed(long lastPlayed) { this.lastPlayed = lastPlayed; }
}
