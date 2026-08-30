package com.neopick.adapter.web.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the Vue single-page app (history-mode router) by forwarding its
 * client-side routes to index.html. Spring Boot already serves index.html at
 * "/" and static assets under /assets/**; this covers the deep links.
 */
@Controller
public class SpaController {

    @GetMapping({"/login", "/bookings", "/chat", "/teachers/**"})
    public String forward() {
        return "forward:/index.html";
    }
}
