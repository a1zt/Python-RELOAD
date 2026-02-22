package python.reload.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import python.reload.api.event.Listener;
import python.reload.api.event.EventListener;
import python.reload.api.event.events.other.RotationUpdateEvent;
import python.reload.api.event.events.player.world.AttackEvent;
import python.reload.api.event.events.player.other.UpdateEvent;
import python.reload.api.module.Category;
import python.reload.api.module.Module;
import python.reload.api.module.ModuleRegister;
import python.reload.api.module.setting.BooleanSetting;
import python.reload.api.module.setting.ModeSetting;
import python.reload.api.module.setting.MultiBooleanSetting;
import python.reload.api.module.setting.SliderSetting;
import python.reload.api.utils.combat.CombatExecutor;
import python.reload.api.utils.combat.TargetManager;
import python.reload.api.utils.neuro.AIPredictor;
import python.reload.api.utils.rotation.misc.AuraUtil;
import python.reload.api.utils.math.MathUtil;
import python.reload.api.utils.rotation.RotationUtil;
import python.reload.api.utils.rotation.manager.Rotation;
import python.reload.api.utils.rotation.manager.RotationManager;
import python.reload.api.utils.rotation.manager.RotationMode;
import python.reload.api.utils.rotation.manager.RotationStrategy;
import python.reload.api.utils.rotation.rotations.*;
import python.reload.api.utils.rotation.rotations.*;
import python.reload.api.utils.task.TaskPriority;
import python.reload.client.features.modules.combat.elytratarget.ElytraTargetModule;
import python.reload.client.features.modules.movement.MoveFixModule;

@ModuleRegister(name = "Aura", category = Category.COMBAT)
public class AuraModule extends Module {
    @Getter private static final AuraModule instance = new AuraModule();

    private final AIPredictor predictor = new AIPredictor();
    private final TargetManager targetManager = new TargetManager();
    public final CombatExecutor combatExecutor = new CombatExecutor();

    // aiming
    @Getter private final ModeSetting aimMode = new ModeSetting("Aim mode").value("Smooth").values(
            "Smooth", "Snap", "HvH",
            "Matrix", "Vulcan",
            "Spooky Time", "Fun Time", "Holy World", "Lony Grief", "Neuro"
    ).onAction(() -> {
        if (getAimMode().is("Neuro")) {
            if (predictor.isLoaded()) {
                predictor.close();
            }
            loadModel();
        } else {
            predictor.close();
        }
    });

    // targeting and distance
    private final SliderSetting distance = new SliderSetting("Distance").value(3f).range(2.5f, 6f).step(0.1f);
    private final SliderSetting preDistance = new SliderSetting("Pre distance").value(0.3f).range(0f, 3f).step(0.1f);
    private final MultiBooleanSetting targets = new MultiBooleanSetting("Targets").value(
            new BooleanSetting("Players").value(true),
            new BooleanSetting("Mobs").value(true),
            new BooleanSetting("Animals").value(true)
    );

    // options
    public final MultiBooleanSetting options = combatExecutor.options();

    private final BooleanSetting clientLook = new BooleanSetting("Client look").value(false);
    private final BooleanSetting elytraOverride = new BooleanSetting("Elytra override").value(false);
    private final SliderSetting elytraDistance = new SliderSetting("Elytra distance").value(4f).range(2.5f, 6f).step(0.1f).setVisible(elytraOverride::getValue);
    private final SliderSetting elytraPreDistance = new SliderSetting("Elytra pre distance").value(16f).range(0f, 32f).step(0.1f).setVisible(elytraOverride::getValue);

    public LivingEntity target;

    public AuraModule() {
        addSettings(aimMode, distance, preDistance, targets, options, clientLook,
                elytraOverride, elytraDistance, elytraPreDistance
        );
    }

    public float getPreDistance() {
        return (mc.player.isGliding() && elytraOverride.getValue()) ? elytraPreDistance.getValue() : preDistance.getValue();
    }

    public float getAttackDistance() {
        return (mc.player.isGliding() && elytraOverride.getValue()) ? elytraDistance.getValue() : distance.getValue();
    }

    @Override
    public void onDisable() {
        targetManager.releaseTarget();
        target = null;
        predictor.close();
    }

