package com.academy.auth;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowTest {

    private static final String COOKIE = "ACADEMY_TOKEN";

    @Autowired
    MockMvc mvc;
    @Autowired
    LoginRateLimiter limiter;

    @BeforeEach
    @AfterEach  // the Spring context (and limiter) is shared with other test classes
    void resetLimiter() {
        limiter.reset("127.0.0.1");
    }

    private MvcResult login(String username, String password) throws Exception {
        return mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andReturn();
    }

    @Test
    void loginSetsSecureCookieAndUnlocksProtectedEndpoints() throws Exception {
        MvcResult res = login("tester", "correct-horse-battery");
        assertThat(res.getResponse().getStatus()).isEqualTo(200);

        String setCookie = res.getResponse().getHeader("Set-Cookie");
        assertThat(setCookie).contains(COOKIE + "=").contains("HttpOnly").contains("SameSite=Strict");

        Cookie cookie = res.getResponse().getCookie(COOKIE);
        assertThat(cookie).isNotNull();

        mvc.perform(get("/api/auth/me").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("tester"))
                .andExpect(jsonPath("$.displayName").value("Tester"));

        mvc.perform(get("/api/dashboard").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.streakDays").value(0))
                .andExpect(jsonPath("$.modules[0].key").value("today"));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        MvcResult res = login("tester", "wrong-password");
        assertThat(res.getResponse().getStatus()).isEqualTo(401);
        assertThat(res.getResponse().getHeader("Set-Cookie")).isNull();
    }

    @Test
    void unknownUserIsRejected() throws Exception {
        assertThat(login("nobody", "whatever-123").getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void protectedEndpointsRequireAuthentication() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/dashboard").cookie(new Cookie(COOKIE, "not-a-jwt"))).andExpect(status().isUnauthorized());
    }

    @Test
    void healthIsPublic() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void repeatedFailuresAreRateLimited() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertThat(login("tester", "wrong-" + i).getResponse().getStatus()).isEqualTo(401);
        }
        // Even the correct password is refused while blocked.
        assertThat(login("tester", "correct-horse-battery").getResponse().getStatus()).isEqualTo(429);
    }

    @Test
    void invalidBodyReturnsProblemJson() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logoutClearsCookie() throws Exception {
        MvcResult res = mvc.perform(post("/api/auth/logout")).andExpect(status().isNoContent()).andReturn();
        assertThat(res.getResponse().getHeader("Set-Cookie")).contains("Max-Age=0");
    }
}
