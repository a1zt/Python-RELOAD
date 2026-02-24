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

public class TargetEspFireworks extends TargetEspMode {

    private float timer = 0f;

    // Каждый фейерверк: {originX, originY, originZ, progress, colorOffset, numParticles, ...particle directions}
    private final List<float[]> fireworks = new ArrayList<>();
    // Трейлы запуска: {x, y, z, progress, colorOff}
    private final List<float[]> rockets = new ArrayList<>();

    @Override
    public void onUpdate() {
        updateTarget();
        timer += 1f;

        if (currentTarget != null && timer % 20 < 1) { // новый фейерверк каждые 20 тиков
            float angle = (float)(Math.random() * 360);
            float radius = currentTarget.getWidth() * (0.5f + (float)(Math.random() * 1.5f));
            float ox = (float)(Math.cos(Math.toRadians(angle)) * radius);
            float oz = (float)(Math.sin(Math.toRadians(angle)) * radius);
            float oy = currentTarget.getHeight() * (0.8f + (float)(Math.random() * 0.8f));

            rockets.add(new float[]{ox, 0, oz, 0f, (float)(Math.random() * 360), oy});
        }

        // Обновляем ракеты
        Iterator<float[]> rit = rockets.iterator();
        while (rit.hasNext()) {
            float[] r = rit.next();
            r[3] += 0.06f;
            if (r[3] >= 1f) {
                rit.remove();
                // Создаём взрыв
                int particles = 16 + (int)(Math.random() * 12);
                float[] fw = new float[6 + particles * 3];
                fw[0] = r[0]; fw[1] = r[5]; fw[2] = r[2];
                fw[3] = 0f; fw[4] = r[4]; fw[5] = particles;
                for (int i = 0; i < particles; i++) {
                    float theta = (float)(Math.random() * Math.PI * 2);
                    float phi = (float)(Math.random() * Math.PI);
                    fw[6 + i * 3] = (float)(Math.cos(theta) * Math.sin(phi));
                    fw[6 + i * 3 + 1] = (float)(Math.cos(phi));
                    fw[6 + i * 3 + 2] = (float)(Math.sin(theta) * Math.sin(phi));
                }
                fireworks.add(fw);
            }
        }

        // Обновляем взрывы
        Iterator<float[]> fit = fireworks.iterator();
        while (fit.hasNext()) {
            float[] fw = fit.next();
            fw[3] += 0.025f;
            if (fw[3] > 1.2f) fit.remove();
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
        double camX = mc.getEntityRenderDispatcher().camera.getPos().getX();
        double camY = mc.getEntityRenderDispatcher().camera.getPos().getY();
        double camZ = mc.getEntityRenderDispatcher().camera.getPos().getZ();

        // Рендер ракет (поднимающихся)
        for (float[] r : rockets) {
            float t = r[3];
            double rx = cx + r[0] * anim;
            double ry = cy + t * r[5] * anim;
            double rz = cz + r[2] * anim;
            renderGlow(ms, rx - camX, ry - camY, rz - camZ, r[4], 0.12f * anim, show * 0.8f);
            // Хвост ракеты
            for (int i = 1; i <= 5; i++) {
                float tt = Math.max(0, t - i * 0.012f);
                double try2 = cy + tt * r[5] * anim;
                float tAlpha = show * (1f - i / 6f) * 0.4f;
                renderGlow(ms, rx - camX, try2 - camY, rz - camZ, r[4] + i * 15, 0.06f * anim, tAlpha);
            }
        }

        // Рендер взрывов
        for (float[] fw : fireworks) {
            float progress = fw[3];
            int particles = (int) fw[5];
            float radius = progress * currentTarget.getWidth() * 2f * anim;
            float gravity = progress * progress * 0.3f * anim;
            float fade = Math.max(0, 1f - progress / 1.2f);

            for (int i = 0; i < particles; i++) {
                float dx = fw[6 + i * 3] * radius;
                float dy = fw[6 + i * 3 + 1] * radius - gravity;
                float dz = fw[6 + i * 3 + 2] * radius;

                double px = cx + fw[0] * anim + dx;
                double py = cy + fw[1] * anim + dy;
                double pz = cz + fw[2] * anim + dz;

                float sz = 0.1f * anim * fade;
                float al = show * fade * fade * 0.7f;
                renderGlow(ms, px - camX, py - camY, pz - camZ, fw[4] + i * 25, sz, al);

                // Мини-трейл каждой частицы
                for (int t = 1; t <= 3; t++) {
                    float trailP = Math.max(0, progress - t * 0.008f);
                    float trailR = trailP * currentTarget.getWidth() * 2f * anim;
                    float trailG = trailP * trailP * 0.3f * anim;
                    double tx = cx + fw[0] * anim + fw[6 + i * 3] * trailR;
                    double ty = cy + fw[1] * anim + fw[6 + i * 3 + 1] * trailR - trailG;
                    double tz = cz + fw[2] * anim + fw[6 + i * 3 + 2] * trailR;
                    renderGlow(ms, tx - camX, ty - camY, tz - camZ, fw[4] + i * 25, 0.04f * anim * fade, show * fade * 0.2f);
                }
            }

            // Центральная вспышка
            if (progress < 0.3f) {
                float flashAlpha = (1f - progress / 0.3f);
                renderGlow(ms, cx + fw[0] * anim - camX, cy + fw[1] * anim - camY, cz + fw[2] * anim - camZ,
                        fw[4], 0.5f * anim * flashAlpha, show * flashAlpha * 0.4f);
            }
        }

        // Базовый маркер цели (скромное кольцо у ног)
        for (int i = 0; i < 16; i++) {
            float angle = timer * 2f + i * (360f / 16f);
            float r = currentTarget.getWidth() * 0.8f * anim;
            double bx = cx + Math.cos(Math.toRadians(angle)) * r;
            double bz = cz + Math.sin(Math.toRadians(angle)) * r;
            renderGlow(ms, bx - camX, cy - camY, bz - camZ, angle, 0.04f * anim, show * 0.3f);
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