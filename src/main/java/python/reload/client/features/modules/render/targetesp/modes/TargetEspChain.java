package python.reload.client.features.modules.render.targetesp.modes;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import python.reload.api.event.events.render.Render3DEvent;
import python.reload.api.system.files.FileUtil;
import python.reload.api.utils.color.UIColors;
import python.reload.api.utils.math.MathUtil;
import python.reload.client.features.modules.render.targetesp.TargetEspMode;
import python.reload.client.features.modules.render.targetesp.TargetEspModule;

import java.awt.Color;

public class TargetEspChain extends TargetEspMode {

    // ── Анимация ──────────────────────────────────────────────
    private float rotation     = 0f;
    private float prevRotation = 0f;
    private float waveTime     = 0f;
    private float prevWaveTime = 0f;

    // ── Импакт ────────────────────────────────────────────────
    private float impactProgress = 0f;
    private int   prevHurtTime   = 0;

    // ── Параметры цепей ───────────────────────────────────────
    /** Количество спиральных цепей вокруг цели */
    private static final int   CHAIN_COUNT     = 2;
    /** Звенья на одну цепь */
    private static final int   LINKS_PER_CHAIN = 12;
    /** Сколько полных оборотов делает спираль снизу доверху */
    private static final float HELIX_TURNS     = 1.5f;
    /** Промежуточные частицы-связки между звеньями */
    private static final int   SUB_LINKS       = 2;

    /* ================================================================
     *  UPDATE
     * ================================================================ */

    @Override
    public void onUpdate() {
        if (currentTarget == null || !canDraw()) return;

        TargetEspModule module = TargetEspModule.getInstance();

        prevRotation = rotation;
        rotation += module.circleSpeed.getValue();

        prevWaveTime = waveTime;
        waveTime += module.circleSpeed.getValue() * 0.7f;

        updateImpactAnimation();
    }

    private void updateImpactAnimation() {
        TargetEspModule module = TargetEspModule.getInstance();

        if (!module.circleRedOnImpact.getValue() || currentTarget == null) {
            impactProgress = 0f;
            prevHurtTime   = 0;
            return;
        }

        float fadeIn       = module.circleImpactFadeIn.getValue();
        float fadeOut      = module.circleImpactFadeOut.getValue();
        float maxIntensity = module.circleImpactIntensity.getValue();
        int   hurtTime     = currentTarget.hurtTime;

        if (hurtTime > prevHurtTime || (hurtTime > 0 && prevHurtTime == 0)) {
            impactProgress = Math.min(maxIntensity, impactProgress + fadeIn);
        } else if (hurtTime > 0) {
            impactProgress = Math.min(maxIntensity, impactProgress + fadeIn * 0.5f);
        } else {
            impactProgress = Math.max(0f, impactProgress - fadeOut);
        }

        prevHurtTime = hurtTime;
    }

    /* ================================================================
     *  RENDER
     * ================================================================ */

