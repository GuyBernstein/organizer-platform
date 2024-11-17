package com.organizer.platform.service.AI;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.organizer.platform.model.AI.Response;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.util.List;

@Service
public class AiService {
    @Value("${anthropic.api.key}")
    private String apiKey;

    private final ObjectMapper objectMapper;
    @Autowired
    public AiService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String generateCategoryFromText(String text) throws UnirestException, JsonProcessingException {
        Unirest.setTimeouts(0, 0);
        HttpResponse<String> response = Unirest.post("https://api.anthropic.com/v1/messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .body("{\n    \"model\": \"claude-3-5-haiku-20241022\",\n    \"max_tokens\": 8192,\n    " +
                        "\"temperature\": 0.1,\n    \"system\": \"You are a precise text classification system that " +
                        "assigns single category labels to texts. You must respond only with a 1-3 word category in" +
                        " Hebrew, enclosed in <category> tags. Do not include any explanation, analysis, or additional" +
                        " text in your response.\\nThe possible categories are:" +
                        "\\n\\nטכנולוגיה\\nפוליטיקה\\nבריאות\\nסביבה\\nחינוך\\nבידור\\nספורט\\nעסקים\\nמדע\\nתרבות\\nדת\\nאוכל\\nאמנות\\nהיסטוריה\\nתיירות\\n\\n" +
                        "If none of these categories adequately describes the main topic of the text," +
                        " create a new 1-3 word category in Hebrew that better describes it." +
                        " The chosen category must be broad enough to group similar texts, but specific enough to be meaningful." +
                        "\",\n    \"messages\": [\n      {\n        \"role\": \"user\",\n        " +
                        "\"content\": [\n          {\n            \"type\": \"text\",\n            " +
                        "\"text\": \"Classify the following text into the most appropriate category:" +
                        "\\n<input_text>\\n" + convertToJavaString(text) + "\\n</input_text>\\n" +
                        "[The system will then respond with just the category in the format:]" +
                        "\\n<category>קטגוריה</category>\"\n          }\n        ]\n      }\n    ]\n  }")
                .asString();


        return toResult(response);
    }

    public String generateCategoryFromImage(String base64Image) throws UnirestException, JsonProcessingException {
        Unirest.setTimeouts(0, 0);
        HttpResponse<String> response = Unirest.post("https://api.anthropic.com/v1/messages")
                .header("Content-Type", "application/json")
                .header("anthropic-version", "2023-06-01")
                .header("x-api-key", apiKey)
                .body("{\n    \"model\": \"claude-3-5-sonnet-20241022\",\n    \"max_tokens\": 8192,\n    " +
                        "\"temperature\": 0.1,\n    \"system\": " +
                        "\"You are a precise image classification AI that focuses solely on visual content." +
                        " Your purpose is to match images to the most specific relevant category available." +
                        "\\n\\nRules:\\n- Choose the most specific matching category" +
                        "\\n- Create a new category only if no existing ones fit" +
                        "\\n- Consider ONLY the visual content, not implied context" +
                        "\\n- Never add explanations to your classifications\\n" +
                        "\\nProcess:\\n1. Identify key visual elements\\n2. Match against available categories" +
                        "\\n3. Select best fit OR create specific new category\\n" +
                        "\\nAlways output your classification in this format:\\n<category>קטגוריה</category>" +
                        "\",\n    \"messages\": [\n        {\n            \"role\": \"user\",\n            \"content\":" +
                        " [\n                {\n                    \"type\": \"text\",\n                    \"text\": " +
                        "\"Analyze and classify the image using these categories:" +
                        "\\n<categories>\\nטכנולוגיה\\nפוליטיקה\\nבריאות\\nסביבה\\nחינוך\\nבידור\\nספורט\\nעסקים\\nמדע\\nתרבות\\nדת\\nאוכל\\nאמנות\\nהיסטוריה\\nתיירות\\nבינה מלאכותית\\n</categories>\"" +
                        "\n                },\n                {\n                    \"type\": \"image\"," +
                        "\n                    \"source\": {\n                        " +
                        "\"type\": \"base64\",\n                        \"media_type\": \"image/jpeg\"," +
                        "\n                        \"data\": \"" + base64Image + "\"\n                    }" +
                        "\n                }\n            ]\n        }\n    ]\n}"
                )
                .asString();


        return toResult(response);
    }

    private String toResult(HttpResponse<String> response) throws JsonProcessingException {
        Response res = objectMapper.readValue(response.getBody(), Response.class);
        if (res.getContent().isEmpty()) {
            return null;
        }
        String category = res.getContent().get(0).getText();
        String result = category.replaceAll("<category>(.*?)</category>", "$1");

        // Check if the AI did put the <category> xml tags
        if(result.isEmpty())
            return category;
        return result;
    }

    private static String convertToJavaString(String input) {
        if (input == null) {
            return null;
        }

        return input
                .replace("\\", "\\\\") // Must be first to avoid double-escaping
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\"", "\\\"")
                .replace("\t", "\\t");
    }



}
