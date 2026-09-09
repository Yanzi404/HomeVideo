package art.ayachinene.homevideo.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FileServiceTest {

    @Autowired
    private MockMvc mockMvc;

    static Path tempDir;

    static {
        try {
            tempDir = Files.createTempDirectory("homevideo-test");

            Files.createFile(tempDir.resolve("family.mp4"));
            Files.setLastModifiedTime(tempDir.resolve("family.mp4"),
                    FileTime.from(Instant.parse("2024-01-01T00:00:00Z")));

            Files.createFile(tempDir.resolve("movie.mkv"));
            Files.setLastModifiedTime(tempDir.resolve("movie.mkv"),
                    FileTime.from(Instant.parse("2024-06-01T00:00:00Z")));

            Files.createFile(tempDir.resolve("notes.txt"));
            Files.createFile(tempDir.resolve("photo.jpg"));

            Path sub = Files.createDirectory(tempDir.resolve("sub"));
            Files.createFile(sub.resolve("clip.avi"));
            Files.setLastModifiedTime(sub,
                    FileTime.from(Instant.parse("2023-01-01T00:00:00Z")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("homevideo.media-dir", () -> tempDir.toAbsolutePath().toString());
    }

    @Test
    @DisplayName("列出根目录 — 仅返回视频文件和子文件夹，过滤非视频文件")
    void listFiles_root_returnsOnlyVideosAndFolders() throws Exception {
        mockMvc.perform(get("/api/files").param("path", "/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items[0].name").value("family.mp4"))
                .andExpect(jsonPath("$.items[0].type").value("file"))
                .andExpect(jsonPath("$.items[0].size").isNumber())
                .andExpect(jsonPath("$.items[1].name").value("movie.mkv"))
                .andExpect(jsonPath("$.items[2].name").value("sub"))
                .andExpect(jsonPath("$.items[2].type").value("folder"));
    }

    @Test
    @DisplayName("列出子目录内容")
    void listFiles_subDirectory() throws Exception {
        mockMvc.perform(get("/api/files").param("path", "/sub"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].name").value("clip.avi"))
                .andExpect(jsonPath("$.path").value("/sub"));
    }

    @Test
    @DisplayName("分页 — 每页 2 条")
    void listFiles_pagination() throws Exception {
        mockMvc.perform(get("/api/files")
                        .param("path", "/")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.total").value(3));
    }

    @Test
    @DisplayName("按文件名升序排列")
    void listFiles_sortByName() throws Exception {
        mockMvc.perform(get("/api/files")
                        .param("path", "/")
                        .param("sort", "name")
                        .param("order", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("family.mp4"))
                .andExpect(jsonPath("$.items[1].name").value("movie.mkv"))
                .andExpect(jsonPath("$.items[2].name").value("sub"));
    }

    @Test
    @DisplayName("按修改时间降序排列")
    void listFiles_sortByModifiedDesc() throws Exception {
        mockMvc.perform(get("/api/files")
                        .param("path", "/")
                        .param("sort", "modified")
                        .param("order", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("movie.mkv"))
                .andExpect(jsonPath("$.items[1].name").value("family.mp4"));
    }

    @Test
    @DisplayName("搜索 — 返回匹配的视频文件")
    void searchFiles_matchingKeyword() throws Exception {
        mockMvc.perform(get("/api/files/search").param("keyword", "clip"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("clip.avi"))
                .andExpect(jsonPath("$[0].path").value("sub/clip.avi"));
    }

    @Test
    @DisplayName("搜索 — 无匹配返回空列表")
    void searchFiles_noMatch() throws Exception {
        mockMvc.perform(get("/api/files/search").param("keyword", "不存在"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("搜索 — 不返回非视频文件")
    void searchFiles_doesNotReturnNonVideoFiles() throws Exception {
        mockMvc.perform(get("/api/files/search").param("keyword", "notes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("路径遍历 — 返回 400")
    void listFiles_pathTraversal_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/files").param("path", "/../../../etc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("不存在的路径 — 返回 400")
    void listFiles_nonExistentPath_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/files").param("path", "/nonexistent"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("支持的视频格式全部正确返回")
    void listFiles_allSupportedFormats() throws Exception {
        for (String ext : new String[]{"mp4", "mkv", "avi", "mov", "ts", "flv", "wmv", "rmvb"}) {
            Files.createFile(tempDir.resolve("test." + ext));
        }

        var result = mockMvc.perform(get("/api/files").param("path", "/").param("size", "200"))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        for (String ext : new String[]{"mp4", "mkv", "avi", "mov", "ts", "flv", "wmv", "rmvb"}) {
            assertTrue(json.contains("test." + ext), "Missing format: " + ext);
        }
    }
}
