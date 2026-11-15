package com.academy.dashboard;

import com.academy.auth.CurrentUser;
import com.academy.dashboard.DashboardDtos.Dashboard;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final CurrentUser currentUser;
    private final DashboardService dashboard;

    public DashboardController(CurrentUser currentUser, DashboardService dashboard) {
        this.currentUser = currentUser;
        this.dashboard = dashboard;
    }

    @GetMapping
    public Dashboard get(Authentication auth) {
        return dashboard.build(currentUser.require(auth));
    }
}
