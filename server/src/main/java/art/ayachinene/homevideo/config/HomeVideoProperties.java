package art.ayachinene.homevideo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "homevideo")
public class HomeVideoProperties {

    private final String mediaDir;

    @ConstructorBinding
    public HomeVideoProperties(String mediaDir) {
        this.mediaDir = mediaDir;
    }

    public String getMediaDir() {
        return mediaDir;
    }
}
