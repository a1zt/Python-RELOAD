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

import java.util.Random;

/**
 * TargetEspPlasma — «Tesla Coil / Plasma Field»
 *
 * ┌──────────────────────────────────────────────────────┐
 * │  Layer 1 — Электрические дуги (lightning arcs)       │
 * │  Layer 2 — Плазменное поле (particle shell)          │
 * │  Layer 3 — Электростатические узлы (nodes)           │
 * │  Layer 4 — Искровые выбросы (spark bursts)           │
 * │  Layer 5 — Заземляющие линии (ground discharge)      │
 * │  Layer 6 — Электромагнитные кольца (EM rings)        │
 * └──────────────────────────────────────────────────────┘
 */
public class TargetEspPlasma extends TargetEspMode {

    /* ═══════════════════════════════════════════════════
     *  TIMERS
     * ═══════════════════════════════════════════════════ */
    private float time     = 0f, prevTime     = 0f;
    private float arcTime  = 0f, prevArcTime  = 0f;
    private float emPhase  = 0f, prevEmPhase  = 0f;

    /* ═══════════════════════════════════════════════════
     *  RANDOMNESS (deterministic per frame)
     * ═══════════════════════════════════════════════════ */
    private final Random rng = new Random();
    private long frameSeed = 0;

    /* ═══════════════════════════════════════════════════
     *  CONSTANTS
     * ═══════════════════════════════════════════════════ */
    private static final int ARC_COUNT      = 5;
    private static final int ARC_SEGMENTS   = 8;
    private static final int SHELL_LAYERS   = 2;
    private static final int SHELL_POINTS   = 16;
    private static final int NODE_COUNT     = 6;
    private static final int SPARK_BURSTS   = 3;
    private static final int SPARKS_PER_B   = 6;
    private static final int GROUND_LINES   = 4;
    private static final int GROUND_SEGS    = 6;
    private static final int EM_RING_COUNT  = 2;
    private static final int EM_SEGMENTS    = 24;

    /* ═══════════════════════════════════════════════════
     *  UPDATE
     * ═══════════════════════════════════════════════════ */

