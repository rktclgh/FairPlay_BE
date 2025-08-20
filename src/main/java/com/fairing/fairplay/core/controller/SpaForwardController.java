package com.fairing.fairplay.core.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {
    @GetMapping({
        "/{path:[^\\.]*}", 
        "/{path:[^\\.]*}/**", 
        "/auth/kakao/callback",
        "/eventdetail/**",
        "/booth/**",
        "/mypage/**",
        "/host/**",
        "/admin_dashboard/**",
        "/booth-admin/**"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
