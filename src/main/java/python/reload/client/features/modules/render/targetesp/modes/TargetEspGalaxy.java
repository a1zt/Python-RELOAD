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
 * TargetEspGalaxy — «Spiral Galaxy»
 *
 * ┌──────────────────────────────────────────────┐
 * │  Layer 1 — 3 спиральных рукава галактики     │
 * │  Layer 2 — аккреционный диск (горизонталь)    │
 * │  Layer 3 — центральное ядро (пульсирующее)    │
 * │  Layer 4 — выбросы из полюсов (джеты)         │
 * │  Layer 5 — орбитальные «звёзды»               │
 * │  Layer 6 — пылевое облако / туманность         │
 * └──────────────────────────────────────────────┘
 */
public class TargetEspGalaxy extends TargetEspMode {

    /* ═══════════════════════════════════════════════════
     *  TIMERS
     * ═══════════════════════════════════════════════════ */
    private float rotation     = 0f, prevRotation     = 0f;
    private float pulseTime    = 0f, prevPulseTime    = 0f;
    private float jetTime      = 0f, prevJetTime      = 0f;

    /* ═══════════════════════════════════════════════════
     *  TRAIL
     * ═══════════════════════════════════════════════════ */
    private final List<double[]> starTrails = new ArrayList<>();

    /* ═══════════════════════════════════════════════════
     *  CONSTANTS
     * ═══════════════════════════════════════════════════ */
    private static final int   SPIRAL_ARMS         = 3;
    private static final int   PARTICLES_PER_ARM   = 18;
    private static final float SPIRAL_TIGHTNESS    = 2.5f;
    private static final int   ACCRETION_SEGMENTS  = 28;
    private static final int   CORE_PARTICLES      = 8;
    private static final int   JET_PARTICLES       = 10;
    private static final int   ORBITING_STARS      = 6;
    private static final int   STAR_TRAIL_LENGTH   = 10;
    private static final int   DUST_PARTICLES      = 20;

    /* ═══════════════════════════════════════════════════
     *  UPDATE
     * ═══════════════════════════════════════════════════ */

    @Override
    public void onUpdate() {
        updateTarget();

        prevRotation  = rotation;
        prevPulseTime = pulseTime;
        prevJetTime   = jetTime;

        rotation  += 8f;
        pulseTime += 6f;
        jetTime   += 12f;
    }

    /* ═══════════════════════════════════════════════════
     *  RENDER
     * ═══════════════════════════════════════════════════ */

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
        double midY  = cy + height / 2.0;

        float rot   = MathUtil.interpolate(prevRotation, rotation);
        float pulse = MathUtil.interpolate(prevPulseTime, pulseTime);
        float jet   = MathUtil.interpolate(prevJetTime, jetTime);

        float breathe    = 1f + 0.05f * (float) Math.sin(Math.toRadians(pulse * 1.2));
        float baseRadius = width * 1.6f * anim * breathe;

        /* ═══════════════════════════════════════════════════
         *  LAYER 1 — Spiral Arms
         * ═══════════════════════════════════════════════════ */
        for (int arm = 0; arm < SPIRAL_ARMS; arm++) {
            float armOffset = arm * (360f / SPIRAL_ARMS);

            for (int i = 0; i < PARTICLES_PER_ARM; i++) {
                float t = (float) i / PARTICLES_PER_ARM;

                // логарифмическая спираль
                float r     = baseRadius * (0.2f + t * 0.9f);
                float angle = armOffset + rot + t * 360f * SPIRAL_TIGHTNESS;
                float rad   = (float) Math.toRadians(angle);

                // дисковая плоскость с лёгким наклоном
                float tiltAngle = (float) Math.toRadians(15f);
                float localX = (float) Math.cos(rad) * r;
                float localZ = (float) Math.sin(rad) * r;
                float localY = localZ * (float) Math.sin(tiltAngle);
                localZ *= (float) Math.cos(tiltAngle);

                // вертикальное колебание
                localY += (float) Math.sin(Math.toRadians(pulse * 2 + i * 25)) * 0.04f;

                double px = cx + localX;
                double py = midY + localY;
                double pz = cz + localZ;

                float size  = 0.13f * (0.5f + t);
                float alpha = (0.3f + t * 0.7f) * 0.85f;
                int   col   = (int) (armOffset + angle * 0.4f);

                renderParticle(stack, camera, camPos,
                        px, py, pz, col, size, alphaPC, alpha);
            }
        }

