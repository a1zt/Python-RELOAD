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

public class TargetEspNebula extends TargetEspMode {

    private float driftPhase = 0f, prevDriftPhase = 0f;
    private float swirl = 0f, prevSwirl = 0f;
    private float twinkle = 0f, prevTwinkle = 0f;

    // Предгенерированные облачные частицы (статичные смещения)
    private final List<float[]> cloudPoints = new ArrayList<>();
    private boolean initialized = false;

    private void initCloud() {
        if (initialized) return;
        initialized = true;
        for (int i = 0; i < 80; i++) {
            cloudPoints.add(new float[]{
                    (float)(Math.random() * 2 - 1),  // x offset
                    (float)(Math.random() * 2 - 1),  // y offset
                    (float)(Math.random() * 2 - 1),  // z offset
                    (float)(Math.random() * 360),     // color phase
                    (float)(0.5f + Math.random() * 0.5f), // size mult
                    (float)(Math.random() * Math.PI * 2),  // drift speed
                    (float)(0.3f + Math.random() * 0.7f)   // alpha mult
            });
        }
    }

    @Override
    public void onUpdate() {
        updateTarget();
        initCloud();
        prevDriftPhase = driftPhase; prevSwirl = swirl; prevTwinkle = twinkle;
        driftPhase += 1.5f; swirl += 3f; twinkle += 7f;
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

        float iDrift = MathUtil.interpolate(prevDriftPhase, driftPhase);
        float iSwirl = MathUtil.interpolate(prevSwirl, swirl);
        float iTwinkle = MathUtil.interpolate(prevTwinkle, twinkle);

        float spread = w * 1.8f * anim;
        float vSpread = h * 0.9f * anim;

        // Облачные частицы
        for (float[] pt : cloudPoints) {
            float driftX = (float) Math.sin(Math.toRadians(iDrift * pt[5] * 50 + pt[3])) * 0.15f;
            float driftY = (float) Math.cos(Math.toRadians(iDrift * pt[5] * 40 + pt[3] * 1.3f)) * 0.1f;
            float driftZ = (float) Math.sin(Math.toRadians(iDrift * pt[5] * 30 + pt[3] * 0.7f)) * 0.15f;

            double px = cx + (pt[0] + driftX) * spread;
            double py = midY + (pt[1] + driftY) * vSpread;
            double pz = cz + (pt[2] + driftZ) * spread;

            float twinkleVal = (float) Math.sin(Math.toRadians(iTwinkle * pt[5] * 80 + pt[3])) * 0.3f + 0.7f;
            float sz = 0.15f * pt[4] * anim * twinkleVal;
            float al = show * pt[6] * twinkleVal * 0.35f;

            renderGlow(ms, px - camX, py - camY, pz - camZ, pt[3] + iSwirl * 0.5f, sz, al);
        }

        // Яркие звёзды внутри туманности
        for (int i = 0; i < 12; i++) {
            float sAngle = iSwirl * 0.3f + i * 30f;
            float sR = w * 0.8f * anim;
            float sY = (float)(midY + Math.sin(Math.toRadians(sAngle * 1.5f + iDrift)) * h * 0.3f * anim);
            double sx = cx + Math.cos(Math.toRadians(sAngle)) * sR;
            double sz = cz + Math.sin(Math.toRadians(sAngle)) * sR;
            float starTwinkle = (float) Math.sin(Math.toRadians(iTwinkle * 2 + i * 30)) * 0.4f + 0.6f;
            renderGlow(ms, sx - camX, sY - camY, sz - camZ, sAngle + 45, 0.08f * anim * starTwinkle, show * starTwinkle * 0.7f);
        }

        // Центральная звезда
        float corePulse = 1f + (float) Math.sin(Math.toRadians(iTwinkle * 0.8f)) * 0.2f;
        renderGlow(ms, cx - camX, midY - camY, cz - camZ, iSwirl, 0.4f * anim * corePulse, show * 0.35f);
        renderGlow(ms, cx - camX, midY - camY, cz - camZ, iSwirl + 120, 0.65f * anim, show * 0.1f);

        // Внешняя дымка (большие полупрозрачные частицы)
        for (int i = 0; i < 6; i++) {
            float hAngle = iDrift * 0.2f + i * 60f;
            float hR = w * 2.5f * anim;
            float hY = (float)(midY + Math.sin(Math.toRadians(hAngle + iDrift * 0.5f)) * h * 0.5f * anim);
            double hx = cx + Math.cos(Math.toRadians(hAngle)) * hR;
            double hz = cz + Math.sin(Math.toRadians(hAngle)) * hR;
            renderGlow(ms, hx - camX, hY - camY, hz - camZ, hAngle + 90, 0.35f * anim, show * 0.06f);
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