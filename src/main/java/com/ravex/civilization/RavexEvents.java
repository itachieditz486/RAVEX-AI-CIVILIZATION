package com.ravex.civilization;

import com.ravex.civilization.command.NpcCommand;
import com.ravex.civilization.npc.NpcBrain;
import com.ravex.civilization.npc.NpcEntity;
import com.ravex.civilization.npc.NpcNames;
import com.ravex.civilization.npc.NpcWorldData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Game-bus event wiring: chat detection, command registration, Yassine duplicate guard, memory restore. */
public final class RavexEvents {
    public static void register() {
        NeoForge.EVENT_BUS.addListener(RavexEvents::onChat);
        NeoForge.EVENT_BUS.addListener(RavexEvents::onCommands);
        NeoForge.EVENT_BUS.addListener(RavexEvents::onJoin);
        NeoForge.EVENT_BUS.addListener(RavexEvents::onServerStarted);
    }

    public static List<NpcEntity> allNpcs(MinecraftServer server) {
        List<NpcEntity> out = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            out.addAll(level.getEntities(EntityTypeTest.forClass(NpcEntity.class), e -> e.isAlive()));
        }
        return out;
    }

    private static void onChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        String text = event.getRawText();
        if (text == null || text.isBlank()) return;
        for (NpcEntity npc : allNpcs(player.server)) {
            if (NpcNames.mentions(npc.npcName(), text)) {
                NpcBrain.handle(npc, player, text);
            }
        }
    }

    private static void onCommands(RegisterCommandsEvent event) {
        NpcCommand.register(event.getDispatcher());
    }

    private static void onServerStarted(ServerStartedEvent event) {
        NpcWorldData.get(event.getServer()).restoreYassineBackupIfNeeded(event.getServer());
    }

    /** Prevents a second Yassine from ever entering the world (e.g. copied chunks, creative duplication). */
    private static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof NpcEntity npc)) return;
        if (!npc.isYassine() || !(event.getLevel() instanceof ServerLevel level)) return;
        NpcWorldData data = NpcWorldData.get(level.getServer());
        UUID registered = data.yassineEntity();
        if (registered == null) {
            data.setYassineEntity(npc.getUUID());
        } else if (!registered.equals(npc.getUUID())) {
            for (ServerLevel l : level.getServer().getAllLevels()) {
                if (l.getEntity(registered) != null) {
                    event.setCanceled(true);
                    RavexCiv.LOGGER.warn("Blocked a duplicate Yassine entity.");
                    return;
                }
            }
        }
    }

    private RavexEvents() {}
}
