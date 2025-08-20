package com.fairing.fairplay.core.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {
    @GetMapping(value = {
        "/auth/kakao/callback",
        "/eventdetail/**",
        "/booth/**",
        "/mypage/**",
        "/host/**",
        "/admin_dashboard/**",
        "/booth-admin/**",
        "/login",
        "/register",
        "/eventoverview",
        "/event-registration-intro"
    }, produces = "text/html")
    public String forward() {
        return "forward:/index.html";
    }
}
