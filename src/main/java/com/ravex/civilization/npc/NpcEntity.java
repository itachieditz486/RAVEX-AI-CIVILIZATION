package com.ravex.civilization.npc;

import com.ravex.civilization.RavexConfig;
import com.ravex.civilization.ai.CollectWoodGoal;
import com.ravex.civilization.ai.FollowOwnerGoal;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.SimpleContainer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class NpcEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> DATA_SKIN =
            SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.STRING);

    private String npcKey = "";
    private NpcMission mission = NpcMission.IDLE;
    private int missionTarget;
    private int missionProgress;
    private UUID ownerId;
    private String lang = "en";

    private boolean thinking;
    private long lastAiTick = -100000L;

    public final SimpleContainer inventory = new SimpleContainer(27);

    public NpcEntity(EntityType<? extends NpcEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD,
                EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            this.setDropChance(s, 1.0F);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SKIN, "skin_1");
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15, true));
        this.goalSelector.addGoal(2, new CollectWoodGoal(this));
        this.goalSelector.addGoal(3, new FollowOwnerGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
                e -> !(e instanceof Creeper) && !(e instanceof EnderMan) && !(e instanceof Warden)));
    }

    @Override
    public boolean removeWhenFarAway(double distanceSqr) {
        return false;
    }

    // ---- identity -------------------------------------------------------------------------------------------------
    public void initProfile(NpcProfile p) {
        this.npcKey = p.key();
        this.entityData.set(DATA_SKIN, p.skin());
        this.setCustomName(Component.literal(p.name()));
        this.setCustomNameVisible(true);
    }

    public String npcKey() { return npcKey; }
    public String skin() { return this.entityData.get(DATA_SKIN); }
    public boolean isYassine() { return NpcNames.YASSINE_KEY.equals(npcKey); }

    public String npcName() {
        return getCustomName() != null ? getCustomName().getString() : "NPC";
    }

    public void giveStarterKit(RandomSource r) {
        if (!RavexConfig.STARTER_KIT.get()) return;
        inventory.addItem(new ItemStack(Items.BREAD, 3 + r.nextInt(4)));
        if (isYassine()) {
            inventory.addItem(new ItemStack(Items.STONE_AXE));
            inventory.addItem(new ItemStack(Items.STONE_SWORD));
        } else {
            if (r.nextBoolean()) inventory.addItem(new ItemStack(Items.STONE_AXE));
            if (r.nextInt(3) == 0) inventory.addItem(new ItemStack(Items.WOODEN_SWORD));
        }
    }

    // ---- mission state --------------------------------------------------------------------------------------------
    public NpcMission mission() { return mission; }
    public int missionTarget() { return missionTarget; }
    public int missionProgress() { return missionProgress; }
    public String lang() { return lang; }
    public void setLang(String l) { this.lang = l; }
    public boolean isThinking() { return thinking; }
    public void setThinking(boolean t) { this.thinking = t; }
    public long lastAiTick() { return lastAiTick; }
    public void setLastAiTick(long t) { this.lastAiTick = t; }

    public ServerPlayer owner() {
        if (ownerId == null || !(level() instanceof ServerLevel sl)) return null;
        return sl.getServer().getPlayerList().getPlayer(ownerId);
    }

    /** Applies a validated action. Returns false if the action type is unknown/unsupported. */
    public boolean applyAction(String type, int count, ServerPlayer player) {
        switch (type) {
            case "follow" -> {
                ownerId = player.getUUID();
                mission = NpcMission.FOLLOW;
                missionProgress = 0;
                return true;
            }
            case "protect" -> {
                ownerId = player.getUUID();
                mission = NpcMission.PROTECT;
                missionProgress = 0;
                return true;
            }
            case "collect_wood" -> {
                ownerId = player.getUUID();
                mission = NpcMission.COLLECT_WOOD;
                missionTarget = count > 0 ? Math.min(count, 512) : 8;
                missionProgress = 0;
                return true;
            }
            case "stop" -> {
                mission = NpcMission.IDLE;
                missionProgress = 0;
                getNavigation().stop();
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    public void onLogCollected() {
        missionProgress++;
        if (missionProgress >= missionTarget) {
            finishMission(String.format(Lang.t(lang, "Done! I collected %d logs.",
                    "Terminé ! J'ai récolté %d bûches.", "تم! جمعت %d قطعة خشب."), missionProgress));
        } else if (missionProgress % 8 == 0) {
            say(String.format(Lang.t(lang, "Progress: %d/%d logs.", "Progression : %d/%d bûches.",
                    "التقدم: %d من %d قطعة خشب."), missionProgress, missionTarget));
        }
    }

    public void finishMission(String message) {
        mission = NpcMission.IDLE;
        getNavigation().stop();
        say(message);
    }

    public void say(String message) {
        MinecraftServer server = level().getServer();
        if (server == null) return;
        Component c = Component.literal("<" + npcName() + "> ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(message).withStyle(ChatFormatting.WHITE));
        server.getPlayerList().broadcastSystemMessage(c, false);
    }

    public String inventorySummary() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack s = inventory.getItem(i);
            if (s.isEmpty()) continue;
            counts.merge(BuiltInRegistries.ITEM.getKey(s.getItem()).getPath(), s.getCount(), Integer::sum);
        }
        if (counts.isEmpty()) return "empty";
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (n++ >= 12) { sb.append(", ..."); break; }
            if (sb.length() > 0) sb.append(", ");
            sb.append(e.getKey()).append(" x").append(e.getValue());
        }
        return sb.toString();
    }

    // ---- equipment (items move between inventory and slots; nothing is duplicated) ---------------------------------
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (tickCount % RavexConfig.UPDATE_INTERVAL_TICKS.get() == 0) refreshEquipment();
    }

    private void refreshEquipment() {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack s = inventory.getItem(i);
            if (s.getItem() instanceof ArmorItem a) {
                EquipmentSlot slot = a.getType().getSlot();
                if (getItemBySlot(slot).isEmpty()) {
                    moveToSlot(i, slot);
                }
            }
        }
        boolean fighting = getTarget() != null;
        int best = -1;
        float bestScore = toolScore(getMainHandItem(), fighting);
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            float sc = toolScore(inventory.getItem(i), fighting);
            if (sc > bestScore) {
                bestScore = sc;
                best = i;
            }
        }
        if (best >= 0) moveToSlot(best, EquipmentSlot.MAINHAND);
    }

    private float toolScore(ItemStack s, boolean fighting) {
        if (s.isEmpty()) return 0;
        if (fighting && s.getItem() instanceof SwordItem sw) return 4 + sw.getTier().getAttackDamageBonus();
        if (fighting && s.getItem() instanceof AxeItem ax) return 3 + ax.getTier().getAttackDamageBonus();
        if (!fighting && mission == NpcMission.COLLECT_WOOD && s.getItem() instanceof AxeItem ax)
            return 1 + ax.getTier().getSpeed();
        return 0;
    }

    private void moveToSlot(int invIndex, EquipmentSlot slot) {
        ItemStack taken = inventory.removeItem(invIndex, 1);
        if (taken.isEmpty()) return;
        ItemStack old = getItemBySlot(slot);
        setItemSlot(slot, taken);
        if (!old.isEmpty()) {
            ItemStack left = inventory.addItem(old);
            if (!left.isEmpty()) spawnAtLocation(left);
        }
    }

    // ---- death / persistence --------------------------------------------------------------------------------------
    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide && isYassine() && level().getServer() != null) {
            NpcWorldData data = NpcWorldData.get(level().getServer());
            if (uuid.equals(data.yassineEntity())) data.setYassineEntity(null);
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack s = inventory.removeItemNoUpdate(i);
            if (!s.isEmpty()) spawnAtLocation(s);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("NpcKey", npcKey);
        tag.putString("NpcSkin", skin());
        tag.putString("NpcLang", lang);
        tag.putString("Mission", mission.name());
        tag.putInt("MissionTarget", missionTarget);
        tag.putInt("MissionProgress", missionProgress);
        if (ownerId != null) tag.putUUID("Owner", ownerId);
        tag.put("NpcInventory", inventory.createTag(registryAccess()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        npcKey = tag.getString("NpcKey");
        if (tag.contains("NpcSkin")) entityData.set(DATA_SKIN, tag.getString("NpcSkin"));
        if (tag.contains("NpcLang")) lang = tag.getString("NpcLang");
        try {
            mission = NpcMission.valueOf(tag.getString("Mission"));
        } catch (IllegalArgumentException ex) {
            mission = NpcMission.IDLE;
        }
        missionTarget = tag.getInt("MissionTarget");
        missionProgress = tag.getInt("MissionProgress");
        if (tag.hasUUID("Owner")) ownerId = tag.getUUID("Owner");
        if (tag.contains("NpcInventory", Tag.TAG_LIST)) {
            inventory.fromTag(tag.getList("NpcInventory", Tag.TAG_COMPOUND), registryAccess());
        }
        if (hasCustomName()) setCustomNameVisible(true);
    }
}
