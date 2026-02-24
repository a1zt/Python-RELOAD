package python.reload.client.features.modules.render.targetesp.modes;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
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

public class TargetEspDragon extends TargetEspMode {

    private float flyAngle = 0f, prevFlyAngle = 0f;
    private float bodyPhase = 0f, prevBodyPhase = 0f;
    private float breathPhase = 0f, prevBreathPhase = 0f;
    private final List<double[]> bodyTrail = new ArrayList<>();

    @Override
    public void onUpdate() {
        updateTarget();
        prevFlyAngle = flyAngle; prevBodyPhase = bodyPhase; prevBreathPhase = breathPhase;
        flyAngle += 5f; bodyPhase += 10f; breathPhase += 7f;
    }

    @Override
    public void onRender3D(Render3DEvent.Render3DEventData event) {
        if (currentTarget == null || !canDraw()) return;
        MatrixStack ms = event.matrixStack();
        RenderUtil.WORLD.startRender(ms);

        float anim = (float) MathUtil.interpolate(prevSizeAnimation, sizeAnimation.getValue());
        float show = (float) showAnimation.getValue();
        if (anim < 0.01f || show < 0.01f) { RenderUtil.WORLD.endRender(ms); return; }

        double cx = getTargetX(), cy = getTargetY(), cz = getTargetZ();
        float h = currentTarget.getHeight(), w = currentTarget.getWidth();
        double midY = cy + h / 2.0;
        double camX = mc.getEntityRenderDispatcher().camera.getPos().getX();
        double camY = mc.getEntityRenderDispatcher().camera.getPos().getY();
        double camZ = mc.getEntityRenderDispatcher().camera.getPos().getZ();

        float iFly = MathUtil.interpolate(prevFlyAngle, flyAngle);
        float iBody = MathUtil.interpolate(prevBodyPhase, bodyPhase);
        float iBreath = MathUtil.interpolate(prevBreathPhase, breathPhase);

        // Дракон летает вокруг — голова
        float dragonR = w * 2.0f * anim;
        float dragonBob = (float) Math.sin(Math.toRadians(iFly * 2)) * 0.4f * anim;
        double headX = cx + Math.cos(Math.toRadians(iFly)) * dragonR;
        double headY = midY + dragonBob + 0.5f * anim;
        double headZ = cz + Math.sin(Math.toRadians(iFly)) * dragonR;

        // Голова (яркая)
        renderGlow(ms, headX - camX, headY - camY, headZ - camZ, iFly, 0.25f * anim, show * 0.9f);
        renderGlow(ms, headX - camX, headY - camY, headZ - camZ, iFly + 60, 0.4f * anim, show * 0.2f);

        // Тело — змеиное, следует за головой с задержкой
        bodyTrail.add(0, new double[]{headX, headY, headZ, iFly});
        if (bodyTrail.size() > 30) bodyTrail.subList(30, bodyTrail.size()).clear();

        for (int i = 1; i < bodyTrail.size(); i++) {
            double[] seg = bodyTrail.get(i);
            float t = i / (float) bodyTrail.size();
            float segSize = (0.2f - t * 0.1f) * anim;
            float segAlpha = show * (1f - t * 0.5f) * 0.7f;

            // Волнообразное движение тела
            float wave = (float) Math.sin(Math.toRadians(iBody + i * 30)) * 0.1f * anim;
            renderGlow(ms, seg[0] - camX, seg[1] + wave - camY, seg[2] - camZ,
                    (float) seg[3] + i * 10, segSize, segAlpha);
        }

        // Крылья — взмах от головы
        float wingFlap = (float) Math.sin(Math.toRadians(iBody * 1.5f));
        float headDir = (float) Math.toRadians(iFly + 90);
        for (int wing = 0; wing < 2; wing++) {
            float side = wing == 0 ? 1f : -1f;
            for (int i = 0; i < 6; i++) {
                float t = (i + 1) / 6f;
                float wingSpread = t * w * 1.5f * anim;
                float wingLift = wingFlap * t * 0.6f * anim;

                double wx = headX + Math.cos(headDir) * wingSpread * side;
                double wy = headY + wingLift;
                double wz = headZ + Math.sin(headDir) * wingSpread * side;

                float sz = (0.15f - t * 0.015f) * anim;
                renderGlow(ms, wx - camX, wy - camY, wz - camZ, iFly + t * 60 + wing * 120, sz, show * (1f - t * 0.3f) * 0.6f);
            }
        }

        // Огненное дыхание (волна от головы к цели)
        float breathIntensity = (float) Math.max(0, Math.sin(Math.toRadians(iBreath)));
        if (breathIntensity > 0.3f) {
            for (int i = 0; i < 8; i++) {
                float t = i / 8f;
                double bx = headX + (cx - headX) * t;
                double by = headY + (midY - headY) * t;
                double bz = headZ + (cz - headZ) * t;
                float spread = t * 0.3f * anim;
                float bAngle = iBreath * 3 + t * 90;
                bx += Math.sin(Math.toRadians(bAngle)) * spread;
                by += Math.cos(Math.toRadians(bAngle * 1.3f)) * spread * 0.5f;

                float bSz = 0.15f * anim * breathIntensity * (1f - t * 0.3f);
                float bAl = show * breathIntensity * (1f - t * 0.5f) * 0.5f;
                renderGlow(ms, bx - camX, by - camY, bz - camZ, bAngle + 30, bSz, bAl);
            }
        }

        // Хвост — конец трейла
        if (bodyTrail.size() > 25) {
            for (int i = 25; i < bodyTrail.size(); i++) {
                double[] tail = bodyTrail.get(i);
                float t = (i - 25f) / 5f;
                float tailSize = 0.06f * (1f - t) * anim;
                renderGlow(ms, tail[0] - camX, tail[1] - camY, tail[2] - camZ,
                        (float) tail[3] + 90, tailSize, show * (1f - t) * 0.5f);
            }
        }

        // Ореол цели
        for (int i = 0; i < 12; i++) {
            float angle = iFly * 0.3f + i * 30f;
            float r = w * 0.7f * anim;
            double ox = cx + Math.cos(Math.toRadians(angle)) * r;
            double oz = cz + Math.sin(Math.toRadians(angle)) * r;
            renderGlow(ms, ox - camX, cy - camY, oz - camZ, angle, 0.04f * anim, show * 0.25f);
        }

        RenderSystem.enableCull();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderUtil.WORLD.endRender(ms);
    }

    private void renderGlow(MatrixStack stack, double x, double y, double z, float colorAngle, float size, float alpha) {
        stack.push(); stack.translate(x, y, z);
        Camera camera = mc.gameRenderer.getCamera();
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw() + 180.0F));
        stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-camera.getPitch() + 180.0F));
        RenderSystem.setShaderTexture(0, FileUtil.getImage("particles/glow"));
        Matrix4f matrix = stack.peek().getPositionMatrix();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        float a = Math.min(1f, Math.max(0f, alpha));
        float[] c = ColorUtil.normalize(ColorUtil.setAlpha(UIColors.gradient((int) colorAngle), (int)(255 * a)));
        buffer.vertex(matrix, -size, size, 0).texture(0f, 1f).color(c[0], c[1], c[2], c[3]);
        buffer.vertex(matrix, size, size, 0).texture(1f, 1f).color(c[0], c[1], c[2], c[3]);
        buffer.vertex(matrix, size, -size, 0).texture(1f, 0f).color(c[0], c[1], c[2], c[3]);
        buffer.vertex(matrix, -size, -size, 0).texture(0f, 0f).color(c[0], c[1], c[2], c[3]);
        BufferRenderer.drawWithGlobalProgram(buffer.end()); stack.pop();
    }
}