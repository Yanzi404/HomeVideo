package art.ayachinene.homevideo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import art.ayachinene.homevideo.config.HomeVideoProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HomeVideoApplicationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HomeVideoProperties properties;

    @Test
    @DisplayName("应用上下文加载成功")
    void contextLoads() {
        assertThat(properties).isNotNull();
        assertThat(properties.getMediaDir()).isNotBlank();
    }

    @Test
    @DisplayName("GET /api/health 返回 200 和 status=ok")
    void healthEndpoint_returnsOk() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }
}
