package com.fairing.fairplay.core.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {
    @GetMapping({"/{path:[^\\.]*}", "/auth/kakao/callback"})
    public String forward() {
        return "forward:/index.html";
    }
}
