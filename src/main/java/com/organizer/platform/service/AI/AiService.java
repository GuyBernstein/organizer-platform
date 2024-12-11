package com.organizer.platform.service.AI;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.organizer.platform.model.AI.Response;
import com.organizer.platform.model.organizedDTO.WhatsAppMessage;
import com.organizer.platform.service.WhatsApp.WhatsAppMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiService {
    @Value("${anthropic.api.key}")
    private String apiKey;

    private final ObjectMapper objectMapper;
    private final WhatsAppMessageService whatsAppMessageService;
    @Autowired
    public AiService(ObjectMapper objectMapper, WhatsAppMessageService whatsAppMessageService) {
        this.objectMapper = objectMapper;
        this.whatsAppMessageService = whatsAppMessageService;
    }

    public void generateOrganizationFromText(WhatsAppMessage whatsAppMessage) throws UnirestException, JsonProcessingException {
        Unirest.setTimeouts(0, 0);
        HttpResponse<String> response = Unirest.post("https://api.anthropic.com/v1/messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .body("{\n    \"model\": \"claude-3-5-haiku-20241022\",\n    \"max_tokens\": 8192,\n    " +
                        "\"temperature\": 0,\n    \"system\": \"You are a precise text organization system that helps users find their content easily." +
                        " Please organize and classify the content in Hebrew" +
                        "\\n\\nOutput following this example format:\\n<content_organization_schema>" +
                        "\\n    <!-- הגדרת הסיווג הראשי של התוכן -->\\n    <primary_classification>" +
                        "\\n        <category>\\n            <!-- הקטגוריה הראשית של התוכן (למשל: קריירה, חינוך, עסקים) -->" +
                        "\\n        </category>\\n        <subcategory>" +
                        "\\n            <!-- תת-קטגוריה ספציפית יותר (למשל: מכתב פנייה, קורות חיים, הצעת עבודה) -->" +
                        "\\n        </subcategory>\\n    </primary_classification>\\n    \\n    <content_type>" +
                        "\\n        <type>\\n            <!-- סוג המסמך או התוכן (למשל: מכתב, מצגת, דו\\\"ח, מסמך) -->" +
                        "\\n        </type>\\n        <purpose>" +
                        "\\n            <!-- המטרה העיקרית של התוכן (למשל: עסקי, אישי, לימודי) -->\\n        </purpose>" +
                        "\\n    </content_type>\\n    \\n    <user_metadata>\\n        <tags>" +
                        "\\n            <!-- תגיות מפתח חדשות, מופרדות על ידי פסיקים, המתארות את התוכן בהתבסס על ניתוח מעמיק של הטקסט -->" +
                        "\\n        </tags>\\n        <next_steps>\\n            <!-- פעולות המשך נדרשות, מופרדות על ידי פסיקים -->" +
                        "\\n        </next_steps>\\n    </user_metadata>\\n</content_organization_schema> " +
                        "\\n\\nDo not add any information beyond the requested XML schema.\",\n    " +
                        "\"messages\": [\n      {\n        \"role\": \"user\",\n        \"content\": [\n          " +
                        "{\n            \"type\": \"text\",\n            \"text\": \"Analyze and organize this content:" +
                        "\\n\\n<input_text>\\n" + convertToJavaString(whatsAppMessage.getMessageContent()) + " \\n</input_text>" +
                        "\"\n          }\n        ]\n      }\n    ]\n  }")
                .asString();
        if(response.getBody() == null)
            throw new NullPointerException("Returned null from AI during text organization");

        toWhatsappMessage(response, whatsAppMessage);
    }

    public void generateOrganizationFromImage(String base64Image, WhatsAppMessage whatsAppMessage) throws UnirestException, JsonProcessingException {
        Unirest.setTimeouts(0, 0);
        HttpResponse<String> response = Unirest.post("https://api.anthropic.com/v1/messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .body("{\n    \"model\": \"claude-3-5-sonnet-20241022\",\n    \"max_tokens\": 8192,\n    " +
                        "\"temperature\": 0.1,\n    \"system\": " +
                        "\"You are a precise image organization system that helps users find their visual content easily." +
                        "  Please organize and classify the content in Hebrew" +
                        "\\n\\nOutput following this schema format:\\n<content_organization_schema>" +
                        "\\n    <!-- הגדרת הסיווג הראשי של התוכן -->\\n    <primary_classification>\\n        <category>" +
                        "\\n            <!-- הקטגוריה הראשית של התמונה (למשל: טבע, אומנות, אירועים) -->" +
                        "\\n        </category>\\n        <subcategory>" +
                        "\\n            <!-- תת-קטגוריה ספציפית יותר (למשל: נוף, פורטרט, חתונה) -->" +
                        "\\n        </subcategory>\\n    </primary_classification>\\n\\n    <content_type>" +
                        "\\n        <type>\\n            <!-- סוג התמונה (למשל: צילום, איור, גרפיקה) -->\\n        </type>" +
                        "\\n        <purpose>\\n            <!-- המטרה העיקרית של התמונה (למשל: מסחרי, אישי, חינוכי) -->" +
                        "\\n        </purpose>\\n    </content_type>\\n\\n    <user_metadata>\\n        <tags>" +
                        "\\n            <!-- תגיות מפתח חדשות, מופרדות על ידי פסיקים, המתארות את התמונה -->\\n        </tags>" +
                        "\\n        <next_steps>\\n            <!-- פעולות המשך נדרשות, מופרדות על ידי פסיקים -->" +
                        "\\n        </next_steps>\\n    </user_metadata>\\n</content_organization_schema>" +
                        "\\n\\nDo not add any information beyond the requested XML schema.\"," +
                        "\n    \"messages\": [\n      {\n        \"role\": \"user\",\n        \"content\": [" +
                        "\n          {\n            \"type\": \"text\",\n            \"text\": " +
                        "\"Analyze and organize this image content." +
                        "\"\n          },\n          {\n            \"type\": \"image\",\n            \"source\": {\n              " +
                        "\"type\": \"base64\",\n              \"media_type\": \"image/jpeg\",\n              " +
                        "\"data\": \"" + base64Image + "\"\n            }\n          }\n        ]\n      }\n    ]\n  }")
                .asString();

        if(response.getBody() == null)
            throw new NullPointerException("Returned null from AI during image organization");
        toWhatsappMessage(response, whatsAppMessage);
    }
    public void generateOrganizationFromPDF(String base64PDF, WhatsAppMessage whatsAppMessage) throws UnirestException, JsonProcessingException {
        Unirest.setTimeouts(0, 0);
        HttpResponse<String> response = Unirest.post("https://api.anthropic.com/v1/messages")
                .header("content-type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("anthropic-beta", "pdfs-2024-09-25")
                .body("{\n  \"model\": \"claude-3-5-sonnet-20241022\",\n  \"max_tokens\": 8192,\n  \"temperature\": 0,\n  \"system\": \"you are a precise PDF document " +
                        "organization system that helps users find their document content easily." +
                        " Please organize and classify the content in Hebrew.\\n\\nOutput following this schema format:" +
                        "\\n<content_organization_schema>\\n    <!-- הגדרת הסיווג הראשי של התוכן -->\\n    " +
                        "<primary_classification>\\n        <category>" +
                        "\\n            <!-- הקטגוריה הראשית של המסמך (למשל: משפטי, פיננסי, טכני) -->\\n        </category>" +
                        "\\n        <subcategory>\\n            <!-- תת-קטגוריה ספציפית יותר (למשל: חוזה, חשבונית, מדריך) -->" +
                        "\\n        </subcategory>\\n    </primary_classification>\\n\\n    <content_type>\\n        <type>" +
                        "\\n            <!-- סוג המסמך (למשל: דוח, טופס, מצגת) -->\\n        </type>\\n        <purpose>" +
                        "\\n            <!-- המטרה העיקרית של המסמך (למשל: פנימי, לקוח, תיעוד) -->\\n        </purpose>" +
                        "\\n    </content_type>\\n\\n    <user_metadata>\\n        <tags>" +
                        "\\n            <!-- תגיות מפתח חדשות, מופרדות על ידי פסיקים, המתארות את המסמך -->\\n        </tags>" +
                        "\\n        <next_steps>\\n            <!-- פעולות המשך נדרשות, מופרדות על ידי פסיקים -->" +
                        "\\n        </next_steps>\\n    </user_metadata>\\n</content_organization_schema>" +
                        "\\n\\nDo not add any information beyond the requested XML schema.\",\n  \"messages\":" +
                        " [\n    {\n      \"role\": \"user\",\n      \"content\": [\n        {\n          \"type\": " +
                        "\"document\",\n          \"source\": {\n            \"type\": \"base64\",\n            " +
                        "\"media_type\": \"application/pdf\",\n            \"data\": \"" + base64PDF +
                        "\"\n          }\n        },\n        {\n          \"type\": \"text\",\n          " +
                        "\"text\": \"Analyze and organize this PDF content:\"\n        }\n      ]\n    }\n  ]\n}")
                .asString();


        if(response.getBody() == null)
            throw new NullPointerException("Returned null from AI during image organization");
        toWhatsappMessage(response, whatsAppMessage);
    }

    public void generateOrganizationFromURL(WhatsAppMessage whatsAppMessage) throws UnirestException, JsonProcessingException {
        Unirest.setTimeouts(0, 0);
        HttpResponse<String> response = Unirest.post("https://api.anthropic.com/v1/messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .body("{\n    \"model\": \"claude-3-5-haiku-20241022\",\n    \"max_tokens\": 8192,\n    " +
                        "\"temperature\": 0,\n    \"system\": \"You are an advanced content organization system" +
                        " designed to classify and structure Hebrew web content. Your task is to analyze both the" +
                        " URL and the scraped content of a webpage, then organize this information into a structured" +
                        " XML format for easy retrieval and management.\\n\\nOutput following this example format:" +
                        "\\n<content_organization_schema>\\n    <!-- הגדרת הסיווג הראשי של התוכן -->\\n    " +
                        "<primary_classification>\\n        <category>" +
                        "\\n            <!-- הקטגוריה הראשית של התוכן (למשל: קריירה, חינוך, עסקים) -->\\n        " +
                        "</category>\\n        <subcategory>" +
                        "\\n            <!-- תת-קטגוריה ספציפית יותר (למשל: מכתב פנייה, קורות חיים, הצעת עבודה) -->" +
                        "\\n        </subcategory>\\n    </primary_classification>\\n    \\n    <content_type>" +
                        "\\n        <type>\\n            <!-- סוג המסמך או התוכן (למשל: מכתב, מצגת, דו\\\"ח, מסמך) -->" +
                        "\\n        </type>\\n        <purpose>" +
                        "\\n            <!-- המטרה העיקרית של התוכן (למשל: עסקי, אישי, לימודי) -->\\n        </purpose>" +
                        "\\n    </content_type>\\n    \\n    <user_metadata>\\n        <tags>" +
                        "\\n            <!-- תגיות מפתח חדשות, מופרדות בפסיקים, המתארות את התוכן בהתבסס על ניתוח מעמיק של הטקסט -->" +
                        "\\n        </tags>\\n        <next_steps>\\n            <!-- פעולות המשך נדרשות, המופרדות בפסיקים -->" +
                        "\\n        </next_steps>\\n    </user_metadata>\\n</content_organization_schema>" +
                        "\\n\\nDo not add any information beyond what is requested in the XML schema." +
                        "\",\n    \"messages\": [\n      {\n        \"role\": \"user\",\n        \"content\": [" +
                        "\n          {\n            \"type\": \"text\",\n            \"text\": " +
                        "\"Carefully analyze both the input <input> and scraped content <scraped_content> below " +
                        "to classify and organize the information.\\n\\n<input>\\n" + convertToJavaString(whatsAppMessage.getMessageContent()) +
                        "\\n</input>\\n\\n<scraped_content>\\n" + convertToJavaString(whatsAppMessage.getPurpose()) +
                        "\\n</scraped_content>\"\n          }\n        ]\n      }\n    ]\n  }")
                .asString();

        if(response.getBody() == null)
            throw new NullPointerException("Returned null from AI during URL organization");
        toWhatsappMessage(response, whatsAppMessage);
    }





    private void toWhatsappMessage(HttpResponse<String> response, WhatsAppMessage whatsAppMessage) throws JsonProcessingException {
        try {
            Response res = objectMapper.readValue(response.getBody(), Response.class);

            if (res.getContent().isEmpty())
                return;
            String content = res.getContent().get(0).getText();

            // Extract all fields into the message entity
            whatsAppMessage.setCategory(extractFromXMLContent(content, "category"));
            whatsAppMessage.setSubCategory(extractFromXMLContent(content, "subcategory"));
            whatsAppMessage.setType(extractFromXMLContent(content, "type"));
            whatsAppMessage.setPurpose(extractFromXMLContent(content, "purpose"));

            // Add tags and next steps if we have any
            addTagsAndNextSteps(whatsAppMessage, content);
        } catch (NullPointerException e){
            System.out.println("Error is: " + e.getMessage());
        }
    }

    private void addTagsAndNextSteps(WhatsAppMessage whatsAppMessage, String content) {
        String tagsContent = extractFromXMLContent(content, "tags");
        String nextStepsContent = (extractFromXMLContent(content, "next_steps"));
        assert tagsContent != null;
        assert nextStepsContent != null;
        whatsAppMessageService.addTagsAndNextSteps(whatsAppMessage, tagsContent, nextStepsContent);
    }

    private String extractFromXMLContent(String content, String xmlName) {
        Pattern pattern = Pattern.compile("<" + xmlName + ">(.*?)</" + xmlName + ">", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);

        return matcher.find() ? matcher.group(1).trim() : null;
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
