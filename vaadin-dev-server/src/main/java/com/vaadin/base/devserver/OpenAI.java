package com.vaadin.base.devserver;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

public class OpenAI {

    public static String ask(String message, String openAiApiKey)
            throws IOException, InterruptedException {

        getLogger().info("Querying OpenAI");
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10)).build();

        JsonObject jsonObject = Json.createObject();
        JsonObject messageObject = Json.createObject();
        messageObject.put("role", "user");
        messageObject.put("content", message);
        JsonArray messages = Json.createArray();
        messages.set(0, messageObject);
        jsonObject.put("messages", messages);
        jsonObject.put("model", "gpt-3.5-turbo");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Authorization", "Bearer " + openAiApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonObject.toJson()))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        JsonObject responseJson = Json.parse(response.body());
        if (responseJson.hasKey("error")) {
            throw new IllegalStateException(
                    responseJson.getObject("error").getString("message"));
        }
        getLogger().info("Got response");
        return responseJson.getArray("choices").getObject(0)
                .getObject("message").getString("content");
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(OpenAI.class);
    }
}
