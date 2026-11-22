package com.ravex.civilization;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Config file: config/ravex_civ-common.toml. API keys are NEVER stored here. */
public final class RavexConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue AI_ENABLED;
    public static final ModConfigSpec.ConfigValue<String> PROVIDER;
    public static final ModConfigSpec.ConfigValue<String> MODEL;
    public static final ModConfigSpec.ConfigValue<String> BASE_URL;
    public static final ModConfigSpec.IntValue MAX_TOKENS;
    public static final ModConfigSpec.IntValue HISTORY_TURNS_SENT;
    public static final ModConfigSpec.IntValue MIN_SECONDS_BETWEEN_REQUESTS;
    public static final ModConfigSpec.IntValue TIMEOUT_SECONDS;
    public static final ModConfigSpec.IntValue UPDATE_INTERVAL_TICKS;
    public static final ModConfigSpec.BooleanValue STARTER_KIT;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("ai");
        AI_ENABLED = b.comment("Use the real LLM provider. If false (or no API key), only the basic offline command parser runs.")
                .define("enabled", true);
        PROVIDER = b.comment("Provider id. Implemented: \"openai\". (\"anthropic\" and \"ollama\" are planned, not implemented.)")
                .define("provider", "openai");
        MODEL = b.comment("Model name sent to the provider.").define("model", "gpt-4o-mini");
        BASE_URL = b.comment("OpenAI-compatible base URL (no trailing slash).").define("baseUrl", "https://api.openai.com/v1");
        MAX_TOKENS = b.comment("Max tokens per reply (cost control).").defineInRange("maxTokens", 250, 50, 2000);
        HISTORY_TURNS_SENT = b.comment("How many recent chat turns are sent with each request (cost control).")
                .defineInRange("historyTurnsSent", 10, 0, 40);
        MIN_SECONDS_BETWEEN_REQUESTS = b.comment("Minimum seconds between AI requests for one NPC.")
                .defineInRange("minSecondsBetweenRequests", 2, 0, 60);
        TIMEOUT_SECONDS = b.defineInRange("timeoutSeconds", 30, 5, 120);
        b.pop();
        b.push("performance");
        UPDATE_INTERVAL_TICKS = b.comment("How often (ticks) NPCs re-evaluate equipment. 20 ticks = 1 second.")
                .defineInRange("updateIntervalTicks", 40, 10, 400);
        b.pop();
        b.push("gameplay");
        STARTER_KIT = b.comment("Give newly spawned NPCs a small starter kit (bread, basic tools). Set false for a pure from-scratch start.")
                .define("starterKit", true);
        b.pop();
        SPEC = b.build();
    }

    private RavexConfig() {}
}
