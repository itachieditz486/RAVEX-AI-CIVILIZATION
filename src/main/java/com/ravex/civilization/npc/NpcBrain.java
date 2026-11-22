package com.ravex.civilization.npc;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ravex.civilization.RavexCiv;
import com.ravex.civilization.RavexConfig;
import com.ravex.civilization.llm.ChatTurn;
import com.ravex.civilization.llm.LlmProvider;
import com.ravex.civilization.llm.LlmProviders;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionException;

/** Routes a chat message to the real LLM (async) or to the offline parser, then applies the resulting action. */
public final class NpcBrain {
    private static final Set<String> ACTIONS = Set.of("none", "follow", "stop", "collect_wood", "protect", "unsupported");

    public static void handle(NpcEntity npc, ServerPlayer player, String text) {
        MinecraftServer server = player.server;
        NpcWorldData data = NpcWorldData.get(server);
        NpcMemory mem = data.memory(npc.npcKey());
        npc.setLang(Lang.detect(text, npc.lang()));
        mem.addTurn("user", player.getGameProfile().getName() + ": " + text);
        data.setDirty();

        LlmProvider provider = RavexConfig.AI_ENABLED.get() ? LlmProviders.current() : null;
        if (provider == null || !provider.isConfigured()) {
            runOffline(npc, player, text, data, "");
            return;
        }

        long now = server.getTickCount();
        long minTicks = RavexConfig.MIN_SECONDS_BETWEEN_REQUESTS.get() * 20L;
        if (npc.isThinking() || now - npc.lastAiTick() < minTicks) {
            npc.say(Lang.t(npc.lang(), "(one moment, I'm still thinking...)", "(un instant, je réfléchis encore...)",
                    "(لحظة، ما زلت أفكر...)"));
            return;
        }
        npc.setThinking(true);
        npc.setLastAiTick(now);

        String system = buildSystemPrompt(npc, player, data);
        List<ChatTurn> turns = new ArrayList<>();
        List<NpcMemory.Turn> hist = mem.history();
        int from = Math.max(0, hist.size() - RavexConfig.HISTORY_TURNS_SENT.get());
        for (int i = from; i < hist.size(); i++) turns.add(new ChatTurn(hist.get(i).role(), hist.get(i).text()));

        provider.chat(system, turns).whenComplete((raw, err) -> server.execute(() -> {
            npc.setThinking(false);
            if (npc.isRemoved()) return;
            if (err != null) {
                Throwable cause = err instanceof CompletionException && err.getCause() != null ? err.getCause() : err;
                RavexCiv.LOGGER.warn("AI request failed for {}: {}", npc.npcName(), cause.toString());
                runOffline(npc, player, text, data, "(AI unavailable) ");
                return;
            }
            applyAiReply(npc, player, raw, data);
        }));
    }

    private static void applyAiReply(NpcEntity npc, ServerPlayer player, String raw, NpcWorldData data) {
        String reply = raw;
        String action = "none";
        int count = 0;
        String remember = "";
        try {
            JsonObject o = JsonParser.parseString(raw).getAsJsonObject();
            if (o.has("reply")) reply = o.get("reply").getAsString();
            if (o.has("action") && o.get("action").isJsonObject()) {
                JsonObject a = o.getAsJsonObject("action");
                if (a.has("type")) action = a.get("type").getAsString().toLowerCase();
                if (a.has("count") && a.get("count").isJsonPrimitive()) count = a.get("count").getAsInt();
            }
            if (o.has("remember") && o.get("remember").isJsonPrimitive()) remember = o.get("remember").getAsString();
        } catch (Exception ex) {
            RavexCiv.LOGGER.warn("AI reply was not valid JSON; showing it as plain text.");
        }
        if (!ACTIONS.contains(action)) action = "none";
        NpcMemory mem = data.memory(npc.npcKey());
        mem.addTurn("assistant", reply);
        mem.addFact(remember);
        data.setDirty();
        if (npc.isYassine()) data.writeYassineBackup(player.server);
        npc.say(reply);
        if (!action.equals("none") && !action.equals("unsupported")) npc.applyAction(action, count, player);
    }

    private static void runOffline(NpcEntity npc, ServerPlayer player, String text, NpcWorldData data, String note) {
        OfflineInterpreter.Result r = OfflineInterpreter.interpret(text, npc.lang());
        String line = "[offline] " + note + r.reply();
        data.memory(npc.npcKey()).addTurn("assistant", line);
        data.setDirty();
        if (npc.isYassine()) data.writeYassineBackup(player.server);
        npc.say(line);
        if (!r.action().equals("none")) npc.applyAction(r.action(), r.count(), player);
    }

    private static String buildSystemPrompt(NpcEntity npc, ServerPlayer player, NpcWorldData data) {
        NpcProfile p = data.profile(npc.npcKey());
        String personality = p != null ? p.personality() : "A friendly survivor.";
        StringBuilder sb = new StringBuilder();
        sb.append("You are ").append(npc.npcName()).append(", an NPC living in a Minecraft Java 1.21.1 world, chatting with the player \"")
                .append(player.getGameProfile().getName()).append("\" in text chat.\n");
        sb.append("Personality: ").append(personality).append("\n");
        sb.append("Reply in the language the player used (Arabic, English or French), in character, at most 2 short sentences. ")
                .append("Never claim to have done something you have not actually done.\n");
        sb.append(String.format("Current state: health %.0f/%.0f, position %d %d %d, mission %s (progress %d/%d), inventory: %s.\n",
                npc.getHealth(), npc.getMaxHealth(), npc.getBlockX(), npc.getBlockY(), npc.getBlockZ(),
                npc.mission().name().toLowerCase(), npc.missionProgress(), npc.missionTarget(), npc.inventorySummary()));
        List<String> facts = data.memory(npc.npcKey()).facts();
        if (!facts.isEmpty()) {
            sb.append("Things you remember:\n");
            for (String f : facts) sb.append("- ").append(f).append("\n");
        }
        sb.append("Abilities that are ACTUALLY implemented right now (only these): follow (walk after the player), stop, ")
                .append("collect_wood (chop nearby trees, needs a count), protect (stay near the player and fight monsters). ")
                .append("Anything else (building, mining ore, crafting, farming, Nether, End, etc.) is NOT implemented yet: ")
                .append("use action type \"unsupported\" and honestly say you cannot do it yet.\n");
        sb.append("Respond ONLY with JSON: {\"reply\":\"...\",\"action\":{\"type\":\"none|follow|stop|collect_wood|protect|unsupported\",")
                .append("\"count\":0},\"remember\":\"optional short lasting fact worth remembering, else empty\"}");
        return sb.toString();
    }

    private NpcBrain() {}
}
