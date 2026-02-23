package python.reload.api.system.client;

import com.mojang.authlib.GameProfile;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class FakePlayerManager {
    @Getter
    private static final FakePlayerManager instance = new FakePlayerManager();
    private final MinecraftClient mc = MinecraftClient.getInstance();

    @Getter @Setter
    private Vec3d playerPosition;

    @Getter
    private final Map<Integer, Long> lastAttackTimeMap = new HashMap<>();
    private final Map<String, OtherClientPlayerEntity> fakeZelenskyi = new HashMap<>();
    private final LinkedList<String> spawnOrder = new LinkedList<>();

    public boolean add(String name) {
        if (mc.player == null || mc.world == null) return false;

        String key = name.toLowerCase();
        if (fakeZelenskyi.containsKey(key)) return false;

        GameProfile profile = new GameProfile(UUID.randomUUID(), name);
        OtherClientPlayerEntity fakeZelensky = new OtherClientPlayerEntity(mc.world, profile);

        fakeZelensky.copyFrom(mc.player);
        fakeZelensky.copyPositionAndRotation(mc.player);
        fakeZelensky.setHealth(20.0f);

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            fakeZelensky.equipStack(slot, mc.player.getEquippedStack(slot).copy());
        }

        fakeZelensky.headYaw = mc.player.headYaw;
        fakeZelensky.bodyYaw = mc.player.bodyYaw;

        setPlayerPosition(mc.player.getPos());

        mc.world.addEntity(fakeZelensky);
        fakeZelenskyi.put(key, fakeZelensky);
        spawnOrder.add(key);
        return true;
    }

    public void onTick() {
        if (mc.world == null) return;
        for (OtherClientPlayerEntity fp : fakeZelenskyi.values()) {
            if (fp.getHealth() <= 0) {
                useTotem(fp);
            }
        }
    }

    public void handleDamage(int id) {
        for (OtherClientPlayerEntity fp : fakeZelenskyi.values()) {
            if (fp.getId() == id) {
                long currentTime = System.currentTimeMillis();
                long lastTime = lastAttackTimeMap.getOrDefault(id, currentTime - 2000);

                float attackSpeed = 1.6f;
                ItemStack hand = mc.player.getMainHandStack();
                if (hand.getItem() instanceof SwordItem) {
                    attackSpeed = 1.6f;
                } else if (hand.getItem().toString().contains("_axe")) {
                    attackSpeed = 1.0f;
                }

                float cooldownMs = 1000.0f / attackSpeed;
                float randomCooldown = cooldownMs + ThreadLocalRandom.current().nextFloat(-75, +75);
                float strength = MathHelper.clamp(((currentTime - lastTime) + 80) / randomCooldown, 0.0f, 1.0f);

                lastAttackTimeMap.put(id, currentTime);

                boolean isFullStrength = strength >= 0.95f;
                boolean isCrit = isFullStrength && mc.player.fallDistance > 0.0f && !mc.player.isOnGround() && !mc.player.isClimbing() && !mc.player.isTouchingWater();

                float damage;
                if (isCrit) {
                    damage = 4.0f;
                    spawnCritParticles(fp);
                    mc.world.playSound(fp.getX(), fp.getY(), fp.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, fp.getSoundCategory(), 1.0f, 1.0f, false);
                } else if (isFullStrength) {
                    damage = 3.0f;
                    spawnComboEffects(fp);
                    mc.world.playSound(fp.getX(), fp.getY(), fp.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, fp.getSoundCategory(), 1.0f, 1.0f, false);
                } else {
                    damage = 1.0f;
                    mc.world.playSound(fp.getX(), fp.getY(), fp.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_WEAK, fp.getSoundCategory(), 1.0f, 1.0f, false);
                }

                applyFinalDamage(fp, damage);
                break;
            }
        }
    }

    private void applyFinalDamage(OtherClientPlayerEntity fp, float amount) {
        fp.setHealth(fp.getHealth() - amount);
        fp.hurtTime = 10;
        fp.maxHurtTime = 10;
        fp.handleStatus((byte) 2);

        if (fp.getHealth() <= 0) {
            useTotem(fp);
        }
    }

    private void spawnComboEffects(OtherClientPlayerEntity fp) {
        double yawRad = mc.player.getYaw() * 0.0174f;
        double d = -MathHelper.sin((float) yawRad);
        double e = MathHelper.cos((float) yawRad);
        mc.world.addParticle(ParticleTypes.SWEEP_ATTACK, fp.getX() + d, fp.getY() + 1.0, fp.getZ() + e, d, 0.0, e);

        for (int i = 0; i < 6; i++) {
            mc.world.addParticle(ParticleTypes.ENCHANTED_HIT, fp.getX() + (Math.random() - 0.5), fp.getY() + 1.0, fp.getZ() + (Math.random() - 0.5), 0, 0.1, 0);
        }
    }

    private void spawnCritParticles(OtherClientPlayerEntity fp) {
        for (int i = 0; i < 15; i++) {
            mc.world.addParticle(ParticleTypes.CRIT, fp.getX() + (Math.random() - 0.5), fp.getY() + 1.2, fp.getZ() + (Math.random() - 0.5), (Math.random() - 0.5) * 0.5, 0.1, (Math.random() - 0.5) * 0.5);
        }
    }

    private void useTotem(OtherClientPlayerEntity fp) {
        fp.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        fp.handleStatus((byte) 35);

        fp.setHealth(20.0f);
        fp.clearStatusEffects();
        fp.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 900, 1));
        fp.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 100, 1));

        mc.world.playSound(fp.getX(), fp.getY(), fp.getZ(), SoundEvents.ITEM_TOTEM_USE, fp.getSoundCategory(), 1.0f, 1.0f, false);

        CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS).execute(() -> {
            fp.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        });
    }

    public String removeLast() {
        if (spawnOrder.isEmpty()) return null;
        String lastJoined = spawnOrder.removeLast();
        OtherClientPlayerEntity entity = fakeZelenskyi.get(lastJoined);
        if (entity != null) {
            lastAttackTimeMap.remove(entity.getId());
        }
        removeEntity(lastJoined);
        return lastJoined;
    }

    public void clear() {
        for (String name : new ArrayList<>(fakeZelenskyi.keySet())) {
            removeEntity(name);
        }
        fakeZelenskyi.clear();
        spawnOrder.clear();
        lastAttackTimeMap.clear();
        setPlayerPosition(null);
    }

    private void removeEntity(String key) {
        OtherClientPlayerEntity entity = fakeZelenskyi.get(key);
        if (entity != null && mc.world != null) {
            mc.world.removeEntity(entity.getId(), Entity.RemovalReason.DISCARDED);
            lastAttackTimeMap.remove(entity.getId());
        }
        fakeZelenskyi.remove(key);
    }
}