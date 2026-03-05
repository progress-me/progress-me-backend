package com.nhavronskyi.progressmebackend.dashboard;

import com.nhavronskyi.progressmebackend.dashboard.dto.DashboardResponse;
import com.nhavronskyi.progressmebackend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService service;

    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "30") int period
    ) {
        return ResponseEntity.ok(service.getDashboard(user.getId(), period));
    }
}
