package com.academy.auth;

import com.academy.user.AppUser;
import com.academy.user.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Component
public class CurrentUser {

    private final AppUserRepository users;

    public CurrentUser(AppUserRepository users) {
        this.users = users;
    }

    public AppUser require(Authentication auth) {
        if (auth == null) throw new ResponseStatusException(UNAUTHORIZED);
        return users.findByUsername(auth.getName()).orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED));
    }
}
