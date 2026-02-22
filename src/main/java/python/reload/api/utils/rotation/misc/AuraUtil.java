package python.reload.api.utils.rotation.misc;

import lombok.experimental.UtilityClass;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import python.reload.api.system.interfaces.QuickImports;
import python.reload.api.utils.math.MathUtil;
import python.reload.api.utils.math.TimerUtil;
import python.reload.api.utils.rotation.RotationUtil;
import python.reload.client.features.modules.combat.AuraModule;

@UtilityClass
public class AuraUtil implements QuickImports {
    private Vec3d dvdPoint = Vec3d.ZERO;
    private Vec3d dvdMotion = Vec3d.ZERO;

    private float hitCount = 0;
    private TimerUtil attackTimer = new TimerUtil();

    public void onAttack(String mode) {
        switch (mode) {
            case "Spooky Time" -> {
                float hits = 0.3f;
                hitCount += hits;
                if (hitCount >= hits * 2) {
                    hitCount = -hits;
                }
            }

            case "Lony Grief" -> {
                attackTimer.reset();
            }

            default -> {
                hitCount += 1;
                if (hitCount >= 3) {
                    hitCount = 0;
                }
            }
        }
    }

    public Vec3d getAimpoint(LivingEntity entity, String mode) {
        switch (mode) {
            case "Holy World" -> {
                return getDvDPoint(entity);
            }

            case "Spooky Time" -> {
                return getSpookyTimePoint(entity);
                //return getBestVector(entity, 0f);
            }

            case "Lony Grief" -> {
                return getDistanceBasedPoint(entity);
                //return getBestVector(entity, 0f);
            }

            case "Fun Time" -> {
                return getDistanceBasedPoint(entity);
            }

            default -> {
                return RotationUtil.getSpot(entity);
            }
        }
    }

    public Vec3d getBestVector(Entity entity, float jitterOnBoxValue) {
        double yExpand = MathHelper.clamp(mc.player.getEyeHeight(mc.player.getPose()) - entity.getEyeHeight(entity.getPose()), entity.getHeight() / 2, entity.getHeight())
                / (mc.player.isGliding() ? 10 : !mc.options.jumpKey.isPressed() && mc.player.isOnGround() ?
                entity.isSneaking() ? 0.8F : 0.6f : 1F);

        Vec3d finalVector = entity.getPos().add(0, yExpand, 0);
        return finalVector.add(jitterOnBoxValue, jitterOnBoxValue / 2, jitterOnBoxValue);
    }

    public Vec3d getSpookyTimePoint(Entity entity) {
        float safe = 0.06f;

        float horValue = (entity.getWidth() / 2f) * hitCount;
        float verValue = (entity.getHeight() / 4f) * (hitCount + 1);

        Box box = entity.getBoundingBox();
        Vec3d best = getBestVector(entity, 0f);
        Vec3d point = new Vec3d(
                MathHelper.clamp(best.x - horValue, box.minX + safe, box.maxX - safe),
                MathHelper.clamp(best.y - verValue, box.minY + safe, box.maxY - safe),
                MathHelper.clamp(best.z + horValue, box.minZ + safe, box.maxZ - safe)
        );

        return point;
    }

    public Vec3d getDistanceBasedPoint(Entity entity) {
        Vec3d eye = mc.player.getEyePos();
        Box box = entity.getBoundingBox();

        float attackDistance = AuraModule.getInstance().getAttackDistance() + AuraModule.getInstance().getPreDistance();
        float distanceFactor = (float) (mc.player.getPos().distanceTo(entity.getPos()) / attackDistance);

        float minY = (float) (box.maxY - box.minY);
        float clampedY = (float) Math.max(box.minY + minY * distanceFactor, box.minY + minY * 0.3f);

        float safePoint = entity.getWidth() * 0.4f;

        Vec3d basePoint = new Vec3d(
                MathHelper.clamp(eye.x, box.minX + safePoint, box.maxX - safePoint),
                MathHelper.clamp(eye.y, box.minY, clampedY),
                MathHelper.clamp(eye.z, box.minZ + safePoint, box.maxZ - safePoint)
        );

        return basePoint;
    }

    public Vec3d getDvDPoint(Entity entity) {
        float minMotionXZ = 0.003f;
        float maxMotionXZ = 0.04f;

        float minMotionY = 0.001f;
        float maxMotionY = 0.03f;

        double lengthX = entity.getBoundingBox().getLengthX();
        double lengthY = entity.getBoundingBox().getLengthY() * 0.8f;
        double lengthZ = entity.getBoundingBox().getLengthZ();

        if (dvdMotion.equals(Vec3d.ZERO))
            dvdMotion = new Vec3d(MathUtil.randomInRange(-0.05f, 0.05f), MathUtil.randomInRange(-0.05f, 0.05f), MathUtil.randomInRange(-0.05f, 0.05f));

        dvdPoint = dvdPoint.add(dvdMotion);

        if (dvdPoint.x >= (lengthX - 0.05) / 2.0)
            dvdMotion = new Vec3d(-MathUtil.randomInRange(minMotionXZ, maxMotionXZ), dvdMotion.getY(), dvdMotion.getZ());

        if (dvdPoint.y >= lengthY)
            dvdMotion = new Vec3d(dvdMotion.getX(), -MathUtil.randomInRange(minMotionY, maxMotionY), dvdMotion.getZ());

        if (dvdPoint.z >= (lengthZ - 0.05) / 2.0)
            dvdMotion = new Vec3d(dvdMotion.getX(), dvdMotion.getY(), -MathUtil.randomInRange(minMotionXZ, maxMotionXZ));

        if (dvdPoint.x <= -(lengthX - 0.05) / 2.0)
            dvdMotion = new Vec3d(MathUtil.randomInRange(minMotionXZ, 0.03f), dvdMotion.getY(), dvdMotion.getZ());

        if (dvdPoint.y <= 0.05)
            dvdMotion = new Vec3d(dvdMotion.getX(), MathUtil.randomInRange(minMotionY, maxMotionY), dvdMotion.getZ());

        if (dvdPoint.z <= -(lengthZ - 0.05) / 2.0)
            dvdMotion = new Vec3d(dvdMotion.getX(), dvdMotion.getY(), MathUtil.randomInRange(minMotionXZ, maxMotionXZ));

        dvdPoint.add(MathUtil.randomInRange(-0.03f, 0.03f), 0f, MathUtil.randomInRange(-0.03f, 0.03f));

        Vec3d dvdPointed = entity.getPos().add(dvdPoint);
        Box box = entity.getBoundingBox();
        return new Vec3d(
                MathHelper.clamp(dvdPointed.x, box.minX, box.maxX),
                MathHelper.clamp(dvdPointed.y, box.minY, box.maxY),
                MathHelper.clamp(dvdPointed.z, box.minZ, box.maxZ)
        );
    }
}