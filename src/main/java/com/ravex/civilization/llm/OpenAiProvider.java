package com.ravex.civilization.llm;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ravex.civilization.RavexConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/** OpenAI (or OpenAI-compatible) /chat/completions over java.net.http, fully asynchronous. */
public class OpenAiProvider implements LlmProvider {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    @Override
    public String id() {
        return "openai";
    }

    @Override
    public boolean isConfigured() {
        return !ApiKeys.openAi().isEmpty();
    }

    @Override
    public CompletableFuture<String> chat(String systemPrompt, List<ChatTurn> turns) {
        JsonObject body = new JsonObject();
        body.addProperty("model", RavexConfig.MODEL.get());
        body.addProperty("max_tokens", RavexConfig.MAX_TOKENS.get());
        body.addProperty("temperature", 0.7);
        JsonObject fmt = new JsonObject();
        fmt.addProperty("type", "json_object");
        body.add("response_format", fmt);

        JsonArray msgs = new JsonArray();
        msgs.add(msg("system", systemPrompt));
        for (ChatTurn t : turns) msgs.add(msg(t.role(), t.content()));
        body.add("messages", msgs);

        HttpRequest req = HttpRequest.newBuilder(URI.create(RavexConfig.BASE_URL.get() + "/chat/completions"))
                .timeout(Duration.ofSeconds(RavexConfig.TIMEOUT_SECONDS.get()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + ApiKeys.openAi())
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        return client.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenApply(resp -> {
            if (resp.statusCode() != 200) {
                String b = resp.body();
                throw new CompletionException(new IOException("HTTP " + resp.statusCode()
                        + (b != null && b.length() > 160 ? " " + b.substring(0, 160) : "")));
            }
            JsonObject root = JsonParser.parseString(resp.body()).getAsJsonObject();
            return root.getAsJsonArray("choices").get(0).getAsJsonObject()
                    .getAsJsonObject("message").get("content").getAsString();
        });
    }

    private static JsonObject msg(String role, String content) {
        JsonObject o = new JsonObject();
        o.addProperty("role", role);
        o.addProperty("content", content);
        return o;
    }
}
