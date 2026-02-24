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
 * TargetEspMatrix — «Digital Rain / Code Cage»
 *
 * ┌─────────────────────────────────────────────────┐
 * │  Layer 1 — Вертикальные «потоки» данных         │
 * │  Layer 2 — Горизонтальные сканирующие линии      │
 * │  Layer 3 — Вращающийся каркас (wireframe cage)    │
 * │  Layer 4 — «Глитч» частицы (случайные вспышки)   │
 * │  Layer 5 — Кольца загрузки (loading rings)        │
 * │  Layer 6 — Данные сканирования (scan grid на полу)│
 * └─────────────────────────────────────────────────┘
 */
public class TargetEspMatrix extends TargetEspMode {

    private float time      = 0f, prevTime      = 0f;
    private float scanPhase = 0f, prevScanPhase = 0f;
    private float gridPhase = 0f, prevGridPhase = 0f;
    private long  frameTick = 0;

    private static final int   DATA_STREAMS     = 12;
    private static final int   STREAM_PARTICLES = 8;
    private static final int   SCAN_LINES       = 3;
    private static final int   SCAN_SEGMENTS    = 20;
    private static final int   CAGE_EDGES       = 12;
    private static final int   CAGE_SUBS        = 6;
    private static final int   GLITCH_COUNT     = 15;
    private static final int   LOADING_RINGS    = 2;
    private static final int   LOADING_SEGMENTS = 16;
    private static final int   GRID_SIZE        = 6;

