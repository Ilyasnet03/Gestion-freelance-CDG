package com.cdg.freelance.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping({"/login", "/dashboard"})
    public String forwardSpaRoutes() {
        return "forward:/index.html";
    }
}
