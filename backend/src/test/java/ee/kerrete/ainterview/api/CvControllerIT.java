package ee.kerrete.ainterview.api;

import ee.kerrete.ainterview.AbstractIntegrationTest;
import ee.kerrete.ainterview.model.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CvControllerIT extends AbstractIntegrationTest {

    @Test
    void extractText_withValidFile_returnsContent() throws Exception {
        createUser("cv@example.com", "Password1!", true, UserRole.USER);
        String token = loginAndGetToken("cv@example.com", "Password1!");

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "cv.txt",
            MediaType.TEXT_PLAIN_VALUE,
            "This is a sample CV text".getBytes()
        );

        mockMvc.perform(multipart("/api/cv/extract-text")
                .file(file)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.text").isNotEmpty());
    }

    @Test
    void extractText_withEmptyFile_returnsBadRequest() throws Exception {
        createUser("cv2@example.com", "Password1!", true, UserRole.USER);
        String token = loginAndGetToken("cv2@example.com", "Password1!");

        MockMultipartFile file = new MockMultipartFile(
            "file",
            "cv.txt",
            MediaType.TEXT_PLAIN_VALUE,
            new byte[0]
        );

        mockMvc.perform(multipart("/api/cv/extract-text")
                .file(file)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isBadRequest());
    }
}

