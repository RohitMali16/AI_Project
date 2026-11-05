package com.interviewprep.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GroqService {

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${app.groq.api-key:}")
    private String apiKey;

    @Value("${app.groq.model:openai/gpt-oss-20b}")
    private String model;

    private String ask(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Groq API key is missing. Add GROQ_API_KEY in Eclipse Run Configurations."
            );
        }

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content", "You are a helpful AI assistant that helps students prepare for technical interviews. Follow the user's requested output format exactly."
                        ),
                        Map.of("role", "user", "content", prompt)
                )
        );

        // Retry a few times for temporary server or rate-limit errors.
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                JsonNode response = restClient.post()
                        .uri("https://api.groq.com/openai/v1/chat/completions")
                        .header("Authorization", "Bearer " + apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody)
                        .retrieve()
                        .body(JsonNode.class);

                String answer = response == null
                        ? ""
                        : response.path("choices").path(0)
                                  .path("message").path("content").asText("");

                if (answer.isBlank()) {
                    throw new IllegalStateException("Groq returned an empty response. Please try again.");
                }
                return answer.trim();

            } catch (RestClientResponseException ex) {
                int status = ex.getStatusCode().value();

                if ((status == 429 || status >= 500) && attempt < 3) {
                    pauseBeforeRetry(attempt);
                    continue;
                }

                if (status == 401 || status == 403) {
                    throw new IllegalStateException(
                            "Groq rejected the API key. Check that GROQ_API_KEY is correct and active."
                    );
                }
                if (status == 404) {
                    throw new IllegalStateException(
                            "The Groq model was not found. Check app.groq.model in application.properties."
                    );
                }
                if (status == 429) {
                    throw new IllegalStateException(
                            "Groq rate limit reached. Please wait a little and try again."
                    );
                }
                if (status >= 500) {
                    throw new IllegalStateException(
                            "Groq is temporarily unavailable. Please try again in a minute."
                    );
                }

                throw new IllegalStateException(
                        "Groq API request failed (HTTP " + status + "). Check the backend console for details."
                );
            }
        }

        throw new IllegalStateException("Groq is temporarily unavailable. Please try again.");
    }

    private void pauseBeforeRetry(int attempt) {
        try {
            Thread.sleep(1000L * attempt);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("The AI request was interrupted.", ex);
        }
    }

    public List<String> generateQuestions(String role) {
        String prompt = """
                Generate exactly 5 useful technical interview questions for this job role:
                %s

                Mix beginner, intermediate and advanced questions.
                Return ONLY valid JSON in exactly this format:
                {"questions":["question 1","question 2","question 3","question 4","question 5"]}
                Do not add markdown fences or any text outside the JSON.
                """.formatted(role);

        JsonNode json = parseJson(ask(prompt));
        JsonNode questions = json.path("questions");

        if (!questions.isArray() || questions.size() != 5) {
            throw new IllegalStateException(
                    "The AI did not return exactly five questions. Please try again."
            );
        }

        List<String> result = new ArrayList<>();
        questions.forEach(question -> result.add(question.asText()));
        return result;
    }

    public Map<String, Object> evaluateAnswer(String role, String question, String answer) {
        String prompt = """
                Act as a fair technical interviewer for the role: %s

                Interview question: %s
                Candidate's answer: %s

                Evaluate the answer fairly. Return ONLY valid JSON in this format:
                {
                  "score": 7,
                  "strengths": ["..."],
                  "missingPoints": ["..."],
                  "suggestions": ["..."],
                  "improvedAnswer": "..."
                }

                The score must be an integer from 0 to 10.
                Do not add markdown fences or text outside the JSON.
                """.formatted(role, question, answer);

        JsonNode json = parseJson(ask(prompt));
        int score = Math.max(0, Math.min(10, json.path("score").asInt(-1)));

        if (score < 0) {
            throw new IllegalStateException("The AI returned an invalid score. Please try again.");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("score", score);
        result.put("strengths", stringList(json.path("strengths")));
        result.put("missingPoints", stringList(json.path("missingPoints")));
        result.put("suggestions", stringList(json.path("suggestions")));
        result.put("improvedAnswer", json.path("improvedAnswer").asText(""));
        return result;
    }

    public String explainConcept(String topic) {
        String prompt = """
                Explain this concept to a beginner preparing for a technical interview:
                %s

                Use simple language, one practical analogy and short numbered steps.
                Keep the explanation under 200 words.
                """.formatted(topic);

        return ask(prompt);
    }

    private JsonNode parseJson(String raw) {
        String cleaned = raw
                .replaceAll("(?s)^```(?:json)?\\s*", "")
                .replaceAll("\\s*```$", "")
                .trim();

        // If the model adds a short sentence around the JSON, isolate the JSON object.
        int firstBrace = cleaned.indexOf('{');
        int lastBrace = cleaned.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            cleaned = cleaned.substring(firstBrace, lastBrace + 1);
        }

        try {
            return mapper.readTree(cleaned);
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "The AI response was not valid JSON. Please try again.", ex
            );
        }
    }

    private List<String> stringList(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node.isArray()) {
            node.forEach(item -> values.add(item.asText()));
        }
        return values;
    }
}
