package com.interviewprep.service;

import com.interviewprep.entity.*;
import com.interviewprep.repository.InterviewSessionRepository;
import com.interviewprep.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class InterviewService {
    private final InterviewSessionRepository sessions;
    private final UserRepository users;
    private final GroqService groq;

    public InterviewService(InterviewSessionRepository sessions, UserRepository users, GroqService groq) {
        this.sessions = sessions;
        this.users = users;
        this.groq = groq;
    }

    @Transactional
    public Map<String, Object> generate(Long userId, String role) {
        User user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found."));
        List<String> generated = groq.generateQuestions(role);
        InterviewSession session = new InterviewSession(user, role.trim());
        generated.forEach(q -> session.addQuestion(new InterviewQuestion(q)));
        sessions.save(session);

        List<Map<String, Object>> questionList = new ArrayList<>();
        for (int i = 0; i < session.getQuestions().size(); i++) {
            questionList.add(Map.of("index", i, "question", session.getQuestions().get(i).getQuestionText()));
        }
        return Map.of("sessionId", session.getId(), "role", session.getRole(), "questions", questionList);
    }

    @Transactional
    public Map<String, Object> evaluate(Long userId, Long sessionId, int questionIndex, String answer) {
        InterviewSession session = sessions.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Interview session not found."));
        if (questionIndex < 0 || questionIndex >= session.getQuestions().size()) {
            throw new IllegalArgumentException("Question index is invalid.");
        }

        InterviewQuestion question = session.getQuestions().get(questionIndex);
        Map<String, Object> evaluation = groq.evaluateAnswer(session.getRole(), question.getQuestionText(), answer);
        question.evaluate(answer, (Integer) evaluation.get("score"),
                castList(evaluation.get("strengths")), castList(evaluation.get("missingPoints")),
                castList(evaluation.get("suggestions")), (String) evaluation.get("improvedAnswer"));
        return Map.of("questionIndex", questionIndex, "evaluation", evaluation);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> history(Long userId) {
        return sessions.findAllByUserIdOrderByCreatedAtDesc(userId).stream().map(s -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("sessionId", s.getId());
            item.put("role", s.getRole());
            item.put("createdAt", s.getCreatedAt());
            item.put("questions", s.getQuestions().stream().map(q -> {
                Map<String, Object> question = new LinkedHashMap<>();
                question.put("question", q.getQuestionText());
                question.put("answer", q.getAnswer());
                question.put("score", q.getScore());
                question.put("strengths", q.getStrengths());
                question.put("missingPoints", q.getMissingPoints());
                question.put("suggestions", q.getSuggestions());
                question.put("improvedAnswer", q.getImprovedAnswer());
                return question;
            }).toList());
            return item;
        }).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> stats(Long userId) {
        List<InterviewSession> all = sessions.findAllByUserIdOrderByCreatedAtDesc(userId);
        int count = 0, sum = 0;
        Map<String, int[]> roleStats = new HashMap<>();
        for (InterviewSession session : all) {
            for (InterviewQuestion q : session.getQuestions()) {
                if (q.getScore() != null) {
                    count++;
                    sum += q.getScore();
                    int[] stat = roleStats.computeIfAbsent(session.getRole(), k -> new int[2]);
                    stat[0] += q.getScore();
                    stat[1]++;
                }
            }
        }
        List<String> weak = new ArrayList<>(), strong = new ArrayList<>();
        roleStats.forEach((role, stat) -> {
            double avg = (double) stat[0] / stat[1];
            if (avg < 6) weak.add(role);
            else if (avg >= 8) strong.add(role);
        });
        return Map.of("totalQuestionsAttempted", count, "averageScore",
                count == 0 ? 0.0 : Math.round((double) sum / count * 10.0) / 10.0,
                "weakTopics", weak, "strongTopics", strong);
    }

    @SuppressWarnings("unchecked")
    private List<String> castList(Object value) {
        return value instanceof List<?> list ? (List<String>) list : List.of();
    }
}
