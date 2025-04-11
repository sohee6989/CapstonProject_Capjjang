package capston.capston_spring.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void joinTest_invalidPasswords() throws Exception {
        mockMvc.perform(post("/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"name\": \"testuser\"," +
                                "\"email\": \"test@example.com\"," +
                                "\"password1\": \"1234\"," +
                                "\"password2\": \"abcd\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginTest_tokenValidated() throws Exception {
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"username\":\"testuser\"," +
                                "\"password\":\"password123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void refreshTokenTest_invalidToken() throws Exception {
        mockMvc.perform(post("/refresh")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.refresh.token"))
                .andExpect(status().isUnauthorized());
    }
}
