package python.reload.client.features.modules.render.targetesp.modes;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import python.reload.api.event.events.render.Render3DEvent;
import python.reload.api.system.files.FileUtil;
import python.reload.api.utils.color.ColorUtil;
import python.reload.api.utils.color.UIColors;
import python.reload.api.utils.math.MathUtil;
import python.reload.api.utils.render.RenderUtil;
import python.reload.client.features.modules.render.targetesp.TargetEspMode;

import java.util.ArrayList;
import java.util.List;

/**
 * TargetEspPhoenix — «Rising Phoenix»
 *
 * ┌────────────────────────────────────────────────────┐
 * │  Layer 1 — Огненные крылья (2 крыла, взмахивают)   │
 * │  Layer 2 — Перья (отлетающие частицы)               │
 * │  Layer 3 — Хвост пламени (tail fire trail)          │
 * │  Layer 4 — Огненный вихрь у ног                     │
 * │  Layer 5 — Пепельные искры (rising ash)              │
 * │  Layer 6 — Нимб пламени над головой                  │
 * └────────────────────────────────────────────────────┘
 */
public class TargetEspPhoenix extends TargetEspMode {

    private float wingPhase   = 0f, prevWingPhase   = 0f;
    private float flameTime   = 0f, prevFlameTime   = 0f;
    private float ashTime     = 0f, prevAshTime     = 0f;

    private final List<double[]> fireTrails = new ArrayList<>();

    private static final int   WING_SEGMENTS     = 14;
    private static final int   FEATHER_COUNT     = 18;
    private static final int   TAIL_PARTICLES    = 16;
    private static final int   TAIL_TRAIL_LEN    = 10;
    private static final int   VORTEX_SEGMENTS   = 16;
    private static final int   ASH_PARTICLES     = 20;
    private static final int   HALO_SEGMENTS     = 12;

    @Override
    public void onUpdate() {
        updateTarget();

        prevWingPhase = wingPhase;
        prevFlameTime = flameTime;
        prevAshTime   = ashTime;

        wingPhase += 8f;
        flameTime += 10f;
        ashTime   += 4f;
    }