    @Override
    public void onRender3D(Render3DEvent.Render3DEventData event) {
        if (currentTarget == null || !canDraw()) return;

        TargetEspModule module     = TargetEspModule.getInstance();
        MatrixStack     stack      = event.matrixStack();
        Camera          camera     = mc.gameRenderer.getCamera();
        float           alphaPC    = (float) showAnimation.getValue();

        // ── Интерполяция позиции цели ─────────────────────────
        Vec3d pos = new Vec3d(
                MathUtil.interpolate(currentTarget.prevX, currentTarget.getX()),
                MathUtil.interpolate(currentTarget.prevY, currentTarget.getY()),
                MathUtil.interpolate(currentTarget.prevZ, currentTarget.getZ())
        );

        double cx = pos.x - camera.getPos().x;
        double cy = pos.y - camera.getPos().y;
        double cz = pos.z - camera.getPos().z;

        float height   = currentTarget.getHeight();
        float width    = currentTarget.getWidth();
        float rotAngle = MathUtil.interpolate(prevRotation, rotation);
        float wave     = MathUtil.interpolate(prevWaveTime, waveTime);

        // ── Размеры ───────────────────────────────────────────
        float baseRadius   = width * 0.85f + (1f - alphaPC) / 3f;
        float linkSize     = 0.22f  * module.circleSize.getValue();
        float subLinkSize  = linkSize * 0.5f;
        float bloomLink    = (0.45f + module.circleBloomSize.getValue()) * module.circleSize.getValue();
        float bloomSub     = bloomLink * 0.45f;

        // ── GL state ──────────────────────────────────────────
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(
                GlStateManager.SrcFactor.SRC_ALPHA,
                GlStateManager.DstFactor.ONE
        );
        RenderSystem.setShaderTexture(0, FileUtil.getImage("particles/glow"));
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        // ── Отрисовка цепей ───────────────────────────────────
        for (int chain = 0; chain < CHAIN_COUNT; chain++) {

            float chainOffset = (360f / CHAIN_COUNT) * chain;
            float direction   = (chain % 2 == 0) ? 1f : -1f;   // чётные — CW, нечётные — CCW

            float prevPx = 0f, prevPy = 0f, prevPz = 0f;
            boolean hasPrev = false;

            for (int link = 0; link <= LINKS_PER_CHAIN; link++) {

                float t = (float) link / (float) LINKS_PER_CHAIN;   // 0 … 1

                // — Радиус: «бочка» — шире в центре, у́же сверху/снизу —
                float bulge  = (float) Math.sin(t * Math.PI);
                float radius = baseRadius * (0.7f + 0.3f * bulge);
                // микро-пульсация
                radius += (float) Math.sin(Math.toRadians(wave * 2f + link * 30f)) * 0.04f;

                // — Угол спирали —
                float angle = chainOffset
                        + rotAngle * direction
                        + t * 360f * HELIX_TURNS;
                float rad = (float) Math.toRadians(angle);

                float px = (float) (cx + Math.sin(rad) * radius);
                float pz = (float) (cz + Math.cos(rad) * radius);

                // — Вертикаль + лёгкая волна —
                float vertWave = (float) Math.sin(Math.toRadians(wave + link * 22f)) * 0.06f;
                float py = (float) (cy + height * t + vertWave);

                // — Цвет —
                int   colorIdx = (int) (chain * 120 + link * 10 + rotAngle * 0.4f);
                int   alpha    = (int) (alphaPC * 255);
                Color color    = UIColors.gradient(colorIdx, alpha);

                if (impactProgress > 0f) {
                    color = lerpColor(color, new Color(255, 32, 32, alpha), impactProgress);
                }

                // — Основное звено —
                renderGlow(stack, camera, px, py, pz,
                        linkSize, bloomLink, color, alphaPC,
                        module.circleBloom.getValue());

                // — Промежуточные связки до предыдущего звена —
                if (hasPrev) {
                    for (int s = 1; s <= SUB_LINKS; s++) {
                        float st = (float) s / (float) (SUB_LINKS + 1);

                        float sx = prevPx + (px - prevPx) * st;
                        float sy = prevPy + (py - prevPy) * st;
                        float sz = prevPz + (pz - prevPz) * st;

                        int   subAlpha    = (int) (alphaPC * 190);
                        int   subColorIdx = (int) (chain * 120 + (link - 1 + st) * 10 + rotAngle * 0.4f);
                        Color subColor    = UIColors.gradient(subColorIdx, subAlpha);

                        if (impactProgress > 0f) {
                            subColor = lerpColor(subColor,
                                    new Color(255, 32, 32, subAlpha), impactProgress);
                        }

                        renderGlow(stack, camera, sx, sy, sz,
                                subLinkSize, bloomSub, subColor, alphaPC,
                                module.circleBloom.getValue());
                    }
                }

                prevPx  = px;
                prevPy  = py;
                prevPz  = pz;
                hasPrev = true;
            }
        }

        // ── Восстановление GL state ──────────────────────────
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.blendFunc(
                GlStateManager.SrcFactor.SRC_ALPHA,
                GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA
        );
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    /* ================================================================
     *  HELPERS
     * ================================================================ */

    /**
     * Рисует billboarded glow-квад с опциональным bloom-слоем.
     */
    private void renderGlow(MatrixStack stack, Camera camera,
                            float px, float py, float pz,
                            float size, float bloomSize,
                            Color color, float alphaPC, boolean bloom) {

        stack.push();
        stack.translate(px, py, pz);
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

        var matrix = stack.peek().getPositionMatrix();

        // ─ Bloom layer ─
        if (bloom) {
            int   ba  = clamp((int) (alphaPC * 255 * 0.1f), 0, 255);
            int   rgb = new Color(color.getRed(), color.getGreen(), color.getBlue(), ba).getRGB();
            float hs  = bloomSize / 2f;

            var buf = Tessellator.getInstance()
                    .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            buf.vertex(matrix, -hs,  hs, -hs).texture(0f, 1f).color(rgb);
            buf.vertex(matrix,  hs,  hs, -hs).texture(1f, 1f).color(rgb);
            buf.vertex(matrix,  hs, -hs, -hs).texture(1f, 0f).color(rgb);
            buf.vertex(matrix, -hs, -hs, -hs).texture(0f, 0f).color(rgb);
            BufferRenderer.drawWithGlobalProgram(buf.end());
        }

        // ─ Main quad ─
        {
            int   rgb = color.getRGB();
            float hs  = size / 2f;

            var buf = Tessellator.getInstance()
                    .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            buf.vertex(matrix, -hs,  hs, -hs).texture(0f, 1f).color(rgb);
            buf.vertex(matrix,  hs,  hs, -hs).texture(1f, 1f).color(rgb);
            buf.vertex(matrix,  hs, -hs, -hs).texture(1f, 0f).color(rgb);
            buf.vertex(matrix, -hs, -hs, -hs).texture(0f, 0f).color(rgb);
            BufferRenderer.drawWithGlobalProgram(buf.end());
        }

        stack.pop();
    }

    /**
     * Smoothstep-интерполяция цветов (hermite).
     */
    private Color lerpColor(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        float s = t * t * (3f - 2f * t);

        return new Color(
                clamp((int) (a.getRed()   + (b.getRed()   - a.getRed())   * s), 0, 255),
                clamp((int) (a.getGreen() + (b.getGreen() - a.getGreen()) * s), 0, 255),
                clamp((int) (a.getBlue()  + (b.getBlue()  - a.getBlue())  * s), 0, 255),
                clamp((int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * s), 0, 255)
        );
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    /* ================================================================
     *  TARGET RESET
     * ================================================================ */

    @Override
    public void updateTarget() {
        super.updateTarget();
        if (currentTarget == null) {
            impactProgress = 0f;
            prevHurtTime   = 0;
        }
    }
}