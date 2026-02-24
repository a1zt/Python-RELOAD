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
 * TargetEspSuper — «Quantum Atom»
 *
 * ┌────────────────────────────────────────────────────┐
 * │  Layer 1 — 3 гироскопических орбитальных кольца    │
 * │            с хвостами-трейлами и bloom              │
 * │  Layer 2 — восходящая спираль из искр               │
 * │  Layer 3 — расходящиеся импульсные кольца у ног     │
 * │  Layer 4 — парящая корона / нимб над головой        │
 * │  Layer 5 — центральный энерго-столб                 │
 * └────────────────────────────────────────────────────┘
 */
public class TargetEspSuper extends TargetEspMode {

    /* ═══════════════════════════════════════════════════
     *  TRAIL STORAGE
     * ═══════════════════════════════════════════════════ */
    private final List<double[]> trailPositions = new ArrayList<>();

    /* ═══════════════════════════════════════════════════
     *  ANIMATION TIMERS
     * ═══════════════════════════════════════════════════ */
    private float rotation      = 0f, prevRotation      = 0f;
    private float verticalPhase = 0f, prevVerticalPhase = 0f;
    private float pulsePhase    = 0f, prevPulsePhase    = 0f;

    /* ═══════════════════════════════════════════════════
     *  CONSTANTS
     * ═══════════════════════════════════════════════════ */
    private static final int RING_COUNT         = 3;
    private static final int PARTICLES_PER_RING = 5;
    private static final int TOTAL_RING_PARTS   = RING_COUNT * PARTICLES_PER_RING;
    private static final int TRAIL_LENGTH       = 14;

    private static final int SPIRAL_PARTICLES   = 12;
    private static final int PULSE_SEGMENTS     = 20;
    private static final int CROWN_SEGMENTS     = 14;
    private static final int BEAM_PARTICLES     = 10;

    /* ═══════════════════════════════════════════════════
     *  UPDATE
     * ═══════════════════════════════════════════════════ */

