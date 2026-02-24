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

public class TargetEspVortex extends TargetEspMode {

    private float vortexAngle = 0f, prevVortexAngle = 0f;
    private float innerAngle = 0f, prevInnerAngle = 0f;
    private float pulse = 0f, prevPulse = 0f;

    @Override
    public void onUpdate() {
        updateTarget();
        prevVortexAngle = vortexAngle; prevInnerAngle = innerAngle; prevPulse = pulse;
        vortexAngle += 8f; innerAngle -= 12f; pulse += 5f;
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

        float iVortex = MathUtil.interpolate(prevVortexAngle, vortexAngle);
        float iInner = MathUtil.interpolate(prevInnerAngle, innerAngle);
        float iPulse = MathUtil.interpolate(prevPulse, pulse);
        float p = 1f + (float) Math.sin(Math.toRadians(iPulse)) * 0.15f;

        // Внешняя воронка — 6 спиральных потоков, сходящихся к центру сверху
        for (int stream = 0; stream < 6; stream++) {
            float streamOff = stream * 60f;
            for (int i = 0; i < 25; i++) {
                float t = i / 25f;
                float angle = iVortex + streamOff + t * 540f;
                float radius = w * (2.5f - t * 2.0f) * anim * p;
                float yPos = (float)(cy + h * 1.5f * anim - t * h * 1.5f * anim);

                double px = cx + Math.cos(Math.toRadians(angle)) * radius;
                double pz = cz + Math.sin(Math.toRadians(angle)) * radius;

                float sz = (0.09f - t * 0.02f) * anim;
                float al = show * (0.3f + t * 0.4f);
                renderGlow(ms, px - camX, yPos - camY, pz - camZ, angle + stream * 20, sz, al);
            }
        }

        // Внутренний обратный вихрь (выходит снизу)
        for (int stream = 0; stream < 4; stream++) {
            float streamOff = stream * 90f;
            for (int i = 0; i < 15; i++) {
                float t = i / 15f;
                float angle = iInner + streamOff + t * 360f;
                float radius = w * (0.2f + t * 1.0f) * anim;
                float yPos = (float)(cy - 0.2f * anim + t * h * 0.8f * anim);

                double px = cx + Math.cos(Math.toRadians(angle)) * radius;
                double pz = cz + Math.sin(Math.toRadians(angle)) * radius;

                float sz = 0.06f * anim * t;
                float al = show * (1f - t) * 0.4f;
                renderGlow(ms, px - camX, yPos - camY, pz - camZ, angle + stream * 30 + 90, sz, al);
            }
        }

        // Центральный столб энергии
        for (int i = 0; i < 12; i++) {
            float t = i / 12f;
            float coreY = (float)(cy - 0.2f * anim + t * (h * 1.7f) * anim);
            float coreAngle = iInner * 2f + t * 300f;
            float coreR = w * 0.08f * anim * (float) Math.sin(t * Math.PI);
            double coreX = cx + Math.cos(Math.toRadians(coreAngle)) * coreR;
            double coreZ = cz + Math.sin(Math.toRadians(coreAngle)) * coreR;
            float intensity = (float) Math.sin(t * Math.PI);
            renderGlow(ms, coreX - camX, coreY - camY, coreZ - camZ, coreAngle, 0.13f * anim * intensity, show * intensity * 0.7f);
        }

        // Горизонтальные дисковые кольца на разных высотах
        for (int ring = 0; ring < 4; ring++) {
            float ringY = (float)(cy + ring * h * 0.4f * anim);
            float ringR = w * (2.0f - ring * 0.4f) * anim * p;
            int dots = 18 - ring * 2;
            float speed = ring % 2 == 0 ? 1f : -0.7f;
            for (int i = 0; i < dots; i++) {
                float angle = iVortex * speed + i * (360f / dots) + ring * 25f;
                double rx = cx + Math.cos(Math.toRadians(angle)) * ringR;
                double rz = cz + Math.sin(Math.toRadians(angle)) * ringR;
                renderGlow(ms, rx - camX, ringY - camY, rz - camZ, angle + ring * 30, 0.04f * anim, show * 0.35f);
            }
        }

        // Ядро
        renderGlow(ms, cx - camX, midY - camY, cz - camZ, iVortex, 0.3f * anim * p, show * 0.4f);
        renderGlow(ms, cx - camX, midY - camY, cz - camZ, iVortex + 120, 0.5f * anim, show * 0.12f);

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