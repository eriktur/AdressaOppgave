package com.eriktn.adressacase.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.eriktn.adressacase.model.QuizModels;
import com.eriktn.adressacase.model.QuizModels.GenerateQuizRequest;
import com.eriktn.adressacase.model.QuizModels.QuizDTO;
import com.eriktn.adressacase.util.JsonUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class OpenAIQuizService {
    private final ObjectMapper mapper = new ObjectMapper();
    private final OpenAIClient client;

    @Value("${openai.model:gpt-5-mini}")
    private String model;

    // Sett til true om du vil at parsing-feil skal gi mock i stedet for 500
    @Value("${app.fallbackOnParseError:true}")
    private boolean fallbackOnParseError;

    public OpenAIQuizService(@Value("${OPENAI_API_KEY}") String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY mangler (sett i .env ved siden av pom.xml)");
        }
        this.client = OpenAIOkHttpClient.builder().apiKey(apiKey).build();
    }

    private String loadSystemPrompt() {
        try {
            var res = new ClassPathResource("prompts/system.txt");
            return new String(res.getContentAsByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) { return ""; }
    }

    /** Hent (tekst)innhold ut av Responses API-strukturen. */
    private String extractText(Response resp) {
        String txt = resp.output().stream()
                .flatMap(item -> item.message().stream())
                .flatMap(msg -> msg.content().stream())
                .flatMap(content -> content.outputText().stream())
                .map(t -> t.text())
                .collect(Collectors.joining());
        return txt == null ? "" : txt.trim();
    }

    /** Enkel sjekk om streng ser ut som JSON-objekt/-array. */
    private boolean isLikelyJson(String s) {
        if (s == null) return false;
        String t = s.trim();
        return (t.startsWith("{") && t.endsWith("}")) || (t.startsWith("[") && t.endsWith("]"));
    }

    /** Hvis modellen har skrevet prose rundt, plukk ut første JSON-objekt/array. */
    private String tryExtractFirstJsonBlock(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (isLikelyJson(t)) return t;

        // veldig enkel heuristikk: finn { ... } eller [ ... ] (balansering er hardt; vi tar 'best effort')
        Pattern p = Pattern.compile("(\\{.*\\})|(\\[.*\\])", Pattern.DOTALL);
        Matcher m = p.matcher(t);
        if (m.find()) {
            return m.group();
        }
        return "";
    }

    /** Et ekstra kall som tvinger JSON-only. */
    private String forceJson(String prompt) {
        ResponseCreateParams params = ResponseCreateParams.builder()
                .model(resolveChatModel(model))
                .input(
                        "SVAR KUN med STRICT gyldig JSON som matcher schema. " +
                                "Ingen forklaringer, ingen markdown, ingen tekst før/etter. " +
                                "Start med '{' og slutt med '}'.\n\n" + prompt
                )
                .maxOutputTokens(2000)
                .build();
        Response resp = client.responses().create(params);
        return extractText(resp);
    }

    public QuizDTO generateQuiz(GenerateQuizRequest req) {
        String system = loadSystemPrompt();

        // Kompakt utgave av artiklene
        List<JsonNode> compact = new ArrayList<>();
        for (Object a : req.articles) {
            JsonNode n = mapper.valueToTree(a);
            var node = mapper.createObjectNode();
            if (n.has("id")) node.set("id", n.get("id"));
            if (n.has("urls")) node.set("urls", n.get("urls"));
            if (n.has("title")) node.set("title", n.get("title"));
            if (n.has("intro")) node.set("intro", n.get("intro"));
            if (n.has("components")) node.set("components", n.get("components"));
            if (n.has("tags")) node.set("tags", n.get("tags"));
            compact.add(node);
        }

        var userInput = mapper.createObjectNode();
        userInput.put("instructionLocale", req.locale == null ? "nb-NO" : req.locale);
        userInput.put("numQuestions", req.numQuestions == null ? 8 : req.numQuestions);
        userInput.put("difficulty", req.difficulty == null ? "mixed" : req.difficulty);
        userInput.set("articles", mapper.valueToTree(compact));

        String prompt = system + "\n---\nINNSTILLINGER OG DATA:\n" + JsonUtils.pretty(userInput);

        String text = "";
        try {
            // 1) Primærkall
            ResponseCreateParams params = ResponseCreateParams.builder()
                    .model(resolveChatModel(model))
                    .input(prompt)
                    .maxOutputTokens(3500)
                    .build();

            Response resp = client.responses().create(params);
            text = extractText(resp);

            if (text.isBlank()) {
                // 2) Hvis tomt, forsøk en tvungen JSON-respons
                text = forceJson(prompt);
            }

            // 3) Prøv parsing direkte
            String toParse = isLikelyJson(text) ? text : tryExtractFirstJsonBlock(text);
            if (toParse.isBlank()) throw new IOException("Tomt/ugyldig JSON-svar fra modellen.");

            QuizDTO quiz = mapper.readValue(toParse, QuizDTO.class);
            if (quiz.meta == null) quiz.meta = new QuizModels.Meta();
            quiz.meta.createdAt = Instant.now();
            return quiz;

        } catch (Exception parseOrApiError) {
            // 4) Siste sjanse: ett reparasjonskall til
            try {
                String repairedText = forceJson(prompt);
                String toParse = isLikelyJson(repairedText) ? repairedText : tryExtractFirstJsonBlock(repairedText);
                if (!toParse.isBlank()) {
                    QuizDTO quiz = mapper.readValue(toParse, QuizDTO.class);
                    if (quiz.meta == null) quiz.meta = new QuizModels.Meta();
                    quiz.meta.createdAt = Instant.now();
                    return quiz;
                }
            } catch (Exception ignored) {
                // fallthrough til fallback/feil
            }

            if (fallbackOnParseError) {
                // 5) Fallback: returner en minimal mock-quiz slik at frontend ikke dør
                return buildFallbackQuiz(req);
            }

            throw new RuntimeException("Klarte ikke å parse modellens JSON-svar (og reparasjon feilet).", parseOrApiError);
        }
    }

    private QuizDTO buildFallbackQuiz(GenerateQuizRequest req) {
        QuizDTO quiz = new QuizDTO();
        quiz.title = "Nyhetsquiz (fallback)";
        quiz.locale = (req.locale == null ? "nb-NO" : req.locale);
        quiz.meta = new QuizModels.Meta();
        quiz.meta.createdAt = Instant.now();
        quiz.questions = new ArrayList<>();

        var q = new QuizModels.Question();
        q.id = "fallback-spm-1";
        q.sourceId = req.articles != null && !req.articles.isEmpty() ? "opplasta-artikler" : "ingen-artikler";
        q.sourceTitle = "Generert lokalt";
        q.difficulty = "easy";
        q.prompt = "Denne quizen ble generert som fallback. Hvilken modus er aktiv?";
        q.options = List.of("Fallback", "Produksjon");
        q.answerIndex = 0;
        q.explanation = "Modellen returnerte tomt/ugyldig JSON. Fallback er aktivert for demo.";
        q.tags = List.of("dev", "fallback");

        quiz.questions.add(q);
        return quiz;
    }

    private ChatModel resolveChatModel(String m) {
        String key = (m == null ? "gpt-5-mini" : m).toLowerCase();
        return switch (key) {
            case "gpt-5" -> ChatModel.GPT_5;
            case "gpt-5-mini" -> ChatModel.GPT_5_MINI;
            case "gpt-5-nano" -> ChatModel.GPT_5_NANO;
            default -> ChatModel.GPT_5_MINI;
        };
    }
}
