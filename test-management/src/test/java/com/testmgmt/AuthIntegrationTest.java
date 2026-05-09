package com.testmgmt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void loginWithInvalidCredentials_returns401() throws Exception {
        Map<String, String> body = Map.of(
            "email",    "nobody@example.com",
            "password", "wrongpassword"
        );
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void registerNewUser_returns201() throws Exception {
        Map<String, String> body = Map.of(
            "username", "testuser",
            "email",    "testuser@example.com",
            "password", "SecurePass@123",
            "fullName", "Test User",
            "team",     "QA-Alpha"
        );
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("testuser@example.com"))
            .andExpect(jsonPath("$.role").value("TESTER"));
    }

    @Test
    void registerDuplicateEmail_returns409() throws Exception {
        Map<String, String> body = Map.of(
            "username", "dupeuser",
            "email",    "testuser@example.com",   // same as above
            "password", "SecurePass@123",
            "fullName", "Duplicate User",
            "team",     "QA-Beta"
        );
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isConflict());
    }
}
