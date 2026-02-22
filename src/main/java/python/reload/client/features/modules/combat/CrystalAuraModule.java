package python.reload.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import python.reload.api.event.EventListener;
import python.reload.api.event.Listener;
import python.reload.api.event.events.player.other.UpdateEvent;
import python.reload.api.module.Category;
import python.reload.api.module.Module;
import python.reload.api.module.ModuleRegister;
import python.reload.api.module.setting.BooleanSetting;
import python.reload.api.module.setting.ModeSetting;
import python.reload.api.module.setting.MultiBooleanSetting;
import python.reload.api.module.setting.SliderSetting;
import python.reload.api.utils.combat.TargetManager;
import python.reload.api.utils.math.TimerUtil;
import python.reload.api.utils.rotation.RotationUtil;
import python.reload.api.utils.rotation.manager.Rotation;
import python.reload.api.utils.rotation.manager.RotationManager;
import python.reload.api.utils.rotation.manager.RotationStrategy;
import python.reload.api.utils.task.TaskPriority;
import python.reload.client.features.modules.movement.MoveFixModule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@ModuleRegister(name = "Crystal Aura", category = Category.COMBAT)
public class CrystalAuraModule extends Module {
    @Getter private static final CrystalAuraModule instance = new CrystalAuraModule();

    private final TargetManager targetManager = new TargetManager();

    // Targeting
    private final SliderSetting range = new SliderSetting("Range")
            .value(5.0f)
            .range(1.0f, 7.0f)
            .step(0.1f);

    private final MultiBooleanSetting targets = new MultiBooleanSetting("Targets").value(
            new BooleanSetting("Players").value(true),
            new BooleanSetting("Mobs").value(false),
            new BooleanSetting("Animals").value(false)
    );

    // Placement
    private final SliderSetting placeRange = new SliderSetting("Place Range")
            .value(4.5f)
            .range(1.0f, 7.0f)
            .step(0.1f);

    private final SliderSetting placeDelay = new SliderSetting("Place Delay")
            .value(50f)
            .range(0f, 500f)
            .step(10f);

    private final BooleanSetting autoSwap = new BooleanSetting("Auto Swap")
            .value(true);

    private final ModeSetting swapMode = new ModeSetting("Swap Mode")
            .value("Silent")
            .values("Normal", "Silent")
            .setVisible(autoSwap::getValue);

    // Break
    private final SliderSetting breakRange = new SliderSetting("Break Range")
            .value(5.0f)
            .range(1.0f, 7.0f)
            .step(0.1f);

    private final SliderSetting breakDelay = new SliderSetting("Break Delay")
            .value(50f)
            .range(0f, 500f)
            .step(10f);

    // Damage
    private final SliderSetting minDamage = new SliderSetting("Min Damage")
            .value(6.0f)
            .range(0.0f, 20.0f)
            .step(0.5f);

    private final SliderSetting maxSelfDamage = new SliderSetting("Max Self Damage")
            .value(8.0f)
            .range(0.0f, 20.0f)
            .step(0.5f);

    private final BooleanSetting antiSuicide = new BooleanSetting("Anti Suicide")
            .value(true);

    private final SliderSetting minHealth = new SliderSetting("Min Health")
            .value(6.0f)
            .range(0.0f, 20.0f)
            .step(0.5f)
            .setVisible(antiSuicide::getValue);

    // Rotation
    private final BooleanSetting rotate = new BooleanSetting("Rotate")
            .value(true);

    // Misc
    private final BooleanSetting rayTrace = new BooleanSetting("Ray Trace")
            .value(true);

    private final BooleanSetting predict = new BooleanSetting("Predict")
            .value(true);

    private final SliderSetting predictTicks = new SliderSetting("Predict Ticks")
            .value(2f)
            .range(1f, 10f)
            .step(1f)
            .setVisible(predict::getValue);

    private final BooleanSetting inhibit = new BooleanSetting("Inhibit")
            .value(true);

    private final BooleanSetting sequential = new BooleanSetting("Sequential")
            .value(true);

