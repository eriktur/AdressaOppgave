package com.eriktn.adressacase.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


// Enkle JSON-hjelpefunksjoner
public class JsonUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String pretty(Object o) {
        try { return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(o); }
        catch (JsonProcessingException e) { return String.valueOf(o); }
    }

    public static JsonNode toNode(Object o) {
        return MAPPER.valueToTree(o);
    }
}
