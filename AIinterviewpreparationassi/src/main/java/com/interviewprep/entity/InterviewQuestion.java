package com.interviewprep.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "interview_questions")
public class InterviewQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id")
    private InterviewSession session;

    @Column(nullable = false, length = 2000)
    private String questionText;

    @Column(columnDefinition = "TEXT")
    private String answer;

    private Integer score;

    @ElementCollection
    @CollectionTable(name = "question_strengths", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "strength", length = 500)
    private List<String> strengths = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "question_missing_points", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "missing_point", length = 500)
    private List<String> missingPoints = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "question_suggestions", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "suggestion", length = 500)
    private List<String> suggestions = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String improvedAnswer;

    private LocalDateTime answeredAt;

    protected InterviewQuestion() {}

    public InterviewQuestion(String questionText) {
        this.questionText = questionText;
    }

    public void setSession(InterviewSession session) { this.session = session; }
    public void evaluate(String answer, int score, List<String> strengths,
                         List<String> missingPoints, List<String> suggestions,
                         String improvedAnswer) {
        this.answer = answer;
        this.score = score;
        this.strengths = strengths == null ? new ArrayList<>() : strengths;
        this.missingPoints = missingPoints == null ? new ArrayList<>() : missingPoints;
        this.suggestions = suggestions == null ? new ArrayList<>() : suggestions;
        this.improvedAnswer = improvedAnswer;
        this.answeredAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getQuestionText() { return questionText; }
    public String getAnswer() { return answer; }
    public Integer getScore() { return score; }
    public List<String> getStrengths() { return strengths; }
    public List<String> getMissingPoints() { return missingPoints; }
    public List<String> getSuggestions() { return suggestions; }
    public String getImprovedAnswer() { return improvedAnswer; }
    public LocalDateTime getAnsweredAt() { return answeredAt; }
}
