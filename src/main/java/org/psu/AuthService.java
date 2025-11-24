package org.psu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

import java.awt.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class AuthService {
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri = "http://localhost:8888/Callback";
    private final String scope = "https://www.googleapis.com/auth/gmail.compose";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final File tokensFile = new File("tokens.json");

    private String accessToken;
    private String refreshToken;

    public AuthService() throws IOException {
        InputStream in = AuthService.class.getResourceAsStream("/credentials.json");
        JsonNode secrets = objectMapper.readTree(in).get("web");
        this.clientId = secrets.get("client_id").asText();
        this.clientSecret = secrets.get("client_secret").asText();
    }

    public String getAccessToken() throws Exception {
        if (accessToken != null) {
            return accessToken;
        }

        if (tokensFile.exists()) {
            JsonNode tokens = objectMapper.readTree(tokensFile);
            this.refreshToken = tokens.get("refresh_token").asText();
            refreshAccessToken();
            return accessToken;
        }

        startAuthorizationFlow();
        return accessToken;
    }

    private void startAuthorizationFlow() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        HttpServer server = HttpServer.create(new InetSocketAddress(8888), 0);
        server.createContext("/Callback", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            String code = query.split("=")[1].split("&")[0];

            String response = "Received verification code. You may now close this window.";
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }

            exchangeCodeForTokens(code);
            latch.countDown();
        });
        server.setExecutor(null);
        server.start();

        String authUrl = "https://accounts.google.com/o/oauth2/auth?" +
                "client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8) +
                "&response_type=code" +
                "&access_type=offline";

        System.out.println("Please open the following URL in your browser:");
        System.out.println(authUrl);
        Desktop.getDesktop().browse(new URI(authUrl));

        latch.await();
        server.stop(0);
    }

    private void exchangeCodeForTokens(String code) {
        try {
            String body = "code=" + URLEncoder.encode(code, StandardCharsets.UTF_8) +
                    "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                    "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8) +
                    "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                    "&grant_type=authorization_code";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://oauth2.googleapis.com/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode tokens = objectMapper.readTree(response.body());

            this.accessToken = tokens.get("access_token").asText();
            this.refreshToken = tokens.get("refresh_token").asText();

            saveTokens();
        } catch (Exception e) {
            throw new RuntimeException("Could not exchange code for tokens", e);
        }
    }

    private void refreshAccessToken() throws Exception {
        String body = "client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8) +
                "&refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8) +
                "&grant_type=refresh_token";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://oauth2.googleapis.com/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode tokens = objectMapper.readTree(response.body());

        this.accessToken = tokens.get("access_token").asText();

        if (tokens.has("refresh_token")) {
            this.refreshToken = tokens.get("refresh_token").asText();
        }
        saveTokens();
    }

    private void saveTokens() throws IOException {
        Map<String, String> tokens = Map.of("refresh_token", refreshToken);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(tokensFile, tokens);
    }
}