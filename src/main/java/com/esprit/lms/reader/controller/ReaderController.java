package com.esprit.lms.reader.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/reader")
public class ReaderController {

    @GetMapping("/hello")
    public Map<String, String> hello() {
        return Map.of("module", "reader", "status", "ready");
    }

    // TODO Sprint 5: POST /api/v1/reader/session/{itemId}
    // TODO Sprint 5: GET  /api/v1/reader/progress/{itemId}
    // TODO Sprint 5: PUT  /api/v1/reader/progress/{itemId}
}
