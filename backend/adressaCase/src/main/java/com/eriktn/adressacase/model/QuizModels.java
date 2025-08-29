package com.eriktn.adressacase.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

// Modeller for quiz-generering
public class QuizModels {
    public static class GenerateQuizRequest {
        public List<Object> articles;
        public Integer numQuestions = 8;
        public String difficulty = "mixed";
        public String locale = "nb-NO";
    }

    // DTO for quiz-resultat
    public static class QuizDTO {
        public String title;
        public String locale;
        public List<Question> questions;
        public Meta meta;
    }

    // Metadata om quiz-genereringen
    public static class Meta {
        public Instant createdAt;
        public List<String> sourceIds;
    }

    // Enkelt spørsmål
    public static class Question {
        public String id;
        public String sourceId;
        public String sourceTitle;
        public String difficulty;
        public String prompt;
        public List<String> options;
        public int answerIndex;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public String explanation;
        @JsonProperty("tags")
        public List<String> tags;
    }
}

