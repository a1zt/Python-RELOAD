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

public class TargetEspTornado extends TargetEspMode {

    private float tornadoAngle = 0f, prevTornadoAngle = 0f;
    private float swirlPhase = 0f, prevSwirlPhase = 0f;
    private final List<float[]> flyingDebris = new ArrayList<>();

    @Override
    public void onUpdate() {
        updateTarget();
        prevTornadoAngle = tornadoAngle;
        prevSwirlPhase = swirlPhase;
        tornadoAngle += 14f;
        swirlPhase += 5f;

        if (currentTarget != null && Math.random() < 0.5) {
            flyingDebris.add(new float[]{
                    (float)(Math.random() * 360), (float)(0.5f + Math.random() * 2f),
                    (float)(Math.random() * 0.3f), (float)(Math.random() * 360),
                    (float)(0.015f + Math.random() * 0.02f), (float)(Math.random() * 0.5f + 0.5f)
            });
        }
        Iterator<float[]> it = flyingDebris.iterator();
        while (it.hasNext()) {
            float[] d = it.next();
            d[2] += d[4]; // прогресс подъёма
            d[0] += 8f;   // вращение
            d[1] -= d[4] * 1.5f; // сужение радиуса
            if (d[2] > 1.3f || d[1] < 0.05f) it.remove();
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
        double camX = mc.getEntityRenderDispatcher().camera.getPos().getX();
        double camY = mc.getEntityRenderDispatcher().camera.getPos().getY();
        double camZ = mc.getEntityRenderDispatcher().camera.getPos().getZ();

        float iTornado = MathUtil.interpolate(prevTornadoAngle, tornadoAngle);
        float iSwirl = MathUtil.interpolate(prevSwirlPhase, swirlPhase);

        // Основная воронка торнадо — 4 спиральных потока
        for (int stream = 0; stream < 4; stream++) {
            float streamOff = stream * 90f;
            for (int i = 0; i < 35; i++) {
                float t = i / 35f;
                float angle = iTornado + streamOff + t * 720f;

                // Воронка: широкая внизу, узкая вверху
                float radius = w * (1.8f - t * 1.3f) * anim;
                float yPos = (float)(cy + t * h * 2.2f * anim);

                // Турбулентность
                float turbX = (float) Math.sin(Math.toRadians(angle * 2.3f + iSwirl)) * 0.08f * anim;
                float turbZ = (float) Math.cos(Math.toRadians(angle * 1.7f + iSwirl)) * 0.08f * anim;

                double px = cx + Math.cos(Math.toRadians(angle)) * radius + turbX;
                double pz = cz + Math.sin(Math.toRadians(angle)) * radius + turbZ;

                float sz = (0.1f - t * 0.04f) * anim;
                float al = show * (0.5f + (1f - t) * 0.3f);
                if (sz > 0.01f) renderGlow(ms, px - camX, yPos - camY, pz - camZ, angle + stream * 30, sz, al);
            }
        }

        // Пыль у основания
        for (int i = 0; i < 24; i++) {
            float angle = iTornado * 0.5f + i * 15f;
            float r = w * (1.8f + (float) Math.sin(Math.toRadians(angle * 2 + iSwirl)) * 0.4f) * anim;
            double dx = cx + Math.cos(Math.toRadians(angle)) * r;
            double dz = cz + Math.sin(Math.toRadians(angle)) * r;
            float yOff = (float)(Math.random() * 0.1f) * anim;
            renderGlow(ms, dx - camX, cy + yOff - camY, dz - camZ, angle, 0.07f * anim, show * 0.35f);
        }

        // Летающие обломки
        for (float[] d : flyingDebris) {
            float dAngle = d[0];
            float dR = d[1] * w * 0.4f * anim;
            float dY = (float)(cy + d[2] * h * 2.2f * anim);
            double dx = cx + Math.cos(Math.toRadians(dAngle)) * dR;
            double dz = cz + Math.sin(Math.toRadians(dAngle)) * dR;
            float fade = Math.max(0, 1f - d[2] / 1.3f) * d[5];
            renderGlow(ms, dx - camX, dY - camY, dz - camZ, d[3], 0.08f * anim * fade, show * fade * 0.7f);
        }

        // Глаз бури (центральный столб)
        for (int i = 0; i < 8; i++) {
            float t = i / 8f;
            float eyeY = (float)(cy + t * h * 2.2f * anim);
            float eyeR = w * 0.1f * anim * (float) Math.sin(t * Math.PI);
            float eyeAngle = iTornado * 3f + t * 200;
            double ex = cx + Math.cos(Math.toRadians(eyeAngle)) * eyeR;
            double ez = cz + Math.sin(Math.toRadians(eyeAngle)) * eyeR;
            renderGlow(ms, ex - camX, eyeY - camY, ez - camZ, eyeAngle, 0.12f * anim, show * 0.45f);
        }

        // Верхушка — рассеивание
        for (int i = 0; i < 12; i++) {
            float angle = iTornado * 0.8f + i * 30f;
            float topY = (float)(cy + h * 2.2f * anim);
            float topR = w * (0.5f + (float) Math.sin(Math.toRadians(angle + iSwirl)) * 0.5f) * anim;
            double tx = cx + Math.cos(Math.toRadians(angle)) * topR;
            double tz = cz + Math.sin(Math.toRadians(angle)) * topR;
            renderGlow(ms, tx - camX, topY - camY, tz - camZ, angle + 90, 0.06f * anim, show * 0.3f);
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