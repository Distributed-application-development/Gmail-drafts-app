package org.psu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class GmailApiService {
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String API_BASE_URL = "https://www.googleapis.com/gmail/v1/users/me/";

    // READ (List)
    public List<Draft> getDrafts(String accessToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE_URL + "drafts"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode responseJson = objectMapper.readTree(response.body());
        List<Draft> drafts = new ArrayList<>();
        if (responseJson.has("drafts")) {
            for (JsonNode draftNode : responseJson.get("drafts")) {
                String draftId = draftNode.get("id").asText();
                drafts.add(new Draft().setId(draftId));
            }
        }
        return drafts;
    }

    // READ (Single)
    public Draft getDraftById(String draftId, String accessToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE_URL + "drafts/" + draftId + "?format=full"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode draftJson = objectMapper.readTree(response.body());

        String subject = "";
        JsonNode headers = draftJson.at("/message/payload/headers");
        for (JsonNode header : headers) {
            if ("Subject".equals(header.get("name").asText())) {
                subject = header.get("value").asText();
                break;
            }
        }
        return new Draft().setId(draftId).setSubject(subject);
    }

    // CREATE
    public void createDraft(String accessToken, Draft draft) throws Exception {
        String rawMessage = createRawMessage(draft);
        ObjectNode messageNode = objectMapper.createObjectNode();
        messageNode.put("raw", rawMessage);
        ObjectNode draftNode = objectMapper.createObjectNode();
        draftNode.set("message", messageNode);
        String body = objectMapper.writeValueAsString(draftNode);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE_URL + "drafts"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    // UPDATE
    public void updateDraft(String accessToken, Draft draft) throws Exception {
        String rawMessage = createRawMessage(draft);
        ObjectNode messageNode = objectMapper.createObjectNode();
        messageNode.put("raw", rawMessage);
        ObjectNode draftNode = objectMapper.createObjectNode();
        draftNode.set("message", messageNode);
        String body = objectMapper.writeValueAsString(draftNode);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE_URL + "drafts/" + draft.getId()))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();

        client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    // DELETE
    public void deleteDraft(String accessToken, String draftId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE_URL + "drafts/" + draftId))
                .header("Authorization", "Bearer " + accessToken)
                .DELETE()
                .build();
        client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String createRawMessage(Draft draft) throws Exception {
        MimeMessage email = Utils.createEmail("to@example.com", "me", draft.getSubject(), draft.getBody());
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        email.writeTo(buffer);
        return Base64.getUrlEncoder().encodeToString(buffer.toByteArray());
    }
}
