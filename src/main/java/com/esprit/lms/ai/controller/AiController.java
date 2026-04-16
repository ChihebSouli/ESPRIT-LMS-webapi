package com.esprit.lms.ai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    @GetMapping("/hello")
    public Map<String, String> hello() {
        return Map.of("module", "ai", "status", "ready");
    }

    // TODO Sprint 8: GET  /api/v1/ai/recommendations
    // TODO Sprint 9: POST /api/v1/ai/chat (SSE streaming)
    // TODO Sprint 9: GET  /api/v1/ai/summary/{itemId}
    // TODO Sprint 9: POST /api/v1/ai/summary/{itemId} (regenerate)
}
