package com.ravex.civilization.npc;

import com.ravex.civilization.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.UUID;

/** Custom "spawn egg": the Yassine egg always makes the one permanent Yassine; the random egg makes a new identity. */
public class NpcSpawnEggItem extends Item {
    private final boolean yassine;

    public NpcSpawnEggItem(boolean yassine, Properties props) {
        super(props);
        this.yassine = yassine;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (!(level instanceof ServerLevel sl)) return InteractionResult.SUCCESS;
        Player player = ctx.getPlayer();
        MinecraftServer server = sl.getServer();
        NpcWorldData data = NpcWorldData.get(server);
        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());

        NpcProfile profile;
        if (yassine) {
            UUID existing = data.yassineEntity();
            if (existing != null) {
                boolean loaded = false;
                for (ServerLevel l : server.getAllLevels()) {
                    Entity e = l.getEntity(existing);
                    if (e != null && e.isAlive()) { loaded = true; break; }
                }
                String msg = loaded
                        ? "Yassine already exists in this world - there can only be one."
                        : "Yassine already exists in this world (probably in an unloaded area). Use /npc release_yassine if he is truly lost.";
                if (player != null) player.displayClientMessage(Component.literal(msg), true);
                return InteractionResult.FAIL;
            }
            profile = data.yassineProfile();
        } else {
            profile = data.newRandomProfile(sl.getRandom());
        }

        NpcEntity npc = ModEntities.NPC.get().create(sl);
        if (npc == null) return InteractionResult.FAIL;
        npc.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, ctx.getRotation(), 0.0F);
        npc.initProfile(profile);
        if (yassine) {
            if (!data.yassineKitGiven()) {
                npc.giveStarterKit(sl.getRandom());
                data.setYassineKitGiven();
            }
            data.setYassineEntity(npc.getUUID());
        } else {
            npc.giveStarterKit(sl.getRandom());
        }
        sl.addFreshEntity(npc);
        if (player != null && !player.getAbilities().instabuild) ctx.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
}
