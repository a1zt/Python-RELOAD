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

public class TargetEspButterfly extends TargetEspMode {

    // Каждая бабочка: {orbitAngle, orbitSpeed, heightPhase, heightSpeed, wingSpeed, colorOff, radius}
    private final List<float[]> butterflies = new ArrayList<>();
    private float timer = 0f, prevTimer = 0f;
    private boolean initialized = false;

    private void initButterflies() {
        if (initialized) return;
        initialized = true;
        for (int i = 0; i < 5; i++) {
            butterflies.add(new float[]{
                    i * 72f,                              // начальный угол
                    (float)(2f + Math.random() * 4f),    // скорость орбиты
                    (float)(Math.random() * 360),         // фаза высоты
                    (float)(1.5f + Math.random() * 3f),  // скорость высоты
                    (float)(10f + Math.random() * 8f),   // скорость крыльев
                    i * 72f,                              // цветовой оффсет
                    (float)(0.8f + Math.random() * 1.2f)  // множитель радиуса
            });
        }
    }

    @Override
    public void onUpdate() {
        updateTarget();
        initButterflies();
        prevTimer = timer;
        timer += 1f;

        for (float[] b : butterflies) {
            b[0] += b[1]; // угол орбиты
            b[2] += b[3]; // фаза высоты
        }
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

        float iTimer = MathUtil.interpolate(prevTimer, timer);

        for (float[] b : butterflies) {
            float orbAngle = b[0];
            float orbR = w * b[6] * 1.5f * anim;
            float heightOff = (float) Math.sin(Math.toRadians(b[2])) * h * 0.4f * anim;
            float wingAngle = (float) Math.sin(Math.toRadians(iTimer * b[4]));

            double bx = cx + Math.cos(Math.toRadians(orbAngle)) * orbR;
            double by = midY + heightOff;
            double bz = cz + Math.sin(Math.toRadians(orbAngle)) * orbR;

            // Тело бабочки
            renderGlow(ms, bx - camX, by - camY, bz - camZ, b[5] + orbAngle, 0.1f * anim, show * 0.8f);

            // Крылья — 2 пары
            float bodyDir = (float) Math.toRadians(orbAngle + 90);
            for (int wing = 0; wing < 2; wing++) {
                float side = wing == 0 ? 1f : -1f;

                // Верхнее крыло (большое)
                for (int p = 0; p < 4; p++) {
                    float t = (p + 1) / 4f;
                    float wingSpread = t * w * 0.4f * anim * Math.abs(wingAngle);
                    float wingUp = t * 0.15f * anim * wingAngle * side;

                    double wx = bx + Math.cos(bodyDir) * wingSpread * side;
                    double wy = by + wingUp + t * 0.08f * anim;
                    double wz = bz + Math.sin(bodyDir) * wingSpread * side;

                    float sz = (0.1f - t * 0.01f) * anim;
                    renderGlow(ms, wx - camX, wy - camY, wz - camZ, b[5] + t * 60 + wing * 90, sz, show * (1f - t * 0.2f) * 0.6f);
                }

                // Нижнее крыло (маленькое)
                for (int p = 0; p < 3; p++) {
                    float t = (p + 1) / 3f;
                    float wingSpread = t * w * 0.25f * anim * Math.abs(wingAngle);
                    float wingDown = -t * 0.12f * anim * wingAngle * side;

                    double wx = bx + Math.cos(bodyDir) * wingSpread * side;
                    double wy = by + wingDown - t * 0.05f * anim;
                    double wz = bz + Math.sin(bodyDir) * wingSpread * side;

                    float sz = (0.07f - t * 0.01f) * anim;
                    renderGlow(ms, wx - camX, wy - camY, wz - camZ, b[5] + t * 40 + wing * 90 + 45, sz, show * (1f - t * 0.2f) * 0.45f);
                }
            }

            // Пыльца за бабочкой
            for (int d = 0; d < 3; d++) {
                float delay = d * 8f;
                float dustAngle = orbAngle - delay;
                float dustR = orbR * 0.98f;
                double dx = cx + Math.cos(Math.toRadians(dustAngle)) * dustR;
                double dy = by - 0.05f * d * anim;
                double dz = cz + Math.sin(Math.toRadians(dustAngle)) * dustR;
                float dustFade = 1f - d / 4f;
                renderGlow(ms, dx - camX, dy - camY, dz - camZ, b[5] + d * 20, 0.04f * anim * dustFade, show * dustFade * 0.25f);
            }
        }

        // Цветочное кольцо у ног
        for (int i = 0; i < 16; i++) {
            float angle = iTimer * 0.5f + i * 22.5f;
            float r = w * 0.9f * anim;
            float bloom = (float) Math.sin(Math.toRadians(iTimer * 3 + i * 22.5f)) * 0.2f + 0.8f;
            double fx = cx + Math.cos(Math.toRadians(angle)) * r;
            double fz = cz + Math.sin(Math.toRadians(angle)) * r;
            renderGlow(ms, fx - camX, cy - camY, fz - camZ, angle + 30, 0.05f * anim * bloom, show * bloom * 0.35f);
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