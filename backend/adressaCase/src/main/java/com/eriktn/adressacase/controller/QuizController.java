package com.eriktn.adressacase.controller;

import com.eriktn.adressacase.model.QuizModels.GenerateQuizRequest;
import com.eriktn.adressacase.model.QuizModels.QuizDTO;
import com.eriktn.adressacase.service.OpenAIQuizService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {
    private final OpenAIQuizService service;
    public QuizController(OpenAIQuizService service) { this.service = service; }

    @PostMapping(value = "/generate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public QuizDTO generate(@Valid @RequestBody GenerateQuizRequest req) {
        return service.generateQuiz(req);
    }
}

