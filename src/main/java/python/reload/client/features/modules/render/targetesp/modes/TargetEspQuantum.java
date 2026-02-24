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

public class TargetEspQuantum extends TargetEspMode {

    private float phaseAngle = 0f, prevPhaseAngle = 0f;
    private float glitchTimer = 0f;

    // Квантовые частицы: {x, y, z, targetX, targetY, targetZ, progress, colorOff, lifespan}
    private final List<float[]> quantumParticles = new ArrayList<>();
    // Связанные пары
    private final List<float[]> entangled = new ArrayList<>();

    @Override
    public void onUpdate() {
        updateTarget();
        prevPhaseAngle = phaseAngle;
        phaseAngle += 6f;
        glitchTimer += 1f;

        if (currentTarget != null) {
            float w = currentTarget.getWidth();
            float h = currentTarget.getHeight();

            // Спавн квантовых частиц (телепортируются)
            if (Math.random() < 0.3) {
                float fromAngle = (float)(Math.random() * 360);
                float fromR = (float)(0.5f + Math.random() * 2f) * w;
                float fromY = (float)(Math.random() * h * 1.5f);
                float toAngle = (float)(Math.random() * 360);
                float toR = (float)(0.5f + Math.random() * 2f) * w;
                float toY = (float)(Math.random() * h * 1.5f);
                quantumParticles.add(new float[]{
                        (float) Math.cos(Math.toRadians(fromAngle)) * fromR,
                        fromY,
                        (float) Math.sin(Math.toRadians(fromAngle)) * fromR,
                        (float) Math.cos(Math.toRadians(toAngle)) * toR,
                        toY,
                        (float) Math.sin(Math.toRadians(toAngle)) * toR,
                        0f, (float)(Math.random() * 360), (float)(0.03f + Math.random() * 0.04f)
                });
            }

            // Запутанные пары
            if (Math.random() < 0.15) {
                float angle1 = (float)(Math.random() * 360);
                float r1 = (float)(0.8f + Math.random() * 1.2f) * w;
                float y1 = (float)(Math.random() * h);
                entangled.add(new float[]{
                        (float) Math.cos(Math.toRadians(angle1)) * r1, y1,
                        (float) Math.sin(Math.toRadians(angle1)) * r1,
                        (float) Math.cos(Math.toRadians(angle1 + 180)) * r1, h - y1,
                        (float) Math.sin(Math.toRadians(angle1 + 180)) * r1,
                        0f, (float)(Math.random() * 360), 0.025f
                });
            }
        }

        Iterator<float[]> it = quantumParticles.iterator();
        while (it.hasNext()) { float[] p = it.next(); p[6] += p[8]; if (p[6] > 1f) it.remove(); }
        Iterator<float[]> eit = entangled.iterator();
        while (eit.hasNext()) { float[] e = eit.next(); e[6] += e[8]; if (e[6] > 1f) eit.remove(); }
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

        float iPhase = MathUtil.interpolate(prevPhaseAngle, phaseAngle);

        // Квантовые частицы — появляются/исчезают с глитчем
        for (float[] qp : quantumParticles) {
            float t = qp[6];
            // Телепортация: первая половина — на старой позиции, вторая — на новой, мигание в середине
            float px, py, pz;
            float alpha;
            if (t < 0.4f) {
                px = qp[0]; py = qp[1]; pz = qp[2];
                alpha = 1f;
            } else if (t < 0.6f) {
                // Глитч-переход
                float glitch = (t - 0.4f) / 0.2f;
                boolean blink = ((int)(glitch * 10)) % 2 == 0;
                if (blink) { px = qp[0]; py = qp[1]; pz = qp[2]; }
                else { px = qp[3]; py = qp[4]; pz = qp[5]; }
                alpha = 0.5f + (float) Math.sin(glitch * Math.PI * 5) * 0.5f;
            } else {
                px = qp[3]; py = qp[4]; pz = qp[5];
                alpha = 1f - (t - 0.6f) / 0.4f;
            }

            float sz = 0.12f * anim * Math.min(1f, alpha);
            renderGlow(ms, cx + px * anim - camX, cy + py * anim - camY, cz + pz * anim - camZ,
                    qp[7] + t * 180, sz, show * alpha * 0.7f);
        }

        // Запутанные пары — связь линией
        for (float[] ep : entangled) {
            float t = ep[6];
            float fade = (float) Math.sin(t * Math.PI);
            float pulse2 = (float) Math.sin(Math.toRadians(iPhase * 3 + ep[7])) * 0.3f + 0.7f;

            // Частица 1
            renderGlow(ms, cx + ep[0] * anim - camX, cy + ep[1] * anim - camY, cz + ep[2] * anim - camZ,
                    ep[7], 0.1f * anim * fade * pulse2, show * fade * 0.8f);
            // Частица 2
            renderGlow(ms, cx + ep[3] * anim - camX, cy + ep[4] * anim - camY, cz + ep[5] * anim - camZ,
                    ep[7] + 180, 0.1f * anim * fade * pulse2, show * fade * 0.8f);

            // Линия связи
            for (int l = 1; l < 5; l++) {
                float lt = l / 5f;
                double lx = cx + (ep[0] + (ep[3] - ep[0]) * lt) * anim;
                double ly = cy + (ep[1] + (ep[4] - ep[1]) * lt) * anim;
                double lz = cz + (ep[2] + (ep[5] - ep[2]) * lt) * anim;
                renderGlow(ms, lx - camX, ly - camY, lz - camZ, ep[7] + lt * 90, 0.03f * anim * fade, show * fade * 0.3f);
            }
        }

        // Вероятностное облако (стоячие волны)
        for (int shell = 0; shell < 3; shell++) {
            float shellR = w * (1f + shell * 0.5f) * anim;
            int dots = 16;
            for (int i = 0; i < dots; i++) {
                float angle = iPhase * (shell % 2 == 0 ? 0.5f : -0.3f) + i * (360f / dots) + shell * 20;
                float prob = (float) Math.abs(Math.sin(Math.toRadians(angle * 2 + iPhase)));
                double qx = cx + Math.cos(Math.toRadians(angle)) * shellR;
                double qz = cz + Math.sin(Math.toRadians(angle)) * shellR;
                float qy = (float)(cy + h / 2f + Math.sin(Math.toRadians(angle * 1.5f)) * h * 0.3f * anim);
                renderGlow(ms, qx - camX, qy - camY, qz - camZ, angle + shell * 40, 0.05f * anim * prob, show * prob * 0.35f);
            }
        }

        // Наблюдатель-маркер сверху
        float observerPulse = (float) Math.sin(Math.toRadians(iPhase * 2)) * 0.15f;
        renderGlow(ms, cx - camX, cy + h * 1.4f * anim - camY, cz - camZ, iPhase, 0.2f * anim * (1f + observerPulse), show * 0.7f);

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