    // State
    @Getter public LivingEntity target;
    private final TimerUtil placeTimer = new TimerUtil();
    private final TimerUtil breakTimer = new TimerUtil();
    private int lastSlot = -1;
    private BlockPos lastPlacedPos = null;
    private long lastPlaceTime = 0;

    public CrystalAuraModule() {
        addSettings(
                range, targets,
                placeRange, placeDelay, autoSwap, swapMode,
                breakRange, breakDelay,
                minDamage, maxSelfDamage, antiSuicide, minHealth,
                rotate, rayTrace, predict, predictTicks,
                inhibit, sequential
        );
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null || mc.world == null) return;

            updateTarget();

            // Всегда пытаемся ломать кристаллы, даже без цели
            doBreak();

            // Размещаем только если есть цель
            if (target != null) {
                doPlace();
            }
        }));

        addEvents(updateEvent);
    }

    private void updateTarget() {
        TargetManager.EntityFilter filter = new TargetManager.EntityFilter(targets.getList());
        targetManager.searchTargets(mc.world.getEntities(), range.getValue());
        targetManager.validateTarget(filter::isValid);
        target = targetManager.getCurrentTarget();
    }

    private void doBreak() {
        if (!breakTimer.finished(breakDelay.getValue().longValue())) return;

        List<EndCrystalEntity> crystals = new ArrayList<>();

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof EndCrystalEntity crystal)) continue;
            if (!crystal.isAlive()) continue;

            double dist = mc.player.getPos().distanceTo(crystal.getPos());
            if (dist > breakRange.getValue()) continue;

            crystals.add(crystal);
        }

        if (crystals.isEmpty()) return;

        // Сортируем по урону
        crystals.sort((c1, c2) -> {
            float damage1 = target != null ? calculateDamage(c1.getPos(), target) : 0;
            float damage2 = target != null ? calculateDamage(c2.getPos(), target) : 0;

            // Если нет цели, ломаем ближайший
            if (target == null) {
                return Double.compare(
                        mc.player.getPos().distanceTo(c1.getPos()),
                        mc.player.getPos().distanceTo(c2.getPos())
                );
            }

            return Float.compare(damage2, damage1);
        });

        for (EndCrystalEntity crystal : crystals) {
            float damage = target != null ? calculateDamage(crystal.getPos(), target) : 0;
            float selfDamage = calculateDamage(crystal.getPos(), mc.player);

            // Проверяем anti-suicide даже без цели
            if (antiSuicide.getValue()) {
                if (mc.player.getHealth() + mc.player.getAbsorptionAmount() - selfDamage <= minHealth.getValue()) {
                    continue;
                }
            }

            // Если есть цель, проверяем урон
            if (target != null) {
                if (damage < minDamage.getValue()) continue;
                if (selfDamage > maxSelfDamage.getValue()) continue;
            } else {
                // Без цели просто проверяем что не убьёт нас
                if (selfDamage > maxSelfDamage.getValue()) continue;
            }

            breakCrystal(crystal);
            breakTimer.reset();

            if (sequential.getValue()) {
                break; // Ломаем только один кристалл за раз
            }
        }
    }

    private void doPlace() {
        if (!placeTimer.finished(placeDelay.getValue().longValue())) return;

        int crystalSlot = findCrystalSlot();
        if (crystalSlot == -1) return;

        // Inhibit - не ставим если есть кристалл рядом
        if (inhibit.getValue()) {
            for (Entity entity : mc.world.getEntities()) {
                if (!(entity instanceof EndCrystalEntity crystal)) continue;
                if (mc.player.getPos().distanceTo(crystal.getPos()) <= breakRange.getValue()) {
                    return; // Ждём пока сломаем существующие
                }
            }
        }

        BlockPos bestPos = null;
        float maxDamage = 0;

        List<BlockPos> validPositions = getValidPositions();

        for (BlockPos pos : validPositions) {
            Vec3d crystalPos = Vec3d.ofCenter(pos).add(0, 1, 0);

            float damage = calculateDamage(crystalPos, target);
            float selfDamage = calculateDamage(crystalPos, mc.player);

            if (!isValidDamage(damage, selfDamage)) continue;

            if (damage > maxDamage) {
                maxDamage = damage;
                bestPos = pos;
            }
        }

        if (bestPos != null) {
            placeCrystal(bestPos, crystalSlot);
            placeTimer.reset();
            lastPlacedPos = bestPos;
            lastPlaceTime = System.currentTimeMillis();
        }
    }

    private List<BlockPos> getValidPositions() {
        List<BlockPos> positions = new ArrayList<>();

        if (target == null) return positions;

        Vec3d targetPos = predict.getValue()
                ? target.getPos().add(target.getVelocity().multiply(predictTicks.getValue()))
                : target.getPos();

        BlockPos centerPos = new BlockPos((int)targetPos.x, (int)targetPos.y, (int)targetPos.z);
        int r = (int) Math.ceil(placeRange.getValue());

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = centerPos.add(x, y, z);

                    double distToPlayer = mc.player.getPos().distanceTo(Vec3d.ofCenter(pos));
                    if (distToPlayer > placeRange.getValue()) continue;

                    if (canPlaceCrystal(pos)) {
                        positions.add(pos);
                    }
                }
            }
        }

        // Сортируем по урону
        positions.sort((p1, p2) -> {
            Vec3d pos1 = Vec3d.ofCenter(p1).add(0, 1, 0);
            Vec3d pos2 = Vec3d.ofCenter(p2).add(0, 1, 0);

            float damage1 = calculateDamage(pos1, target);
            float damage2 = calculateDamage(pos2, target);

            return Float.compare(damage2, damage1);
        });

        return positions;
    }

    private boolean canPlaceCrystal(BlockPos pos) {
        BlockState state = mc.world.getBlockState(pos);
        if (!state.isOf(Blocks.OBSIDIAN) && !state.isOf(Blocks.BEDROCK)) return false;

        BlockPos up = pos.up();
        BlockPos up2 = up.up();

        if (!mc.world.isAir(up)) return false;
        if (!mc.world.isAir(up2)) return false;

        // Проверяем что в этой позиции нет сущностей
        Box crystalBox = new Box(
                pos.getX(), pos.getY() + 1, pos.getZ(),
                pos.getX() + 1, pos.getY() + 3, pos.getZ() + 1
        );

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;
            if (entity.getBoundingBox().intersects(crystalBox)) return false;
        }

        return true;
    }

    private boolean isValidDamage(float damage, float selfDamage) {
        if (damage < minDamage.getValue()) return false;
        if (selfDamage > maxSelfDamage.getValue()) return false;

        if (antiSuicide.getValue()) {
            float health = mc.player.getHealth() + mc.player.getAbsorptionAmount();
            if (health - selfDamage <= minHealth.getValue()) {
                return false;
            }
        }

        return true;
    }

    private void placeCrystal(BlockPos pos, int slot) {
        boolean needSwap = mc.player.getInventory().selectedSlot != slot;
        if (needSwap && !autoSwap.getValue()) return;

        if (needSwap) {
            lastSlot = mc.player.getInventory().selectedSlot;
            if (swapMode.is("Silent")) {
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
            } else {
                mc.player.getInventory().selectedSlot = slot;
            }
        }

        Vec3d hitVec = Vec3d.ofCenter(pos).add(0, 0.5, 0);

        if (rotate.getValue()) {
            Rotation rotation = RotationUtil.rotationAt(hitVec);
            RotationStrategy strategy = new RotationStrategy(
                    new python.reload.api.utils.rotation.rotations.SmoothRotation(),
                    MoveFixModule.enabled(),
                    false
            );
            RotationManager.getInstance().addRotation(
                    new Rotation.VecRotation(rotation, hitVec),
                    target,
                    strategy,
                    TaskPriority.HIGH,
                    this
            );
        }

        BlockHitResult hitResult = new BlockHitResult(
                hitVec,
                Direction.UP,
                pos,
                false
        );

        mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, hitResult, 0));
        mc.player.swingHand(Hand.MAIN_HAND);

        if (needSwap && swapMode.is("Silent")) {
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(lastSlot));
        }
    }

    private void breakCrystal(EndCrystalEntity crystal) {
        if (rotate.getValue()) {
            Vec3d crystalPos = crystal.getPos();
            Rotation rotation = RotationUtil.rotationAt(crystalPos);
            RotationStrategy strategy = new RotationStrategy(
                    new python.reload.api.utils.rotation.rotations.SmoothRotation(),
                    MoveFixModule.enabled(),
                    false
            );
            RotationManager.getInstance().addRotation(
                    new Rotation.VecRotation(rotation, crystalPos),
                    target,
                    strategy,
                    TaskPriority.HIGH,
                    this
            );
        }

        mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private float calculateDamage(Vec3d crystalPos, LivingEntity entity) {
        if (entity == null) return 0;

        double dist = entity.getPos().distanceTo(crystalPos);
        if (dist > 12) return 0;

        double size = 12.0;
        double blockDensity = getExposure(crystalPos, entity);

        double impact = (1.0 - (dist / size)) * blockDensity;
        float rawDamage = (float) ((int) ((impact * impact + impact) / 2.0 * 7.0 * size + 1.0));

        // Упрощённый расчёт с бронёй для игроков
        if (entity instanceof PlayerEntity player) {
            int armorValue = player.getArmor();
            int toughness = 0; // Можно добавить расчёт toughness

            float damageAfterArmor = rawDamage * (1.0f - Math.min(20.0f, Math.max(armorValue / 5.0f, armorValue - rawDamage / (2.0f + toughness / 4.0f))) / 25.0f);
            return Math.max(0, damageAfterArmor);
        }

        return rawDamage;
    }

    private float getExposure(Vec3d source, Entity entity) {
        Box box = entity.getBoundingBox();
        double xStep = 1.0 / ((box.maxX - box.minX) * 2.0 + 1.0);
        double yStep = 1.0 / ((box.maxY - box.minY) * 2.0 + 1.0);
        double zStep = 1.0 / ((box.maxZ - box.minZ) * 2.0 + 1.0);

        double xOffset = (1.0 - Math.floor(1.0 / xStep) * xStep) / 2.0;
        double zOffset = (1.0 - Math.floor(1.0 / zStep) * zStep) / 2.0;

        if (xStep < 0.0 || yStep < 0.0 || zStep < 0.0) {
            return 0.0f;
        }

        int visiblePoints = 0;
        int totalPoints = 0;

        for (double x = 0.0; x <= 1.0; x += xStep) {
            for (double y = 0.0; y <= 1.0; y += yStep) {
                for (double z = 0.0; z <= 1.0; z += zStep) {
                    double pointX = box.minX + (box.maxX - box.minX) * x;
                    double pointY = box.minY + (box.maxY - box.minY) * y;
                    double pointZ = box.minZ + (box.maxZ - box.minZ) * z;

                    Vec3d point = new Vec3d(pointX + xOffset, pointY, pointZ + zOffset);

                    RaycastContext context = new RaycastContext(
                            point,
                            source,
                            RaycastContext.ShapeType.COLLIDER,
                            RaycastContext.FluidHandling.NONE,
                            entity
                    );

                    BlockHitResult result = mc.world.raycast(context);

                    if (result.getType() == HitResult.Type.MISS) {
                        visiblePoints++;
                    }

                    totalPoints++;
                }
            }
        }

        if (totalPoints == 0) return 0.0f;
        return (float) visiblePoints / (float) totalPoints;
    }

    private int findCrystalSlot() {
        // Сначала проверяем текущий слот
        if (mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL) {
            return mc.player.getInventory().selectedSlot;
        }

        // Ищем в хотбаре
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.END_CRYSTAL) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public void onEnable() {
        if (mc.player == null) return;
        target = null;
        placeTimer.reset();
        breakTimer.reset();
        lastPlacedPos = null;
        lastSlot = -1;
    }

    @Override
    public void onDisable() {
        targetManager.releaseTarget();
        target = null;
        lastPlacedPos = null;
    }
}