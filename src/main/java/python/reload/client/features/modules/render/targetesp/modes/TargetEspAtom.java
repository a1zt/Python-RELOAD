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

public class TargetEspAtom extends TargetEspMode {

    private float electronAngle = 0f, prevElectronAngle = 0f;
    private float nucleusPulse = 0f, prevNucleusPulse = 0f;
    private final List<double[]> electronTrails = new ArrayList<>();

    @Override
    public void onUpdate() {
        updateTarget();
        prevElectronAngle = electronAngle;
        prevNucleusPulse = nucleusPulse;
        electronAngle += 10f;
        nucleusPulse += 6f;
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

        float iElec = MathUtil.interpolate(prevElectronAngle, electronAngle);
        float iNuc = MathUtil.interpolate(prevNucleusPulse, nucleusPulse);
        float pulse = 1f + (float) Math.sin(Math.toRadians(iNuc)) * 0.2f;

        // Ядро — нуклоны (протоны и нейтроны)
        renderGlow(ms, cx - camX, midY - camY, cz - camZ, iNuc, 0.35f * anim * pulse, show * 0.5f);
        renderGlow(ms, cx - camX, midY - camY, cz - camZ, iNuc + 90, 0.55f * anim, show * 0.15f);
        renderGlow(ms, cx - camX, midY - camY, cz - camZ, iNuc + 180, 0.75f * anim, show * 0.06f);

        // Мини-нуклоны вокруг ядра
        for (int i = 0; i < 6; i++) {
            float nAngle = iNuc * 1.5f + i * 60f;
            float nR = 0.15f * anim * pulse;
            double nx = cx + Math.cos(Math.toRadians(nAngle)) * nR;
            double ny = midY + Math.sin(Math.toRadians(nAngle * 1.3f)) * nR * 0.7f;
            double nz = cz + Math.sin(Math.toRadians(nAngle)) * nR;
            renderGlow(ms, nx - camX, ny - camY, nz - camZ, nAngle + 30, 0.1f * anim, show * 0.6f);
        }

        // 3 электронных орбиты под разными углами наклона
        float[][] orbitalTilts = {{0f, 1f, 0f, 1f}, {0.9f, 0.45f, 0f, 1f}, {0f, 0.45f, 0.9f, 1f}};
        List<double[]> currentElectrons = new ArrayList<>();

        for (int orbit = 0; orbit < 3; orbit++) {
            float orbitR = w * (1.3f + orbit * 0.3f) * anim;
            float tiltX = orbitalTilts[orbit][0];
            float tiltY = orbitalTilts[orbit][1];
            float tiltZ = orbitalTilts[orbit][2];
            float speed = orbit % 2 == 0 ? 1f : -0.8f;

            // Орбитальный путь (пунктирный)
            for (int i = 0; i < 28; i++) {
                float t = i / 28f;
                float angle = t * 360f + iElec * 0.3f * speed;
                float rad = (float) Math.toRadians(angle);
                double ox = cx + Math.cos(rad) * orbitR * tiltY + Math.sin(rad) * orbitR * tiltX * 0.3f;
                double oy = midY + Math.sin(rad) * orbitR * tiltX;
                double oz = cz + Math.sin(rad) * orbitR * tiltY + Math.cos(rad) * orbitR * tiltZ * 0.3f;
                renderGlow(ms, ox - camX, oy - camY, oz - camZ, angle + orbit * 45, 0.025f * anim, show * 0.2f);
            }

            // Электроны (2 на орбиту)
            for (int e = 0; e < 2; e++) {
                float eAngle = iElec * speed + e * 180f + orbit * 60f;
                float rad = (float) Math.toRadians(eAngle);
                double ex = cx + Math.cos(rad) * orbitR * tiltY + Math.sin(rad) * orbitR * tiltX * 0.3f;
                double ey = midY + Math.sin(rad) * orbitR * tiltX;
                double ez = cz + Math.sin(rad) * orbitR * tiltY + Math.cos(rad) * orbitR * tiltZ * 0.3f;

                // Электрон
                renderGlow(ms, ex - camX, ey - camY, ez - camZ, eAngle + orbit * 80, 0.18f * anim, show * 0.9f);
                // Ореол электрона
                renderGlow(ms, ex - camX, ey - camY, ez - camZ, eAngle + orbit * 80 + 30, 0.3f * anim, show * 0.2f);

                currentElectrons.add(new double[]{ex, ey, ez, eAngle + orbit * 80});
            }
        }

        // Трейлы электронов
        electronTrails.addAll(currentElectrons);
        if (electronTrails.size() > 6 * 12) electronTrails.subList(0, electronTrails.size() - 6 * 12).clear();
        for (int i = 0; i < electronTrails.size(); i++) {
            double[] tp = electronTrails.get(i);
            float progress = (i + 1f) / (electronTrails.size() + 1f);
            float tSz = 0.08f * progress * anim;
            float tAl = show * progress * 0.25f;
            if (tSz > 0.01f) renderGlow(ms, tp[0] - camX, tp[1] - camY, tp[2] - camZ, (float) tp[3], tSz, tAl);
        }

        // Энергетические волны от ядра
        for (int wave = 0; wave < 2; wave++) {
            float progress = ((iNuc + wave * 180) % 360) / 360f;
            float wR = w * 0.2f + progress * w * 2.5f;
            float wAlpha = show * (1f - progress) * (1f - progress) * 0.25f;
            for (int i = 0; i < 16; i++) {
                float angle = i * (360f / 16f) + wave * 15f;
                double wx = cx + Math.cos(Math.toRadians(angle)) * wR * anim;
                double wz = cz + Math.sin(Math.toRadians(angle)) * wR * anim;
                renderGlow(ms, wx - camX, midY - camY, wz - camZ, angle + wave * 50, 0.03f * anim * (1f - progress), wAlpha);
            }
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