        /* ═══════════════════════════════════════════════════
         *  LAYER 2 — Accretion Disk
         * ═══════════════════════════════════════════════════ */
        for (int ring = 0; ring < 3; ring++) {
            float ringR = baseRadius * (0.3f + ring * 0.15f);

            for (int s = 0; s < ACCRETION_SEGMENTS; s++) {
                float angle = rot * (1.2f + ring * 0.3f)
                        + (float) s / ACCRETION_SEGMENTS * 360f;
                float rad = (float) Math.toRadians(angle);

                float tilt  = (float) Math.toRadians(15f);
                float lx    = (float) Math.cos(rad) * ringR;
                float lz    = (float) Math.sin(rad) * ringR;
                float ly    = lz * (float) Math.sin(tilt);
                lz *= (float) Math.cos(tilt);

                double px = cx + lx;
                double py = midY + ly;
                double pz = cz + lz;

                float alpha = 0.25f - ring * 0.06f;
                int   col   = (int) (angle * 0.6f + ring * 80);

                renderParticle(stack, camera, camPos,
                        px, py, pz, col, 0.06f, alphaPC, alpha);
            }
        }

        /* ═══════════════════════════════════════════════════
         *  LAYER 3 — Core (пульсирующее ядро)
         * ═══════════════════════════════════════════════════ */
        float corePulse = 0.8f + 0.4f * (float) Math.sin(Math.toRadians(pulse * 4));
        float coreSize  = 0.25f * corePulse;

        // центральная яркая точка
        renderParticle(stack, camera, camPos,
                cx, midY, cz, (int) (rot * 0.5f),
                coreSize, alphaPC, 0.95f);

        // венец вокруг ядра
        for (int i = 0; i < CORE_PARTICLES; i++) {
            float angle = rot * 3f + i * (360f / CORE_PARTICLES);
            float rad   = (float) Math.toRadians(angle);
            float coreR = baseRadius * 0.12f * corePulse;

            double px = cx + Math.cos(rad) * coreR;
            double py = midY + Math.sin(Math.toRadians(
                    pulse * 5 + i * 60)) * 0.06f;
            double pz = cz + Math.sin(rad) * coreR;

            int col = (int) (angle * 0.8f + 40);
            renderParticle(stack, camera, camPos,
                    px, py, pz, col, 0.1f * corePulse,
                    alphaPC, 0.75f);
        }

        /* ═══════════════════════════════════════════════════
         *  LAYER 4 — Polar Jets
         * ═══════════════════════════════════════════════════ */
        for (int pole = -1; pole <= 1; pole += 2) {
            for (int i = 0; i < JET_PARTICLES; i++) {
                float t      = (float) i / JET_PARTICLES;
                float jetH   = height * 0.7f * t * anim;
                float spread = t * 0.08f;

                float wobbleX = (float) Math.sin(
                        Math.toRadians(jet * 3 + i * 45)) * spread;
                float wobbleZ = (float) Math.cos(
                        Math.toRadians(jet * 3 + i * 45 + 90)) * spread;

                double px = cx + wobbleX;
                double py = midY + pole * (0.15f + jetH);
                double pz = cz + wobbleZ;

                float alpha = (1f - t) * 0.65f;
                float size  = 0.09f * (1f - t * 0.5f);
                int   col   = (int) (t * 100 + pole * 60 + jet * 0.5f);

                renderParticle(stack, camera, camPos,
                        px, py, pz, col, size, alphaPC, alpha);
            }
        }

        /* ═══════════════════════════════════════════════════
         *  LAYER 5 — Orbiting Stars + Trails
         * ═══════════════════════════════════════════════════ */
        List<double[]> frameStars = new ArrayList<>();

        for (int i = 0; i < ORBITING_STARS; i++) {
            float orbitR     = baseRadius * (0.6f + 0.4f * ((float) i / ORBITING_STARS));
            float speed      = (i % 2 == 0) ? 1f : -0.7f;
            float angle      = rot * speed * 0.6f + i * (360f / ORBITING_STARS);
            float rad        = (float) Math.toRadians(angle);
            float vertOffset = (float) Math.sin(
                    Math.toRadians(pulse * 1.5f + i * 70)) * 0.15f;

            double sx = cx + Math.cos(rad) * orbitR;
            double sy = midY + vertOffset;
            double sz = cz + Math.sin(rad) * orbitR;

            int col = (int) (angle + i * 50);
            frameStars.add(new double[]{sx, sy, sz, col});

            renderParticle(stack, camera, camPos,
                    sx, sy, sz, col, 0.16f, alphaPC, 0.9f);
        }

