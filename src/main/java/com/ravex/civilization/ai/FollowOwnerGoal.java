package com.ravex.civilization.ai;

import com.ravex.civilization.npc.NpcEntity;
import com.ravex.civilization.npc.NpcMission;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** Walks (never teleports) toward the player while the mission is FOLLOW or PROTECT. */
public class FollowOwnerGoal extends Goal {
    private final NpcEntity npc;
    private ServerPlayer owner;
    private int repath;

    public FollowOwnerGoal(NpcEntity npc) {
        this.npc = npc;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean active() {
        return npc.mission() == NpcMission.FOLLOW || npc.mission() == NpcMission.PROTECT;
    }

    @Override
    public boolean canUse() {
        if (!active() || npc.getTarget() != null) return false;
        owner = npc.owner();
        return owner != null && owner.level() == npc.level() && npc.distanceToSqr(owner) > 16.0;
    }

    @Override
    public boolean canContinueToUse() {
        return active() && npc.getTarget() == null && owner != null && owner.isAlive()
                && owner.level() == npc.level() && npc.distanceToSqr(owner) > 6.25;
    }

    @Override
    public void start() {
        repath = 0;
    }

    @Override
    public void stop() {
        npc.getNavigation().stop();
        owner = null;
    }

    @Override
    public void tick() {
        npc.getLookControl().setLookAt(owner, 10.0F, npc.getMaxHeadXRot());
        if (--repath <= 0) {
            repath = 10;
            double speed = npc.distanceToSqr(owner) > 144.0 ? 1.3 : 1.0;
            npc.getNavigation().moveTo(owner, speed);
        }
    }
}
