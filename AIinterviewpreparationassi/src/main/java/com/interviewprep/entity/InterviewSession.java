package com.interviewprep.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "interview_sessions")
public class InterviewSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 100)
    private String role;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<InterviewQuestion> questions = new ArrayList<>();

    protected InterviewSession() {}

    public InterviewSession(User user, String role) {
        this.user = user;
        this.role = role;
    }

    public void addQuestion(InterviewQuestion question) {
        questions.add(question);
        question.setSession(this);
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getRole() { return role; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<InterviewQuestion> getQuestions() { return questions; }
}