        // трейлы звёзд
        starTrails.addAll(frameStars);
        int maxTrails = STAR_TRAIL_LENGTH * ORBITING_STARS;
        if (starTrails.size() > maxTrails) {
            starTrails.subList(0, starTrails.size() - maxTrails).clear();
        }

        int trailFrames = starTrails.size() / ORBITING_STARS;
        for (int i = 0; i < ORBITING_STARS; i++) {
            for (int t = 0; t < trailFrames; t++) {
                int idx = t * ORBITING_STARS + i;
                if (idx >= starTrails.size()) continue;

                double[] tp       = starTrails.get(idx);
                float    progress = (float) (t + 1) / (trailFrames + 1f);
                float    tSize    = Math.max(0.04f, 0.12f * progress);
                float    tAlpha   = progress * 0.4f;

                renderParticle(stack, camera, camPos,
                        tp[0], tp[1], tp[2],
                        (int) tp[3], tSize, alphaPC, tAlpha);
            }
        }

        /* ═══════════════════════════════════════════════════
         *  LAYER 6 — Dust / Nebula
         * ═══════════════════════════════════════════════════ */
        for (int i = 0; i < DUST_PARTICLES; i++) {
            float seed  = i * 137.508f; // золотой угол
            float dustR = baseRadius * (0.4f + 0.6f
                    * (float) Math.abs(Math.sin(Math.toRadians(seed))));
            float angle = seed + rot * 0.3f;
            float rad   = (float) Math.toRadians(angle);

            float tilt  = (float) Math.toRadians(15f);
            float lx    = (float) Math.cos(rad) * dustR;
            float lz    = (float) Math.sin(rad) * dustR;
            float ly    = lz * (float) Math.sin(tilt);
            lz *= (float) Math.cos(tilt);

            // мерцание
            float flicker = 0.5f + 0.5f * (float) Math.sin(
                    Math.toRadians(pulse * 3 + seed * 2));

            double px = cx + lx;
            double py = midY + ly + (float) Math.sin(
                    Math.toRadians(pulse + seed)) * 0.05f;
            double pz = cz + lz;

            int col = (int) (seed * 0.5f + rot * 0.2f);
            renderParticle(stack, camera, camPos,
                    px, py, pz, col,
                    0.04f + 0.03f * flicker,
                    alphaPC, 0.15f * flicker);
        }

        /* ═══════════════════════════════════════════════════
         *  CLEANUP
         * ═══════════════════════════════════════════════════ */
        RenderSystem.enableCull();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA,
                GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderUtil.WORLD.endRender(stack);
    }

    /* ═══════════════════════════════════════════════════
     *  PARTICLE RENDERER
     * ═══════════════════════════════════════════════════ */

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
        stack.multiply(RotationAxis.POSITIVE_Y
                .rotationDegrees(-camera.getYaw() + 180f));
        stack.multiply(RotationAxis.POSITIVE_X
                .rotationDegrees(-camera.getPitch() + 180f));

        RenderSystem.setShaderTexture(0, FileUtil.getImage("particles/glow"));
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        Matrix4f matrix = stack.peek().getPositionMatrix();

        // bloom
        float bSize  = size * 3f;
        float bAlpha = a * 0.1f;
        BufferBuilder buf = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buf.vertex(matrix, -bSize,  bSize, 0).texture(0f, 1f).color(c[0], c[1], c[2], bAlpha);
        buf.vertex(matrix,  bSize,  bSize, 0).texture(1f, 1f).color(c[0], c[1], c[2], bAlpha);
        buf.vertex(matrix,  bSize, -bSize, 0).texture(1f, 0f).color(c[0], c[1], c[2], bAlpha);
        buf.vertex(matrix, -bSize, -bSize, 0).texture(0f, 0f).color(c[0], c[1], c[2], bAlpha);
        BufferRenderer.drawWithGlobalProgram(buf.end());

        // core
        buf = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buf.vertex(matrix, -size,  size, 0).texture(0f, 1f).color(c[0], c[1], c[2], a);
        buf.vertex(matrix,  size,  size, 0).texture(1f, 1f).color(c[0], c[1], c[2], a);
        buf.vertex(matrix,  size, -size, 0).texture(1f, 0f).color(c[0], c[1], c[2], a);
        buf.vertex(matrix, -size, -size, 0).texture(0f, 0f).color(c[0], c[1], c[2], a);
        BufferRenderer.drawWithGlobalProgram(buf.end());

        stack.pop();
    }

    @Override
    public void updateTarget() {
        super.updateTarget();
        if (currentTarget == null) {
            starTrails.clear();
        }
    }
}