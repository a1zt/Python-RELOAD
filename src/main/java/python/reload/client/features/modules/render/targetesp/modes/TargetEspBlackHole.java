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

/**
 * TargetEspBlackHole
 *
 * ┌──────────────────────────────────────────────────┐
 * │  Layer 1 — Аккреционный диск (3 кольца)          │
 * │  Layer 2 — Гравитационное линзирование (warp)     │
 * │  Layer 3 — Засасывающие частицы (infalling)        │
 * │  Layer 4 — Релятивистские джеты                    │
 * │  Layer 5 — Горизонт событий (event horizon glow)   │
 * │  Layer 6 — Хокинговское излучение (мерцание)       │
 * └──────────────────────────────────────────────────┘
 */
public class TargetEspBlackHole extends TargetEspMode {

    private float rotation  = 0f, prevRotation  = 0f;
    private float accTime   = 0f, prevAccTime   = 0f;
    private float jetPhase  = 0f, prevJetPhase  = 0f;
    private float spiralIn  = 0f, prevSpiralIn  = 0f;

    private static final int   ACCRETION_RINGS    = 3;
    private static final int   RING_SEGMENTS      = 32;
    private static final int   WARP_ARCS          = 2;
    private static final int   WARP_SEGMENTS      = 20;
    private static final int   INFALL_STREAMS     = 4;
    private static final int   INFALL_PARTICLES   = 12;
    private static final int   JET_PARTICLES      = 12;
    private static final int   HORIZON_SEGMENTS   = 20;
    private static final int   HAWKING_PARTICLES  = 16;