    @Override
    public void onUpdate() {
        updateTarget();

        prevRotation      = rotation;
        prevVerticalPhase = verticalPhase;
        prevPulsePhase    = pulsePhase;

        rotation      += 10f;
        verticalPhase += 5f;
        pulsePhase    += 3f;
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

        float height  = currentTarget.getHeight();
        float width   = currentTarget.getWidth();
        double midY   = cy + height / 2.0;

        float rot    = MathUtil.interpolate(prevRotation, rotation);
        float vPhase = MathUtil.interpolate(prevVerticalPhase, verticalPhase);
        float pPhase = MathUtil.interpolate(prevPulsePhase, pulsePhase);

        // ── «дыхание» радиуса ─────────────────────────────────
        float breathe    = 1f + 0.07f * (float) Math.sin(Math.toRadians(pPhase * 1.5));
        float baseRadius = width * 1.3f * anim * breathe;

        /* ═══════════════════════════════════════════════════
         *  LAYER 1 — Orbital Rings + Trails
         * ═══════════════════════════════════════════════════ */
        List<double[]> framePositions = new ArrayList<>();

        float[] ringTilts   = {  0f,   55f,  -55f };
        float[] ringSpeeds  = {  1f, -0.75f, 1.15f };
        int[]   ringColors  = {  0,   100,   220  };

        for (int ring = 0; ring < RING_COUNT; ring++) {
            float tiltRad = (float) Math.toRadians(ringTilts[ring]);
            float cosT = (float) Math.cos(tiltRad);
            float sinT = (float) Math.sin(tiltRad);

            // медленно вращаем ось наклона вокруг Y
            float yRot = (float) Math.toRadians(rot * 0.2f + ring * 40f);
            float cosY = (float) Math.cos(yRot);
            float sinY = (float) Math.sin(yRot);

            for (int p = 0; p < PARTICLES_PER_RING; p++) {
                float angle = rot * ringSpeeds[ring]
                        + (p * 360f / PARTICLES_PER_RING);
                float rad = (float) Math.toRadians(angle);

                // точка на окружности (local)
                float lx = (float) Math.cos(rad) * baseRadius;
                float ly = (float) Math.sin(rad) * baseRadius;

                // наклон вокруг X
                float ty = ly * cosT;
                float tz = ly * sinT;

                // поворот вокруг Y
                float fx = lx * cosY - tz * sinY;
                float fz = lx * sinY + tz * cosY;

                double px = cx + fx;
                double py = midY + ty;
                double pz = cz + fz;

                int colorIdx = ringColors[ring] + (int) (angle * 0.5f);
                framePositions.add(new double[]{ px, py, pz, colorIdx });
            }
        }

        // ── trail bookkeeping ────────────────────────────
        trailPositions.addAll(framePositions);
        int maxEntries = TRAIL_LENGTH * TOTAL_RING_PARTS;
        if (trailPositions.size() > maxEntries) {
            trailPositions.subList(0, trailPositions.size() - maxEntries).clear();
        }

        // ── draw ring particles + trails ─────────────────
        float mainSize = 0.22f;

        for (int i = 0; i < TOTAL_RING_PARTS; i++) {
            double[] pos = framePositions.get(i);

            // головная частица
            renderParticle(stack, camera, camPos,
                    pos[0], pos[1], pos[2],
                    (int) pos[3], mainSize, alphaPC, 1f);

            // хвост
            int frames = trailPositions.size() / TOTAL_RING_PARTS;
            for (int t = 0; t < frames; t++) {
                int idx = t * TOTAL_RING_PARTS + i;
                if (idx >= trailPositions.size()) continue;

                double[] tp       = trailPositions.get(idx);
                float    progress = (float) (t + 1) / (frames + 1f);
                float    tSize    = Math.max(mainSize * 0.2f, mainSize * progress);
                float    tAlpha   = progress * 0.55f;

                renderParticle(stack, camera, camPos,
                        tp[0], tp[1], tp[2],
                        (int) tp[3], tSize, alphaPC, tAlpha);
            }
        }

        /* ═══════════════════════════════════════════════════
         *  LAYER 2 — Rising Spiral Sparkles
         * ═══════════════════════════════════════════════════ */
        for (int i = 0; i < SPIRAL_PARTICLES; i++) {
            float t           = (float) i / SPIRAL_PARTICLES;
            float spiralAngle = vPhase * 1.5f + t * 540f;
            float spiralRad   = (float) Math.toRadians(spiralAngle);
            float spiralR     = baseRadius * 0.55f * (1f - t * 0.4f);

            float wobble = (float) Math.sin(
                    Math.toRadians(vPhase * 3f + i * 40f)) * 0.03f;

            double sx = cx + Math.cos(spiralRad) * (spiralR + wobble);
            double sy = cy + t * height * 1.15f;
            double sz = cz + Math.sin(spiralRad) * (spiralR + wobble);

            float sparkAlpha = (1f - t * t) * 0.7f;
            float sparkSize  = 0.12f * (1f - t * 0.4f);
            int   colorIdx   = (int) (spiralAngle * 0.3f + 50);

            renderParticle(stack, camera, camPos,
                    sx, sy, sz, colorIdx, sparkSize, alphaPC, sparkAlpha);
        }

        /* ═══════════════════════════════════════════════════
         *  LAYER 3 — Ground Pulse Waves
         * ═══════════════════════════════════════════════════ */
        float pulseCycle = pPhase * 2f;

        for (int wave = 0; wave < 2; wave++) {
            float phase      = pulseCycle + wave * 180f;
            float normalized = ((phase % 360f) + 360f) % 360f / 360f;
            float waveR      = baseRadius * (0.8f + normalized * 0.8f);
            float waveAlpha  = (1f - normalized) * 0.4f;

            if (waveAlpha < 0.02f) continue;

            for (int s = 0; s < PULSE_SEGMENTS; s++) {
                float angle = (float) s / PULSE_SEGMENTS * 360f;
                float rad   = (float) Math.toRadians(angle);

                double px = cx + Math.cos(rad) * waveR;
                double py = cy + 0.05;
                double pz = cz + Math.sin(rad) * waveR;

                int colorIdx = (int) (angle + rot * 0.3f);
                renderParticle(stack, camera, camPos,
                        px, py, pz, colorIdx, 0.07f, alphaPC, waveAlpha);
            }
        }

        /* ═══════════════════════════════════════════════════
         *  LAYER 4 — Crown / Halo
         * ═══════════════════════════════════════════════════ */
        float crownR     = baseRadius * 0.35f;
        double crownY    = cy + height + 0.18f;
        float crownPulse = 0.7f + 0.3f
                * (float) Math.sin(Math.toRadians(pPhase * 3f));

        for (int i = 0; i < CROWN_SEGMENTS; i++) {
            float angle = rot * 1.4f
                    + (float) i / CROWN_SEGMENTS * 360f;
            float rad   = (float) Math.toRadians(angle);

            float vertOff = (float) Math.sin(
                    Math.toRadians(angle * 3f + pPhase * 2f)) * 0.05f;

            double px = cx + Math.cos(rad) * crownR;
            double py = crownY + vertOff;
            double pz = cz + Math.sin(rad) * crownR;

            int colorIdx = (int) (angle * 0.8f + 180);
            renderParticle(stack, camera, camPos,
                    px, py, pz, colorIdx,
                    0.09f * crownPulse, alphaPC, 0.85f);
        }

        /* ═══════════════════════════════════════════════════
         *  LAYER 5 — Central Energy Beam
         * ═══════════════════════════════════════════════════ */
        float beamPulse = 0.35f + 0.25f
                * (float) Math.sin(Math.toRadians(pPhase * 2.5f));

        for (int i = 0; i < BEAM_PARTICLES; i++) {
            float t  = (float) i / (BEAM_PARTICLES - 1);
            double by = cy + t * height;

            // альфа ярче в центре, тише к краям
            float edgeFade = 1f - Math.abs(t - 0.5f) * 1.8f;
            edgeFade = Math.max(0.1f, edgeFade);

            float beamWobble = (float) Math.sin(
                    Math.toRadians(vPhase * 4f + i * 50f)) * 0.015f;

            int colorIdx = (int) (t * 120 + rot * 0.5f);
            renderParticle(stack, camera, camPos,
                    cx + beamWobble, by, cz + beamWobble,
                    colorIdx, 0.06f, alphaPC,
                    beamPulse * edgeFade);
        }

        /* ═══════════════════════════════════════════════════
         *  CLEANUP
         * ═══════════════════════════════════════════════════ */
        RenderSystem.enableCull();
        RenderSystem.blendFunc(
                GlStateManager.SrcFactor.SRC_ALPHA,
                GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderUtil.WORLD.endRender(stack);
    }

    /* ═══════════════════════════════════════════════════
     *  PARTICLE RENDERER  (billboard + bloom)
     * ═══════════════════════════════════════════════════ */

    private void renderParticle(MatrixStack stack, Camera camera,
                                Vec3d camPos,
                                double x, double y, double z,
                                int colorIndex,
                                float size, float globalAlpha,
                                float localAlpha) {

        float[] c = ColorUtil.normalize(
                ColorUtil.setAlpha(
                        UIColors.gradient(colorIndex),
                        (int) (255 * globalAlpha)));
        float a = c[3] * localAlpha;
        if (a < 0.01f) return;               // невидимо — пропускаем

        stack.push();
        stack.translate(
                x - camPos.x,
                y - camPos.y,
                z - camPos.z);
        stack.multiply(RotationAxis.POSITIVE_Y
                .rotationDegrees(-camera.getYaw() + 180f));
        stack.multiply(RotationAxis.POSITIVE_X
                .rotationDegrees(-camera.getPitch() + 180f));

        RenderSystem.setShaderTexture(0,
                FileUtil.getImage("particles/glow"));
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        Matrix4f matrix = stack.peek().getPositionMatrix();

        // ── bloom (мягкое свечение) ──────────────────────
        float bSize  = size * 2.8f;
        float bAlpha = a * 0.12f;

        BufferBuilder buf = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.QUADS,
                        VertexFormats.POSITION_TEXTURE_COLOR);
        buf.vertex(matrix, -bSize,  bSize, 0)
                .texture(0f, 1f).color(c[0], c[1], c[2], bAlpha);
        buf.vertex(matrix,  bSize,  bSize, 0)
                .texture(1f, 1f).color(c[0], c[1], c[2], bAlpha);
        buf.vertex(matrix,  bSize, -bSize, 0)
                .texture(1f, 0f).color(c[0], c[1], c[2], bAlpha);
        buf.vertex(matrix, -bSize, -bSize, 0)
                .texture(0f, 0f).color(c[0], c[1], c[2], bAlpha);
        BufferRenderer.drawWithGlobalProgram(buf.end());

        // ── основная частица ─────────────────────────────
        buf = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.QUADS,
                        VertexFormats.POSITION_TEXTURE_COLOR);
        buf.vertex(matrix, -size,  size, 0)
                .texture(0f, 1f).color(c[0], c[1], c[2], a);
        buf.vertex(matrix,  size,  size, 0)
                .texture(1f, 1f).color(c[0], c[1], c[2], a);
        buf.vertex(matrix,  size, -size, 0)
                .texture(1f, 0f).color(c[0], c[1], c[2], a);
        buf.vertex(matrix, -size, -size, 0)
                .texture(0f, 0f).color(c[0], c[1], c[2], a);
        BufferRenderer.drawWithGlobalProgram(buf.end());

        stack.pop();
    }

    /* ═══════════════════════════════════════════════════
     *  TARGET RESET
     * ═══════════════════════════════════════════════════ */

    @Override
    public void updateTarget() {
        super.updateTarget();
        if (currentTarget == null) {
            trailPositions.clear();
        }
    }
}