    @Override
    public void onRender3D(Render3DEvent.Render3DEventData event) {
        if (currentTarget == null || !canDraw()) return;

        MatrixStack stack = event.matrixStack();
        RenderUtil.WORLD.startRender(stack);

        float alphaPC = (float) showAnimation.getValue();
        float anim    = (float) MathUtil.interpolate(prevSizeAnimation, sizeAnimation.getValue());

        Camera camera = mc.gameRenderer.getCamera();
        Vec3d  camPos = camera.getPos();

        double cx = getTargetX();
        double cy = getTargetY();
        double cz = getTargetZ();

        float height = currentTarget.getHeight();
        float width  = currentTarget.getWidth();
        double midY  = cy + height * 0.55;

        float wing  = MathUtil.interpolate(prevWingPhase, wingPhase);
        float flame = MathUtil.interpolate(prevFlameTime, flameTime);
        float ash   = MathUtil.interpolate(prevAshTime, ashTime);

        float baseR = width * 1.2f * anim;

        // взмах крыльев
        float wingFlap = (float) Math.sin(Math.toRadians(wing * 1.5f));
        float flapAngle = wingFlap * 35f; // градусы отклонения

        /* ═══════════════════════════════════════════════════
         *  LAYER 1 — Wings
         *  Каждое крыло — серия точек по кривой Безье
         * ═══════════════════════════════════════════════════ */
        for (int side = -1; side <= 1; side += 2) {
            for (int s = 0; s < WING_SEGMENTS; s++) {
                float t = (float) s / (WING_SEGMENTS - 1);

                // форма крыла: размах увеличивается, потом загибается
                float span    = baseR * (0.3f + t * 1.4f);
                float wingY   = (float) Math.sin(t * Math.PI * 0.8f) * height * 0.4f;
                float curve   = t * t * baseR * 0.3f; // загиб кончика назад

                // взмах вверх/вниз
                float flapRad = (float) Math.toRadians(flapAngle * t);
                wingY += Math.sin(flapRad) * span * 0.3f;

                double px = cx + side * span;
                double py = midY + wingY;
                double pz = cz - curve;

                float edgeFade = (float) Math.sin(t * Math.PI);
                float size = 0.15f * edgeFade * (1f + 0.2f * Math.abs(wingFlap));

                // огненная палитра — color index сдвинут к тёплым
                int col = (int) (t * 40 + wing * 0.5f + (side > 0 ? 0 : 30));

                renderParticle(stack, camera, camPos,
                        px, py, pz, col,
                        size, alphaPC, 0.85f * edgeFade);

                // суб-сегменты для сплошности
                if (s < WING_SEGMENTS - 1) {
                    float t2 = (float) (s + 1) / (WING_SEGMENTS - 1);
                    float span2 = baseR * (0.3f + t2 * 1.4f);
                    float wingY2 = (float) Math.sin(t2 * Math.PI * 0.8f) * height * 0.4f;
                    float curve2 = t2 * t2 * baseR * 0.3f;
                    float flapRad2 = (float) Math.toRadians(flapAngle * t2);
                    wingY2 += Math.sin(flapRad2) * span2 * 0.3f;

                    // середина
                    double mx = cx + side * (span + span2) * 0.5f;
                    double my = midY + (wingY + wingY2) * 0.5f;
                    double mz = cz - (curve + curve2) * 0.5f;

                    renderParticle(stack, camera, camPos,
                            mx, my, mz, col + 10,
                            size * 0.6f, alphaPC, 0.5f * edgeFade);
                }
            }
        }

        /* ═══════════════════════════════════════════════════
         *  LAYER 2 — Feathers (отлетающие перья)
         * ═══════════════════════════════════════════════════ */
        for (int i = 0; i < FEATHER_COUNT; i++) {
            float seed = i * 83.7f;
            float featherT = ((flame * 0.8f + seed) % 360f) / 360f;

            int side = (i % 2 == 0) ? 1 : -1;
            float wingT = 0.3f + 0.7f * ((float) i / FEATHER_COUNT);

            float span    = baseR * (0.3f + wingT * 1.4f);
            float originX = side * span;
            float originY = (float) Math.sin(wingT * Math.PI * 0.8f) * height * 0.4f;

            // перо отлетает от крыла
            float drift = featherT * baseR * 1.5f;
            float fallY = featherT * featherT * height * 0.5f;

            double px = cx + originX + side * drift * 0.5f;
            double py = midY + originY - fallY;
            double pz = cz + (float) Math.sin(Math.toRadians(seed + flame)) * 0.3f;

            float alpha = (1f - featherT) * 0.5f;
            float size  = 0.06f * (1f - featherT * 0.5f);
            int   col   = (int) (seed * 0.5f + flame * 0.3f + 20);

            if (alpha > 0.02f) {
                renderParticle(stack, camera, camPos,
                        px, py, pz, col, size, alphaPC, alpha);
            }
        }

        /* ═══════════════════════════════════════════════════
         *  LAYER 3 — Tail Fire Trail
         * ═══════════════════════════════════════════════════ */
        List<double[]> frameTail = new ArrayList<>();

        for (int i = 0; i < TAIL_PARTICLES; i++) {
            float t     = (float) i / TAIL_PARTICLES;
            float tailR = baseR * (0.3f - t * 0.25f);
            float angle = flame * 2f + i * 25f;
            float rad   = (float) Math.toRadians(angle);

            double tx = cx + Math.cos(rad) * tailR;
            double ty = cy - t * height * 0.6f;
            double tz = cz + Math.sin(rad) * tailR;

            float alpha = (1f - t) * 0.8f;
            float size  = 0.1f * (1f - t * 0.5f);
            int   col   = (int) (angle * 0.3f + 10);

            frameTail.add(new double[]{tx, ty, tz, col});
            renderParticle(stack, camera, camPos,
                    tx, ty, tz, col, size, alphaPC, alpha);
        }

        // tail trails
        fireTrails.addAll(frameTail);
        int maxTrails = TAIL_TRAIL_LEN * TAIL_PARTICLES;
        if (fireTrails.size() > maxTrails) {
            fireTrails.subList(0, fireTrails.size() - maxTrails).clear();
        }

        int tailFrames = fireTrails.size() / Math.max(1, TAIL_PARTICLES);
        for (int i = 0; i < TAIL_PARTICLES; i++) {
            for (int tf = 0; tf < tailFrames; tf++) {
                int idx = tf * TAIL_PARTICLES + i;
                if (idx >= fireTrails.size()) continue;
                double[] tp = fireTrails.get(idx);
                float progress = (float) (tf + 1) / (tailFrames + 1f);
                renderParticle(stack, camera, camPos,
                        tp[0], tp[1], tp[2], (int) tp[3],
                        Math.max(0.03f, 0.07f * progress),
                        alphaPC, progress * 0.25f);
            }
        }

        /* ═══════════════════════════════════════════════════
         *  LAYER 4 — Fire Vortex at feet
         * ═══════════════════════════════════════════════════ */
        for (int ring = 0; ring < 2; ring++) {
            float vortexR = baseR * (0.5f + ring * 0.3f);
            float vSpeed  = 2f - ring * 0.6f;

            for (int s = 0; s < VORTEX_SEGMENTS; s++) {
                float angle = flame * vSpeed + (float) s / VORTEX_SEGMENTS * 360f;
                float rad   = (float) Math.toRadians(angle);

                double vx = cx + Math.cos(rad) * vortexR;
                double vy = cy + 0.05f + ring * 0.08f
                        + Math.sin(Math.toRadians(flame * 3 + s * 20)) * 0.04f;
                double vz = cz + Math.sin(rad) * vortexR;

                float alpha = 0.45f - ring * 0.12f;
                int col = (int) (angle * 0.5f + ring * 40 + 5);

                renderParticle(stack, camera, camPos,
                        vx, vy, vz, col, 0.07f, alphaPC, alpha);
            }
        }

        /* ═══════════════════════════════════════════════════
         *  LAYER 5 — Rising Ash / Embers
         * ═══════════════════════════════════════════════════ */
        for (int i = 0; i < ASH_PARTICLES; i++) {
            float seed = i * 193.5f;
            float ashT = ((ash * 1.5f + seed) % 500f) / 500f;

            float ashR = baseR * (0.2f + 0.8f * (float) Math.abs(
                    Math.sin(Math.toRadians(seed))));
            float ashAngle = seed + ash * 0.3f;
            float ashRad   = (float) Math.toRadians(ashAngle);

            // поднимаются и раскачиваются
            float sway = (float) Math.sin(Math.toRadians(ash * 2 + seed)) * 0.15f;

            double ax = cx + Math.cos(ashRad) * ashR + sway;
            double ay = cy + ashT * height * 2f;
            double az = cz + Math.sin(ashRad) * ashR;

            float alpha = (1f - ashT) * 0.35f;
            float flicker = 0.5f + 0.5f * (float) Math.abs(
                    Math.sin(Math.toRadians(ash * 6 + seed * 2)));

            int col = (int) (seed * 0.3f + ash * 0.2f + 15);

            if (alpha > 0.02f) {
                renderParticle(stack, camera, camPos,
                        ax, ay, az, col,
                        0.04f * flicker, alphaPC, alpha * flicker);
            }
        }

        /* ═══════════════════════════════════════════════════
         *  LAYER 6 — Flame Halo above head
         * ═══════════════════════════════════════════════════ */
        float haloR = baseR * 0.35f;
        double haloY = cy + height + 0.2f;
        float haloPulse = 0.7f + 0.3f * (float) Math.sin(
                Math.toRadians(flame * 4));

        for (int s = 0; s < HALO_SEGMENTS; s++) {
            float angle = flame * 1.8f + (float) s / HALO_SEGMENTS * 360f;
            float rad   = (float) Math.toRadians(angle);

            float flameWave = (float) Math.sin(
                    Math.toRadians(angle * 3 + flame * 5)) * 0.06f;

            double hx = cx + Math.cos(rad) * haloR;
            double hy = haloY + flameWave;
            double hz = cz + Math.sin(rad) * haloR;

            int col = (int) (angle * 0.6f + 25);
            renderParticle(stack, camera, camPos,
                    hx, hy, hz, col,
                    0.1f * haloPulse, alphaPC, 0.8f);
        }

        /* ═══════ CLEANUP ═══════ */
        RenderSystem.enableCull();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA,
                GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderUtil.WORLD.endRender(stack);
    }

