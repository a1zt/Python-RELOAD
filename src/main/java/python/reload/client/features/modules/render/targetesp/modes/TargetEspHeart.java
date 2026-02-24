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
import java.util.Iterator;
import java.util.List;

public class TargetEspHeart extends TargetEspMode {

    private float heartBeat = 0f, prevHeartBeat = 0f;
    private float rotAngle = 0f, prevRotAngle = 0f;
    private float floatPhase = 0f, prevFloatPhase = 0f;
    private final List<float[]> loveParticles = new ArrayList<>();

    @Override
    public void onUpdate() {
        updateTarget();
        prevHeartBeat = heartBeat; prevRotAngle = rotAngle; prevFloatPhase = floatPhase;
        heartBeat += 8f; rotAngle += 3f; floatPhase += 4f;

        if (currentTarget != null && Math.random() < 0.25) {
            loveParticles.add(new float[]{
                    (float)(Math.random() * 360), (float)(0.3f + Math.random() * 1f),
                    0f, (float)(Math.random() * 360), (float)(0.015f + Math.random() * 0.02f)
            });
        }
        Iterator<float[]> it = loveParticles.iterator();
        while (it.hasNext()) { float[] p = it.next(); p[2] += p[4]; if (p[2] > 1.3f) it.remove(); }
    }

    // Параметрическая формула сердца
    private double[] heartPoint(float t, float scale) {
        double x = 16 * Math.pow(Math.sin(t), 3);
        double y = 13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t);
        return new double[]{x * scale / 16.0, y * scale / 16.0};
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

        float iBeat = MathUtil.interpolate(prevHeartBeat, heartBeat);
        float iRot = MathUtil.interpolate(prevRotAngle, rotAngle);
        float iFloat = MathUtil.interpolate(prevFloatPhase, floatPhase);

        // Пульсация сердцебиения
        float beat = 1f + (float) Math.abs(Math.sin(Math.toRadians(iBeat))) * 0.25f;
        float heartScale = w * 0.6f * anim * beat;

        // Плавающее смещение
        float floatY = (float) Math.sin(Math.toRadians(iFloat)) * 0.1f * anim;

        // Вращение сердца
        float cosR = (float) Math.cos(Math.toRadians(iRot));
        float sinR = (float) Math.sin(Math.toRadians(iRot));

        // Сердце из частиц (контур)
        int heartPoints = 40;
        for (int i = 0; i < heartPoints; i++) {
            float t = (float)(i * 2 * Math.PI / heartPoints);
            double[] hp = heartPoint(t, heartScale);

            // Вращение вокруг Y
            double hx = hp[0] * cosR;
            double hz = hp[0] * sinR;
            double hy = hp[1];

            renderGlow(ms, cx + hx - camX, midY + hy + floatY - camY, cz + hz - camZ,
                    iRot + i * 9, 0.08f * anim * beat, show * 0.7f);
        }

        // Заливка сердца (внутренние точки)
        for (int layer = 1; layer <= 3; layer++) {
            float innerScale = heartScale * (1f - layer * 0.25f);
            int innerPoints = heartPoints - layer * 8;
            for (int i = 0; i < innerPoints; i++) {
                float t = (float)(i * 2 * Math.PI / innerPoints);
                double[] hp = heartPoint(t, innerScale);
                double hx = hp[0] * cosR;
                double hz = hp[0] * sinR;
                renderGlow(ms, cx + hx - camX, midY + hp[1] + floatY - camY, cz + hz - camZ,
                        iRot + i * 12 + layer * 30, 0.06f * anim * beat, show * (0.2f - layer * 0.04f));
            }
        }

        // Ореол сердцебиения (расширяющиеся кольца)
        for (int wave = 0; wave < 2; wave++) {
            float progress = ((iBeat + wave * 180) % 360) / 360f;
            float waveScale = heartScale * (1f + progress * 0.8f);
            float waveAlpha = show * (1f - progress) * (1f - progress) * 0.3f;
            int wavePoints = 20;
            for (int i = 0; i < wavePoints; i++) {
                float t = (float)(i * 2 * Math.PI / wavePoints);
                double[] hp = heartPoint(t, waveScale);
                double hx = hp[0] * cosR;
                double hz = hp[0] * sinR;
                renderGlow(ms, cx + hx - camX, midY + hp[1] + floatY - camY, cz + hz - camZ,
                        iRot + i * 18 + wave * 45, 0.04f * anim * (1f - progress), waveAlpha);
            }
        }

        // Маленькие сердечки (love particles), поднимающиеся
        for (float[] lp : loveParticles) {
            float prog = lp[2];
            float lpAngle = lp[0];
            float lpR = lp[1] * w * anim;
            float fade = Math.max(0, 1f - prog / 1.3f);
            double lpx = cx + Math.cos(Math.toRadians(lpAngle)) * lpR * (1f - prog * 0.3f);
            double lpy = midY + prog * h * 1.5f;
            double lpz = cz + Math.sin(Math.toRadians(lpAngle)) * lpR * (1f - prog * 0.3f);
            renderGlow(ms, lpx - camX, lpy - camY, lpz - camZ, lp[3] + prog * 150, 0.07f * anim * fade, show * fade * fade * 0.5f);
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