    @Override
    public void onEnable() {
        targetManager.releaseTarget();
        target = null;

        if (aimMode.is("Neuro") && !predictor.isLoaded()) {
            loadModel();
        }
    }

    public void loadModel() {
        predictor.loadModel("Default");
    }

    @Override
    public void onEvent() {
        predictor.onEvent();

        EventListener eventUpdate = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            updateEventHandler();
        }));

        EventListener rotationUpdateEvent = RotationUpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            postRotMoveEventHandler();
        }));

        EventListener attackEvent = AttackEvent.getInstance().subscribe(new Listener<>(event -> {
            AuraUtil.onAttack(aimMode.getValue());
        }));

        addEvents(predictor.getEventListeners());
        addEvents(eventUpdate, rotationUpdateEvent, attackEvent);
    }

    private void postRotMoveEventHandler() {
        if (target == null) return;

        Vec3d attackVector = getTargetVector(target);
        Rotation rotation = RotationUtil.fromVec3d(attackVector.subtract(mc.player.getEyePos()));

        rotateToTarget(target, attackVector, rotation);
    }

    private void updateEventHandler() {
        target = updateTarget();

        if (target == null) return;

        if (RotationUtil.getSpot(target).distanceTo(mc.player.getEyePos()) > getAttackDistance() + getPreDistance()) {
            targetManager.releaseTarget();
            return;
        }

        if (target != null) {
            attackTarget(target);
        }
    }

    private LivingEntity updateTarget() {
        TargetManager.EntityFilter filter = new TargetManager.EntityFilter(targets.getList());
        targetManager.searchTargets(mc.world.getEntities(), getAttackDistance() + getPreDistance());
        targetManager.validateTarget(filter::isValid);
        return targetManager.getCurrentTarget();
    }

    private void attackTarget(LivingEntity target) {
        combatExecutor.combatManager().configurable(
                new CombatExecutor.CombatConfigurable(
                        target,
                        RotationManager.getInstance().getRotation(),
                        distance.getValue(),
                        options.getList()
                )
        );

        if (mc.player.getEyePos().distanceTo(
                RotationUtil.rayCastBox(target, getTargetVector(target))
        ) > getAttackDistance()) return;

        combatExecutor.performAttack();
    }

    private void rotateToTarget(LivingEntity target, Vec3d targetVec, Rotation rotation) {
        RotationStrategy configurable = new RotationStrategy(getRotationMode(),
                MoveFixModule.enabled(), MoveFixModule.isFree()).clientLook(clientLook.getValue());

        boolean noHitRule = (!combatExecutor.combatManager().canAttack());

        if (usingElytraTarget() && ElytraTargetModule.getInstance().elytraRotationProcessor.customRotations.getValue()) return;

        if (noHitRule && aimMode.is("Snap")) {
            if (!(MoveFixModule.getInstance().isEnabled() && MoveFixModule.getInstance().targeting.getValue()))
                return;
            else rotation = new Rotation(mc.player.getYaw(), mc.player.getPitch());
        }

        RotationManager.getInstance().addRotation(new Rotation.VecRotation(rotation, targetVec), target, configurable, TaskPriority.HIGH, this);
    }

    private RotationMode getRotationMode() {
        return switch (aimMode.getValue()) {
            case "HvH"  -> new InstantRotation();
            case "Spooky Time" -> new SpookyTimeRotation(combatExecutor.combatManager().clickScheduler());
            case "Holy World" -> new UniversalRotation(MathUtil.randomInRange(125, 155), MathUtil.randomInRange(30, 60), true, true);
            case "Vulcan" -> new UniversalRotation(120, 60, false, false);
            case "Lony Grief" -> new LonyGriefRotation();
            case "Fun Time" -> new FunTimeRotation();
            case "Matrix" -> new MatrixRotation();
            case "Neuro" -> new NeuroRotation(predictor, 70, 10);
            default -> new SmoothRotation();
        };
    }

    private Vec3d getTargetVector(LivingEntity target) {
        if (target == null) {
            return Vec3d.ZERO;
        }

        if (usingElytraTarget()) {
            return ElytraTargetModule.getInstance().elytraRotationProcessor.getPredictedPos(target);
        }

        return AuraUtil.getAimpoint(target, aimMode.getValue());
    }

    private boolean usingElytraTarget() {
        return target != null && ElytraTargetModule.getInstance().elytraRotationProcessor.using();
    }
}
