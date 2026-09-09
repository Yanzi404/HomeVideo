package art.ayachinene.homevideo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import art.ayachinene.homevideo.config.HomeVideoProperties;

@SpringBootApplication
@EnableConfigurationProperties(HomeVideoProperties.class)
public class HomeVideoApplication {

    public static void main(String[] args) {
        SpringApplication.run(HomeVideoApplication.class, args);
    }
}