    private void renderParticle(MatrixStack stack, Camera camera,
                                Vec3d camPos,
                                double x, double y, double z,
                                int colorIndex,
                                float size, float globalAlpha,
                                float localAlpha) {
        float[] c = ColorUtil.normalize(
                ColorUtil.setAlpha(UIColors.gradient(colorIndex),
                        (int) (255 * globalAlpha)));
        float a = c[3] * localAlpha;
        if (a < 0.01f) return;

        stack.push();
        stack.translate(x - camPos.x, y - camPos.y, z - camPos.z);
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw() + 180f));
        stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-camera.getPitch() + 180f));

        RenderSystem.setShaderTexture(0, FileUtil.getImage("particles/glow"));
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        Matrix4f matrix = stack.peek().getPositionMatrix();

        float bS = size * 2.8f, bA = a * 0.12f;
        BufferBuilder buf = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buf.vertex(matrix, -bS, bS, 0).texture(0f, 1f).color(c[0], c[1], c[2], bA);
        buf.vertex(matrix, bS, bS, 0).texture(1f, 1f).color(c[0], c[1], c[2], bA);
        buf.vertex(matrix, bS, -bS, 0).texture(1f, 0f).color(c[0], c[1], c[2], bA);
        buf.vertex(matrix, -bS, -bS, 0).texture(0f, 0f).color(c[0], c[1], c[2], bA);
        BufferRenderer.drawWithGlobalProgram(buf.end());

        buf = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buf.vertex(matrix, -size, size, 0).texture(0f, 1f).color(c[0], c[1], c[2], a);
        buf.vertex(matrix, size, size, 0).texture(1f, 1f).color(c[0], c[1], c[2], a);
        buf.vertex(matrix, size, -size, 0).texture(1f, 0f).color(c[0], c[1], c[2], a);
        buf.vertex(matrix, -size, -size, 0).texture(0f, 0f).color(c[0], c[1], c[2], a);
        BufferRenderer.drawWithGlobalProgram(buf.end());

        stack.pop();
    }

    @Override
    public void updateTarget() {
        super.updateTarget();
        if (currentTarget == null) fireTrails.clear();
    }
}