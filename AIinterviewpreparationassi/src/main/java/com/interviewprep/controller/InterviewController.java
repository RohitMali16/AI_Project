package com.interviewprep.controller;

import com.interviewprep.dto.AnswerRequest;
import com.interviewprep.dto.RoleRequest;
import com.interviewprep.service.InterviewService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class InterviewController {
    private final InterviewService interviews;

    public InterviewController(InterviewService interviews) { this.interviews = interviews; }

    @PostMapping("/questions/generate")
    public Map<String, Object> generate(@Valid @RequestBody RoleRequest request, HttpSession session) {
        return interviews.generate(userId(session), request.role());
    }

    @PostMapping("/answer/evaluate")
    public Map<String, Object> evaluate(@Valid @RequestBody AnswerRequest request, HttpSession session) {
        return interviews.evaluate(userId(session), request.sessionId(), request.questionIndex(), request.answer());
    }

    @GetMapping("/history")
    public Map<String, Object> history(HttpSession session) {
        return Map.of("sessions", interviews.history(userId(session)));
    }

    @GetMapping("/dashboard/stats")
    public Map<String, Object> stats(HttpSession session) {
        return interviews.stats(userId(session));
    }

    private Long userId(HttpSession session) {
        Object id = session.getAttribute("userId");
        if (!(id instanceof Long userId)) throw new IllegalStateException("Please log in first.");
        return userId;
    }
}
