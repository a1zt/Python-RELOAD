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

public class TargetEspSakura extends TargetEspMode {

    private float windPhase = 0f, prevWindPhase = 0f;
    private float bloomPhase = 0f, prevBloomPhase = 0f;

    // Лепестки: {startAngle, startHeight, progress, swaySpeed, fallSpeed, radius, colorOff, swayAmplitude}
    private final List<float[]> petals = new ArrayList<>();

    @Override
    public void onUpdate() {
        updateTarget();
        prevWindPhase = windPhase; prevBloomPhase = bloomPhase;
        windPhase += 2f; bloomPhase += 4f;

        // Спавн лепестков
        if (currentTarget != null && Math.random() < 0.45) {
            petals.add(new float[]{
                    (float)(Math.random() * 360),
                    1.2f + (float)(Math.random() * 0.8f),
                    0f,
                    (float)(1.5f + Math.random() * 3f),
                    (float)(0.008f + Math.random() * 0.012f),
                    (float)(0.3f + Math.random() * 1.5f),
                    (float)(Math.random() * 360),
                    (float)(0.2f + Math.random() * 0.4f)
            });
        }

        Iterator<float[]> it = petals.iterator();
        while (it.hasNext()) { float[] p = it.next(); p[2] += p[4]; if (p[2] > 1.5f) it.remove(); }
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

        float iWind = MathUtil.interpolate(prevWindPhase, windPhase);
        float iBloom = MathUtil.interpolate(prevBloomPhase, bloomPhase);

        // Падающие лепестки
        for (float[] petal : petals) {
            float prog = petal[2];
            float sway = (float) Math.sin(Math.toRadians(prog * petal[3] * 360 + petal[0])) * petal[7];
            float windPush = (float) Math.sin(Math.toRadians(iWind + petal[0])) * 0.3f;

            float petalR = petal[5] * w * anim;
            float petalAngle = petal[0] + prog * petal[3] * 60 + sway * 30;
            float petalY = (float)(cy + petal[1] * h * anim - prog * h * 1.8f * anim);

            double px = cx + Math.cos(Math.toRadians(petalAngle)) * (petalR + windPush * anim);
            double pz = cz + Math.sin(Math.toRadians(petalAngle)) * (petalR + windPush * anim * 0.5f);

            float fade = prog < 0.1f ? prog / 0.1f : prog > 1.2f ? Math.max(0, (1.5f - prog) / 0.3f) : 1f;
            float sz = 0.1f * anim * fade;
            float al = show * fade * 0.65f;

            if (sz > 0.01f && al > 0.01f)
                renderGlow(ms, px - camX, petalY - camY, pz - camZ, petal[6] + prog * 100, sz, al);
        }

        // Ветка сакуры (кольцо цветов наверху)
        float crownY = (float)(cy + h * 1.3f * anim);
        for (int i = 0; i < 16; i++) {
            float angle = iBloom * 0.3f + i * (360f / 16f);
            float r = w * 1.0f * anim;
            float bob = (float) Math.sin(Math.toRadians(angle * 2 + iWind)) * 0.05f * anim;
            double bx = cx + Math.cos(Math.toRadians(angle)) * r;
            double bz = cz + Math.sin(Math.toRadians(angle)) * r;
            float bloom = (float) Math.sin(Math.toRadians(iBloom + i * 22.5f)) * 0.2f + 0.8f;
            renderGlow(ms, bx - camX, crownY + bob - camY, bz - camZ, angle, 0.09f * anim * bloom, show * bloom * 0.5f);
        }

        // Ствол дерева (вертикальная линия)
        for (int i = 0; i < 6; i++) {
            float t = i / 6f;
            float trunkY = (float)(cy + t * h * 1.3f * anim);
            float sway2 = (float) Math.sin(Math.toRadians(iWind * 0.5f + t * 60)) * 0.02f * anim;
            renderGlow(ms, cx + sway2 - camX, trunkY - camY, cz - camZ, iBloom + t * 30, 0.04f * anim, show * 0.3f);
        }

        // Упавшие лепестки у ног (кольцо на земле)
        for (int i = 0; i < 12; i++) {
            float angle = i * 30f + iWind * 0.1f;
            float r = w * (0.5f + (float) Math.sin(Math.toRadians(angle * 3 + iBloom)) * 0.3f) * anim;
            double gx = cx + Math.cos(Math.toRadians(angle)) * r;
            double gz = cz + Math.sin(Math.toRadians(angle)) * r;
            renderGlow(ms, gx - camX, cy - camY, gz - camZ, angle + 60, 0.05f * anim, show * 0.25f);
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