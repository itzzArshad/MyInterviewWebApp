package com.academy.today;

import com.academy.auth.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/today")
public class TodayController {

    private final TodayService today;
    private final CurrentUser currentUser;

    public TodayController(TodayService today, CurrentUser currentUser) {
        this.today = today;
        this.currentUser = currentUser;
    }

    @GetMapping
    public TodayService.TodayView get(Authentication auth) {
        return today.today(currentUser.require(auth));
    }
}
