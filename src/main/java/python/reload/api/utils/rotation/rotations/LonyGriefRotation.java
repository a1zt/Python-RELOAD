package python.reload.api.utils.rotation.rotations;

import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import python.reload.api.utils.math.MathUtil;
import python.reload.api.utils.rotation.RaytracingUtil;
import python.reload.api.utils.rotation.RotationUtil;
import python.reload.api.utils.rotation.manager.Rotation;
import python.reload.api.utils.rotation.manager.RotationMode;
import python.reload.client.features.modules.combat.AuraModule;

public class LonyGriefRotation extends RotationMode {
    private static int ticksSinceLastJerk = 0;
    private static int nextJerkTick = 8;

    private static float yawSpeedMultiplier = 0.0f;
    private static float pitchSpeedMultiplier = 0.0f;

    private static float accelerationSpeed = 0.15f;
    private static float decelerationSpeed = 0.35f;

    public LonyGriefRotation() {
        super("Lony Grief");
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3d vec3d, Entity entity) {
        Rotation delta = RotationUtil.calculateDelta(currentRotation, targetRotation);
        float yawDelta = delta.getYaw();
        float pitchDelta = delta.getPitch();

        float distance = AuraModule.getInstance().getAttackDistance() + AuraModule.getInstance().getPreDistance();
        boolean hasTarget = entity != null;

        EntityHitResult hitResult = RaytracingUtil.raytraceEntity(distance, currentRotation, false);
        boolean hasTrace = hasTarget && hitResult != null && hitResult.getEntity() == entity;

        float accelerationSpeed = 0.03f;

        if (Math.abs(yawDelta) > 15.0f) {
            accelerationSpeed = MathUtil.randomInRange(0.06f, 0.32f);
            yawDelta *= 1.5f;
        }

        if (!hasTrace) {
            yawSpeedMultiplier = MathUtil.interpolate(yawSpeedMultiplier, 1.0f, accelerationSpeed);
            pitchSpeedMultiplier = MathUtil.interpolate(pitchSpeedMultiplier, 1.0f, accelerationSpeed);
        } else {
            float decelerationSpeed = 0.55f;
            yawSpeedMultiplier = MathUtil.interpolate(yawSpeedMultiplier, 0.0f, decelerationSpeed);
            pitchSpeedMultiplier = MathUtil.interpolate(pitchSpeedMultiplier, 0.0f, decelerationSpeed);
        }

        float baseYawSpeed = MathUtil.randomInRange(38.0f, 48.0f) / (mc.player.isGliding() ? 3f : 1f);
        float basePitchSpeed = MathUtil.randomInRange(12.0f, 19.0f) / (mc.player.isGliding() ? 3f : 1f);

        float distanceFactor = MathHelper.clamp(Math.abs(yawDelta) / 15.0f, 0.2f, 1.0f);

        float yawSmoothMultiplier = yawSpeedMultiplier * yawSpeedMultiplier * (3 - 2 * yawSpeedMultiplier);
        float pitchSmoothMultiplier = pitchSpeedMultiplier * pitchSpeedMultiplier * (3 - 2 * pitchSpeedMultiplier);

        float yawSpeed = baseYawSpeed * yawSmoothMultiplier * distanceFactor;
        float pitchSpeed = basePitchSpeed * pitchSmoothMultiplier;

        if (Math.abs(yawDelta) > 5.0f) {
            float noise = (float) Math.sin(System.currentTimeMillis() / 20.0) * 5.0f;
            yawDelta += noise;
        }

        if (Math.abs(pitchDelta) > 3.0f) {
            float pitchNoise = (float) Math.cos(System.currentTimeMillis() / 20.0) * 155.5f;
            pitchDelta += pitchNoise;
        }

        ticksSinceLastJerk++;
        if (ticksSinceLastJerk >= nextJerkTick) {
            boolean isFliking = Math.abs(yawDelta) > 15.0f;

            if (!isFliking && hasTrace) {
                float jerkYaw = MathUtil.randomInRange(0.4f, 0.8f);
                float jerkPitch = MathUtil.randomInRange(0.5f, 0.9f);
                yawDelta *= jerkYaw;
                pitchDelta *= jerkPitch;
            }

            ticksSinceLastJerk = 0;
            nextJerkTick = MathUtil.randomInRange(4, 10);
        }

        float clampedYawDelta = MathHelper.clamp(yawDelta, -yawSpeed, yawSpeed);
        float clampedPitchDelta = MathHelper.clamp(pitchDelta, -pitchSpeed, pitchSpeed);

        if (hasTrace && Math.abs(clampedYawDelta) < 0.2f) clampedYawDelta = 0;
        //if (hasTrace && Math.abs(clampedPitchDelta) < 0.2f) clampedPitchDelta = 0;

        return new Rotation(
                currentRotation.getYaw() + clampedYawDelta,
                currentRotation.getPitch() + clampedPitchDelta
        );
    }
}