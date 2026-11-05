package com.interviewprep.controller;

import com.interviewprep.dto.TopicRequest;
import com.interviewprep.service.GroqService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {
    private final GroqService groq;

    public AssistantController(GroqService groq) { this.groq = groq; }

    @PostMapping("/explain")
    public Map<String, String> explain(@Valid @RequestBody TopicRequest request, HttpSession session) {
        if (session.getAttribute("userId") == null) throw new IllegalStateException("Please log in first.");
        return Map.of("topic", request.topic(), "explanation", groq.explainConcept(request.topic()));
    }
}