    @Override
    public void onUpdate() {
        updateTarget();

        prevTime    = time;
        prevArcTime = arcTime;
        prevEmPhase = emPhase;

        time    += 7f;
        arcTime += 20f;   // молнии обновляются быстро
        emPhase += 5f;
        frameSeed++;
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

        float t      = MathUtil.interpolate(prevTime, time);
        float arc    = MathUtil.interpolate(prevArcTime, arcTime);
        float em     = MathUtil.interpolate(prevEmPhase, emPhase);

        float baseR  = width * 1.4f * anim;

        /* ═══════════════════════════════════════════════════
         *  LAYER 1 — Lightning Arcs
         *  Зигзагообразные дуги между случайными точками
         *  на поверхности «сферы» вокруг цели
         * ═══════════════════════════════════════════════════ */
        rng.setSeed(frameSeed / 3);  // меняется каждые 3 кадра → мерцание

        for (int a = 0; a < ARC_COUNT; a++) {
            // стартовая и конечная точки на сфере
            float startTheta = rng.nextFloat() * 360f;
            float startPhi   = -60f + rng.nextFloat() * 120f;
            float endTheta   = rng.nextFloat() * 360f;
            float endPhi     = -60f + rng.nextFloat() * 120f;

            double[] start = spherePoint(cx, midY, cz, baseR * 0.85f,
                    startTheta, startPhi);
            double[] end   = spherePoint(cx, midY, cz, baseR * 0.85f,
                    endTheta, endPhi);

            // рисуем зигзагообразный путь между ними
            double prevPx = start[0], prevPy = start[1], prevPz = start[2];

            for (int s = 1; s <= ARC_SEGMENTS; s++) {
                float frac = (float) s / ARC_SEGMENTS;

                double lerpX = start[0] + (end[0] - start[0]) * frac;
                double lerpY = start[1] + (end[1] - start[1]) * frac;
                double lerpZ = start[2] + (end[2] - start[2]) * frac;

                // зигзаг-отклонение (кроме конечных точек)
                if (s < ARC_SEGMENTS) {
                    float jitter = baseR * 0.25f;
                    lerpX += (rng.nextFloat() - 0.5f) * jitter;
                    lerpY += (rng.nextFloat() - 0.5f) * jitter * 0.6f;
                    lerpZ += (rng.nextFloat() - 0.5f) * jitter;
                }

                // рисуем от prev до lerp как серию частиц
                int subSteps = 3;
                for (int ss = 0; ss <= subSteps; ss++) {
                    float sf  = (float) ss / subSteps;
                    double px = prevPx + (lerpX - prevPx) * sf;
                    double py = prevPy + (lerpY - prevPy) * sf;
                    double pz = prevPz + (lerpZ - prevPz) * sf;

                    float intensity = 0.6f + 0.4f * (float) Math.sin(
                            Math.toRadians(arc * 8 + a * 90 + s * 40));
                    int col = (int) (startTheta + s * 15 + a * 60);

                    renderParticle(stack, camera, camPos,
                            px, py, pz, col,
                            0.08f * intensity, alphaPC,
                            0.85f * intensity);
                }

                prevPx = lerpX;
                prevPy = lerpY;
                prevPz = lerpZ;
            }
        }

        /* ═══════════════════════════════════════════════════
         *  LAYER 2 — Plasma Shell
         *  Полупрозрачная оболочка из мерцающих частиц
         * ═══════════════════════════════════════════════════ */
        for (int layer = 0; layer < SHELL_LAYERS; layer++) {
            float shellR = baseR * (0.9f + layer * 0.2f);

            for (int i = 0; i < SHELL_POINTS; i++) {
                float seed  = i * 137.508f + layer * 63f;  // золотой угол
                float theta = seed + t * 0.4f * (layer % 2 == 0 ? 1 : -1);
                float phi   = (float) Math.toDegrees(
                        Math.asin(2f * ((float) i / SHELL_POINTS) - 1f));

                double[] pos = spherePoint(cx, midY, cz, shellR, theta, phi);

                // мерцание
                float flicker = 0.4f + 0.6f * (float) Math.abs(
                        Math.sin(Math.toRadians(t * 5 + seed * 3)));

                int col = (int) (seed + t * 0.3f);
                renderParticle(stack, camera, camPos,
                        pos[0], pos[1], pos[2], col,
                        0.05f + 0.03f * flicker,
                        alphaPC, 0.2f * flicker);
            }
        }

        /* ═══════════════════════════════════════════════════
         *  LAYER 3 — Electrostatic Nodes
         *  Яркие узлы, которые являются «катодами»
         * ═══════════════════════════════════════════════════ */
        for (int i = 0; i < NODE_COUNT; i++) {
            float angle  = t * 0.6f + i * (360f / NODE_COUNT);
            float phi    = (float) Math.sin(
                    Math.toRadians(t * 0.8f + i * 55)) * 50f;
            float nodeR  = baseR * 0.8f;

            double[] pos = spherePoint(cx, midY, cz, nodeR, angle, phi);

            float pulse = 0.7f + 0.3f * (float) Math.sin(
                    Math.toRadians(t * 6 + i * 72));

            int col = (int) (angle + 180);
            // яркое ядро узла
            renderParticle(stack, camera, camPos,
                    pos[0], pos[1], pos[2], col,
                    0.18f * pulse, alphaPC, 0.9f);
            // ореол узла
            renderParticle(stack, camera, camPos,
                    pos[0], pos[1], pos[2], col,
                    0.35f * pulse, alphaPC, 0.15f);
        }

        /* ═══════════════════════════════════════════════════
         *  LAYER 4 — Spark Bursts
         *  Случайные выбросы искр из узлов
         * ═══════════════════════════════════════════════════ */
        rng.setSeed(frameSeed / 4 + 777);

        for (int b = 0; b < SPARK_BURSTS; b++) {
            int sourceNode = rng.nextInt(NODE_COUNT);
            float nAngle = t * 0.6f + sourceNode * (360f / NODE_COUNT);
            float nPhi   = (float) Math.sin(
                    Math.toRadians(t * 0.8f + sourceNode * 55)) * 50f;
            double[] origin = spherePoint(cx, midY, cz,
                    baseR * 0.8f, nAngle, nPhi);

            for (int s = 0; s < SPARKS_PER_B; s++) {
                float sparkDist  = baseR * (0.1f + rng.nextFloat() * 0.4f);
                float sparkTheta = rng.nextFloat() * 360f;
                float sparkPhi   = -80f + rng.nextFloat() * 160f;

                double[] sparkEnd = spherePoint(
                        origin[0], origin[1], origin[2],
                        sparkDist, sparkTheta, sparkPhi);

                float sparkAlpha = 0.3f + rng.nextFloat() * 0.5f;
                int   col        = (int) (nAngle + sparkTheta * 0.3f);

                renderParticle(stack, camera, camPos,
                        sparkEnd[0], sparkEnd[1], sparkEnd[2],
                        col, 0.06f, alphaPC, sparkAlpha);
            }
        }

        /* ═══════════════════════════════════════════════════
         *  LAYER 5 — Ground Discharge Lines
         *  «Заземление» — дуги вниз от нижних узлов
         * ═══════════════════════════════════════════════════ */
        rng.setSeed(frameSeed / 5 + 1337);

        for (int g = 0; g < GROUND_LINES; g++) {
            float gAngle = t * 0.4f + g * (360f / GROUND_LINES);
            float gRad   = (float) Math.toRadians(gAngle);

            double startX = cx + Math.cos(gRad) * baseR * 0.5f;
            double startY = cy + 0.1f;
            double startZ = cz + Math.sin(gRad) * baseR * 0.5f;

            double endX = cx + Math.cos(gRad) * baseR * 1.2f;
            double endY = cy - 0.05f;
            double endZ = cz + Math.sin(gRad) * baseR * 1.2f;

            for (int s = 0; s <= GROUND_SEGS; s++) {
                float frac = (float) s / GROUND_SEGS;

                double px = startX + (endX - startX) * frac;
                double py = startY + (endY - startY) * frac;
                double pz = startZ + (endZ - startZ) * frac;

                if (s > 0 && s < GROUND_SEGS) {
                    px += (rng.nextFloat() - 0.5f) * 0.08f;
                    py += (rng.nextFloat() - 0.5f) * 0.03f;
                    pz += (rng.nextFloat() - 0.5f) * 0.08f;
                }

                float alpha = (1f - frac) * 0.6f;
                int   col   = (int) (gAngle + s * 20);

                renderParticle(stack, camera, camPos,
                        px, py, pz, col, 0.06f, alphaPC, alpha);
            }
        }

        /* ═══════════════════════════════════════════════════
         *  LAYER 6 — EM Rings
         *  Горизонтальные расходящиеся электромагнитные кольца
         * ═══════════════════════════════════════════════════ */
        for (int ring = 0; ring < EM_RING_COUNT; ring++) {
            float phase      = em * 2f + ring * 180f;
            float normalized = ((phase % 360f) + 360f) % 360f / 360f;
            float emR        = baseR * (0.4f + normalized * 1.2f);
            float emAlpha    = (1f - normalized) * 0.35f;
            float emY        = (float) (midY + (ring == 0 ? 0.1f : -0.1f));

            if (emAlpha < 0.02f) continue;

            for (int s = 0; s < EM_SEGMENTS; s++) {
                float angle = (float) s / EM_SEGMENTS * 360f;
                float rad   = (float) Math.toRadians(angle + t * 0.5f);

                double px = cx + Math.cos(rad) * emR;
                double pz = cz + Math.sin(rad) * emR;

                // прерывистое кольцо (пунктир)
                if ((s + (int) (t * 0.1f)) % 3 == 0) continue;

                int col = (int) (angle * 0.5f + t * 0.4f + ring * 90);
                renderParticle(stack, camera, camPos,
                        px, emY, pz, col,
                        0.05f, alphaPC, emAlpha);
            }
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
     *  HELPERS
     * ═══════════════════════════════════════════════════ */

    /**
     * Точка на сфере по theta (вокруг Y) и phi (наклон от экватора).
     */
    private double[] spherePoint(double cx, double cy, double cz,
                                 float radius,
                                 float thetaDeg, float phiDeg) {
        float thetaR = (float) Math.toRadians(thetaDeg);
        float phiR   = (float) Math.toRadians(phiDeg);
        float cosPhi = (float) Math.cos(phiR);

        return new double[]{
                cx + Math.cos(thetaR) * cosPhi * radius,
                cy + Math.sin(phiR) * radius,
                cz + Math.sin(thetaR) * cosPhi * radius
        };
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
        stack.multiply(RotationAxis.POSITIVE_Y
                .rotationDegrees(-camera.getYaw() + 180f));
        stack.multiply(RotationAxis.POSITIVE_X
                .rotationDegrees(-camera.getPitch() + 180f));

        RenderSystem.setShaderTexture(0, FileUtil.getImage("particles/glow"));
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        Matrix4f matrix = stack.peek().getPositionMatrix();

        // bloom
        float bSize  = size * 3.2f;
        float bAlpha = a * 0.12f;
        BufferBuilder buf = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.QUADS,
                        VertexFormats.POSITION_TEXTURE_COLOR);
        buf.vertex(matrix, -bSize,  bSize, 0).texture(0f, 1f).color(c[0], c[1], c[2], bAlpha);
        buf.vertex(matrix,  bSize,  bSize, 0).texture(1f, 1f).color(c[0], c[1], c[2], bAlpha);
        buf.vertex(matrix,  bSize, -bSize, 0).texture(1f, 0f).color(c[0], c[1], c[2], bAlpha);
        buf.vertex(matrix, -bSize, -bSize, 0).texture(0f, 0f).color(c[0], c[1], c[2], bAlpha);
        BufferRenderer.drawWithGlobalProgram(buf.end());

        // core
        buf = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.QUADS,
                        VertexFormats.POSITION_TEXTURE_COLOR);
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
    }
}