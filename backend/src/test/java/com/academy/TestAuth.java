package com.academy;

import jakarta.servlet.http.Cookie;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Logs the test owner in and returns the auth cookie. */
public final class TestAuth {
    private TestAuth() {
    }

    public static Cookie login(MockMvc mvc) throws Exception {
        return mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"tester\",\"password\":\"correct-horse-battery\"}"))
                .andReturn().getResponse().getCookie("ACADEMY_TOKEN");
    }
}
