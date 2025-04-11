package capston.capston_spring.controller;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AccuracySessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser
    void getAccuracySessions() throws Exception {
        mockMvc.perform(get("/accuracy-session/user/me"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void getAccuracyBySong() throws Exception {
        mockMvc.perform(get("/accuracy-session/song/1/user/me"))
                .andExpect(status().isOk());
    }
}