    @Override
    public void onUpdate() {
        updateTarget();

        prevTime      = time;
        prevScanPhase = scanPhase;
        prevGridPhase = gridPhase;

        time      += 7f;
        scanPhase += 5f;
        gridPhase += 3f;
        frameTick++;
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

        float t    = MathUtil.interpolate(prevTime, time);
        float scan = MathUtil.interpolate(prevScanPhase, scanPhase);
        float grid = MathUtil.interpolate(prevGridPhase, gridPhase);

        float baseR = width * 1.3f * anim;

        /* ═══════════════════════════════════════════════════
         *  LAYER 1 — Data Streams (вертикальные потоки)
         * ═══════════════════════════════════════════════════ */
        for (int stream = 0; stream < DATA_STREAMS; stream++) {
            float angle = (float) stream / DATA_STREAMS * 360f + t * 0.3f;
            float rad   = (float) Math.toRadians(angle);

            float streamR = baseR * (0.8f + 0.2f * (float) Math.sin(
                    Math.toRadians(t + stream * 30)));

            double sx = cx + Math.cos(rad) * streamR;
            double sz = cz + Math.sin(rad) * streamR;

            for (int p = 0; p < STREAM_PARTICLES; p++) {
                // падающая частица: каждая имеет свой фазовый сдвиг
                float fallPhase = ((t * 3f + stream * 37f + p * 45f) % 360f) / 360f;
                double sy = cy + height * 1.2f - fallPhase * height * 1.4f;

                // «головная» частица ярче
                boolean isHead = fallPhase < 0.15f;
                float alpha = isHead ? 0.9f : (1f - fallPhase) * 0.5f;
                float size  = isHead ? 0.1f : 0.06f * (1f - fallPhase * 0.5f);

                int col = (int) (angle + stream * 15 + p * 20);

                if (sy >= cy - 0.2f && alpha > 0.02f) {
                    renderParticle(stack, camera, camPos,
                            sx, sy, sz, col, size, alphaPC, alpha);
                }
            }
        }

        /* ═══════════════════════════════════════════════════
         *  LAYER 2 — Horizontal Scan Lines
         * ═══════════════════════════════════════════════════ */
        for (int line = 0; line < SCAN_LINES; line++) {
            float scanY = ((scan * 1.5f + line * 120f) % 360f) / 360f;
            double lineY = cy + scanY * height;

            float scanAlpha = (float) Math.sin(scanY * Math.PI) * 0.6f;
            if (scanAlpha < 0.02f) continue;

            float lineR = baseR * (0.9f + 0.1f * (float) Math.sin(
                    Math.toRadians(scan + line * 60)));

            for (int s = 0; s < SCAN_SEGMENTS; s++) {
                float angle = (float) s / SCAN_SEGMENTS * 360f;
                float sRad  = (float) Math.toRadians(angle);

                // пунктир
                if ((s + (int)(t * 0.05f)) % 3 == 0) continue;

                double px = cx + Math.cos(sRad) * lineR;
                double pz = cz + Math.sin(sRad) * lineR;

                int col = (int) (angle * 0.5f + scan + line * 40);
                renderParticle(stack, camera, camPos,
                        px, lineY, pz, col,
                        0.05f, alphaPC, scanAlpha);
            }
        }

        /* ═══════════════════════════════════════════════════
         *  LAYER 3 — Wireframe Cage (вращающийся каркас)
         * ═══════════════════════════════════════════════════ */
        // 8 вершин прямоугольного параллелепипеда
        float hw = baseR * 0.65f;
        float hh = height * 0.55f;
        float hd = baseR * 0.65f;

        float cageRot = t * 0.4f;
        float cosR = (float) Math.cos(Math.toRadians(cageRot));
        float sinR = (float) Math.sin(Math.toRadians(cageRot));

        float[][] localVerts = {
                {-hw, -hh, -hd}, { hw, -hh, -hd},
                { hw, -hh,  hd}, {-hw, -hh,  hd},
                {-hw,  hh, -hd}, { hw,  hh, -hd},
                { hw,  hh,  hd}, {-hw,  hh,  hd}
        };

        double[][] worldVerts = new double[8][3];
        double midY = cy + height / 2.0;

        for (int i = 0; i < 8; i++) {
            float rx = localVerts[i][0] * cosR - localVerts[i][2] * sinR;
            float rz = localVerts[i][0] * sinR + localVerts[i][2] * cosR;
            worldVerts[i] = new double[]{cx + rx, midY + localVerts[i][1], cz + rz};
        }

        int[][] edges = {
                {0,1},{1,2},{2,3},{3,0},
                {4,5},{5,6},{6,7},{7,4},
                {0,4},{1,5},{2,6},{3,7}
        };

        for (int e = 0; e < CAGE_EDGES; e++) {
            double[] v1 = worldVerts[edges[e][0]];
            double[] v2 = worldVerts[edges[e][1]];

            for (int s = 0; s <= CAGE_SUBS; s++) {
                float frac = (float) s / CAGE_SUBS;
                double px = v1[0] + (v2[0] - v1[0]) * frac;
                double py = v1[1] + (v2[1] - v1[1]) * frac;
                double pz = v1[2] + (v2[2] - v1[2]) * frac;

                // пульсация по ребру
                float edgePulse = 0.5f + 0.5f * (float) Math.sin(
                        Math.toRadians(t * 3 + e * 30 + s * 20));

                int col = (int) (cageRot + e * 25 + 100);
                renderParticle(stack, camera, camPos,
                        px, py, pz, col,
                        0.05f * edgePulse, alphaPC,
                        0.4f * edgePulse);
            }
        }

        /* ═══════════════════════════════════════════════════
         *  LAYER 4 — Glitch Particles
         * ═══════════════════════════════════════════════════ */
        long glitchSeed = frameTick / 2;
        java.util.Random rng = new java.util.Random(glitchSeed);

        for (int g = 0; g < GLITCH_COUNT; g++) {
            // случайная позиция рядом с целью
            double gx = cx + (rng.nextFloat() - 0.5f) * baseR * 2.5f;
            double gy = cy + rng.nextFloat() * height * 1.2f;
            double gz = cz + (rng.nextFloat() - 0.5f) * baseR * 2.5f;

            float gAlpha = rng.nextFloat() * 0.6f + 0.1f;
            float gSize  = 0.04f + rng.nextFloat() * 0.06f;
            int   col    = (int) (rng.nextFloat() * 360);

            renderParticle(stack, camera, camPos,
                    gx, gy, gz, col, gSize, alphaPC, gAlpha);
        }

        /* ═══════════════════════════════════════════════════
         *  LAYER 5 — Loading Rings
         * ═══════════════════════════════════════════════════ */
        for (int ring = 0; ring < LOADING_RINGS; ring++) {
            float ringY = (float) (cy + height * (0.3f + ring * 0.4f));
            float ringR = baseR * (0.4f + ring * 0.15f);
            float speed = (ring % 2 == 0) ? 1.5f : -1.2f;

            for (int s = 0; s < LOADING_SEGMENTS; s++) {
                float angle = t * speed + (float) s / LOADING_SEGMENTS * 360f;

                // неполное кольцо — дуга ~270°
                float relAngle = ((angle % 360f) + 360f) % 360f;
                if (relAngle > 270f) continue;

                float sRad  = (float) Math.toRadians(angle);
                double lx = cx + Math.cos(sRad) * ringR;
                double lz = cz + Math.sin(sRad) * ringR;

                float segAlpha = 0.5f * (relAngle / 270f);
                int   col      = (int) (angle * 0.5f + ring * 90 + 150);

                renderParticle(stack, camera, camPos,
                        lx, ringY, lz, col,
                        0.06f, alphaPC, segAlpha);
            }
        }

        /* ═══════════════════════════════════════════════════
         *  LAYER 6 — Scan Grid on ground
         * ═══════════════════════════════════════════════════ */
        float gridR    = baseR * 1.4f;
        float gridStep = gridR * 2f / GRID_SIZE;
        double gridY   = cy + 0.02f;

        float gridWave = (grid * 2f % 360f) / 360f;

        for (int gx = 0; gx <= GRID_SIZE; gx++) {
            for (int gz = 0; gz <= GRID_SIZE; gz++) {
                double px = cx - gridR + gx * gridStep;
                double pz = cz - gridR + gz * gridStep;

                float dist = (float) Math.sqrt(
                        (px - cx) * (px - cx) + (pz - cz) * (pz - cz));
                float normDist = dist / gridR;

                // волна расходится от центра
                float wave = (float) Math.abs(Math.sin(
                        (normDist - gridWave) * Math.PI * 3));

                float alpha = (1f - normDist) * 0.3f * wave;
                if (alpha < 0.02f) continue;

                int col = (int) (gx * 20 + gz * 20 + grid * 0.3f + 200);
                renderParticle(stack, camera, camPos,
                        px, gridY, pz, col,
                        0.04f + 0.02f * wave, alphaPC, alpha);
            }
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

        float bS = size * 2.5f, bA = a * 0.1f;
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