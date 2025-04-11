package capston.capston_spring.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PasswordResetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void passwordResetTest() throws Exception {
        mockMvc.perform(post("/password-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"exampleyong2024@example.com\"}"))
                .andExpect(status().isOk());
    }
}