    @Override
    public void onUpdate() {
        updateTarget();

        prevRotation = rotation;
        prevAccTime  = accTime;
        prevJetPhase = jetPhase;
        prevSpiralIn = spiralIn;

        rotation += 12f;
        accTime  += 8f;
        jetPhase += 6f;
        spiralIn += 10f;
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
        double midY  = cy + height / 2.0;

        float rot  = MathUtil.interpolate(prevRotation, rotation);
        float acc  = MathUtil.interpolate(prevAccTime, accTime);
        float jet  = MathUtil.interpolate(prevJetPhase, jetPhase);
        float spIn = MathUtil.interpolate(prevSpiralIn, spiralIn);

        float baseR = width * 1.5f * anim;

        // наклон диска
        float diskTilt = (float) Math.toRadians(25f);
        float cosTilt  = (float) Math.cos(diskTilt);
        float sinTilt  = (float) Math.sin(diskTilt);

        /* ═══════════════════════════════════════════════════
         *  LAYER 1 — Accretion Disk
         * ═══════════════════════════════════════════════════ */
        for (int ring = 0; ring < ACCRETION_RINGS; ring++) {
            float ringR    = baseR * (0.55f + ring * 0.25f);
            float ringSpeed = 1.5f - ring * 0.3f; // внутренние быстрее
            float ringAlpha = 0.7f - ring * 0.15f;

            for (int s = 0; s < RING_SEGMENTS; s++) {
                float angle = rot * ringSpeed + (float) s / RING_SEGMENTS * 360f;
                float rad   = (float) Math.toRadians(angle);

                float lx = (float) Math.cos(rad) * ringR;
                float lz = (float) Math.sin(rad) * ringR;
                float ly = lz * sinTilt;
                lz *= cosTilt;

                // «разогрев» — внутренние кольца ярче
                float heat = (float) Math.sin(Math.toRadians(
                        acc * (3 + ring) + s * 12)) * 0.15f;

                double px = cx + lx;
                double py = midY + ly;
                double pz = cz + lz;

                float size = 0.07f + 0.03f * (ACCRETION_RINGS - ring);
                int col = (int) (angle * 0.6f + ring * 80);

                renderParticle(stack, camera, camPos,
                        px, py, pz, col,
                        size * (1f + heat), alphaPC,
                        ringAlpha * (1f + heat));
            }
        }

        /* ═══════════════════════════════════════════════════
         *  LAYER 2 — Gravitational Lensing (Warp Arcs)
         *  Вертикальные дуги вокруг «горизонта»
         * ═══════════════════════════════════════════════════ */
        for (int arc = 0; arc < WARP_ARCS; arc++) {
            float arcOffset = arc * (360f / WARP_ARCS) + rot * 0.15f;

            for (int s = 0; s < WARP_SEGMENTS; s++) {
                float t      = (float) s / (WARP_SEGMENTS - 1) * 180f - 90f;
                float tRad   = (float) Math.toRadians(t);
                float warpR  = baseR * 0.45f;

                float aRad = (float) Math.toRadians(arcOffset);

                // дуга в вертикальной плоскости
                float wx = (float) Math.cos(aRad) * (float) Math.cos(tRad) * warpR;
                float wy = (float) Math.sin(tRad) * warpR;
                float wz = (float) Math.sin(aRad) * (float) Math.cos(tRad) * warpR;

                // колебание
                float wobble = (float) Math.sin(
                        Math.toRadians(acc * 2 + s * 20 + arc * 90)) * 0.02f;
                wx += wobble;

                double px = cx + wx;
                double py = midY + wy;
                double pz = cz + wz;

                float edgeFade = (float) Math.cos(tRad);
                int col = (int) (arcOffset + t + 120);

                renderParticle(stack, camera, camPos,
                        px, py, pz, col,
                        0.05f, alphaPC, 0.35f * edgeFade);
            }
        }

        /* ═══════════════════════════════════════════════════
         *  LAYER 3 — Infalling Particles (засасывание)
         * ═══════════════════════════════════════════════════ */
        for (int stream = 0; stream < INFALL_STREAMS; stream++) {
            float streamAngle = rot * 0.3f + stream * (360f / INFALL_STREAMS);
            float sRad = (float) Math.toRadians(streamAngle);

            for (int p = 0; p < INFALL_PARTICLES; p++) {
                // спираль закручивается к центру
                float t       = (float) p / INFALL_PARTICLES;
                float spiralA = spIn * (1f + t * 2f) + stream * 90f + p * 25f;
                float spiralRad = (float) Math.toRadians(spiralA);
                float r       = baseR * (1.3f - t * 1.1f); // снаружи внутрь

                float ix = (float) Math.cos(spiralRad) * r;
                float iz = (float) Math.sin(spiralRad) * r;
                float iy = iz * sinTilt * 0.5f;
                iz *= cosTilt;

                double px = cx + ix;
                double py = midY + iy;
                double pz = cz + iz;

                float alpha = t * 0.8f; // ярче ближе к центру
                float size  = 0.05f + t * 0.06f;
                int   col   = (int) (spiralA * 0.5f + stream * 60);

                renderParticle(stack, camera, camPos,
                        px, py, pz, col, size, alphaPC, alpha);
            }
        }

        /* ═══════════════════════════════════════════════════
         *  LAYER 4 — Relativistic Jets
         * ═══════════════════════════════════════════════════ */
        for (int pole = -1; pole <= 1; pole += 2) {
            for (int p = 0; p < JET_PARTICLES; p++) {
                float t    = (float) p / JET_PARTICLES;
                float jetH = height * 0.8f * t * anim;

                // расширяющийся конус
                float coneR = t * baseR * 0.25f;
                float jAngle = jet * 4f + p * 35f;
                float jRad   = (float) Math.toRadians(jAngle);

                float jx = (float) Math.cos(jRad) * coneR;
                float jz = (float) Math.sin(jRad) * coneR;

                double px = cx + jx;
                double py = midY + pole * (0.1f + jetH);
                double pz = cz + jz;

                float alpha = (1f - t) * 0.7f;
                float size  = 0.1f * (1f - t * 0.4f);
                int   col   = (int) (jAngle + pole * 120 + 60);

                renderParticle(stack, camera, camPos,
                        px, py, pz, col, size, alphaPC, alpha);
            }
        }

        /* ═══════════════════════════════════════════════════
         *  LAYER 5 — Event Horizon Glow
         * ═══════════════════════════════════════════════════ */
        float horizonPulse = 0.6f + 0.4f * (float) Math.sin(
                Math.toRadians(acc * 3));
        float horizonR = baseR * 0.35f;

        // центральная тьма (яркая обводка)
        renderParticle(stack, camera, camPos,
                cx, midY, cz, (int) (rot * 0.3f),
                0.3f * horizonPulse, alphaPC, 0.9f);

        for (int s = 0; s < HORIZON_SEGMENTS; s++) {
            float angle = (float) s / HORIZON_SEGMENTS * 360f + rot * 0.5f;
            float rad   = (float) Math.toRadians(angle);

            double hx = cx + Math.cos(rad) * horizonR;
            double hz = cz + Math.sin(rad) * horizonR;

            float flicker = 0.7f + 0.3f * (float) Math.sin(
                    Math.toRadians(acc * 5 + s * 30));

            int col = (int) (angle * 0.4f + 200);
            renderParticle(stack, camera, camPos,
                    hx, midY, hz, col,
                    0.08f * flicker, alphaPC, 0.6f * flicker);
        }

        /* ═══════════════════════════════════════════════════
         *  LAYER 6 — Hawking Radiation
         * ═══════════════════════════════════════════════════ */
        for (int i = 0; i < HAWKING_PARTICLES; i++) {
            float seed = i * 137.508f;
            float hAngle = seed + acc * 0.5f;
            float hRad   = (float) Math.toRadians(hAngle);

            float dist = horizonR * (1.1f + 0.3f * (float) Math.abs(
                    Math.sin(Math.toRadians(seed + acc))));

            float phi = (float) Math.sin(Math.toRadians(seed * 1.3f)) * 60f;
            float phiRad = (float) Math.toRadians(phi);

            double hx = cx + Math.cos(hRad) * Math.cos(phiRad) * dist;
            double hy = midY + Math.sin(phiRad) * dist;
            double hz = cz + Math.sin(hRad) * Math.cos(phiRad) * dist;

            float flicker = 0.3f + 0.7f * (float) Math.abs(
                    Math.sin(Math.toRadians(acc * 7 + seed * 2)));

            int col = (int) (seed + acc * 0.3f + 280);
            renderParticle(stack, camera, camPos,
                    hx, hy, hz, col,
                    0.035f * flicker, alphaPC,
                    0.2f * flicker);
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

        float bS = size * 3.2f, bA = a * 0.12f;
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
    }
}