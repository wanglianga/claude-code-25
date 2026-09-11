package com.nursery.controller;

import com.nursery.config.AuthUser;
import com.nursery.service.DashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static com.nursery.controller.ChildController.toUser;

@RestController
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/api/dashboard")
    public Map<String, Object> dashboard(@AuthenticationPrincipal AuthUser user) {
        return dashboardService.build(toUser(user));
    }
}
