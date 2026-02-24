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

public class TargetEspCrystalsss extends TargetEspMode {

    private float rotY = 0f, prevRotY = 0f;
    private float rotX = 0f, prevRotX = 0f;
    private float pulse = 0f, prevPulse = 0f;
    private float shimmer = 0f, prevShimmer = 0f;

    @Override
    public void onUpdate() {
        updateTarget();
        prevRotY = rotY; prevRotX = rotX; prevPulse = pulse; prevShimmer = shimmer;
        rotY += 4f; rotX += 2.5f; pulse += 5f; shimmer += 8f;
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

        float iRotY = MathUtil.interpolate(prevRotY, rotY);
        float iRotX = MathUtil.interpolate(prevRotX, rotX);
        float iPulse = MathUtil.interpolate(prevPulse, pulse);
        float iShimmer = MathUtil.interpolate(prevShimmer, shimmer);
        float p = 1f + (float) Math.sin(Math.toRadians(iPulse)) * 0.15f;

        // Октаэдр — 6 вершин
        float scale = w * 1.3f * anim * p;
        float vert = h * 0.8f * anim * p;
        double[][] vertices = {
                {0, vert, 0}, {0, -vert, 0},
                {scale, 0, 0}, {-scale, 0, 0},
                {0, 0, scale}, {0, 0, -scale}
        };

        // Вращение вершин
        double cosY = Math.cos(Math.toRadians(iRotY)), sinY = Math.sin(Math.toRadians(iRotY));
        double cosX = Math.cos(Math.toRadians(iRotX)), sinX = Math.sin(Math.toRadians(iRotX));
        double[][] rotated = new double[6][3];
        for (int i = 0; i < 6; i++) {
            double x = vertices[i][0], y = vertices[i][1], z = vertices[i][2];
            double rx = x * cosY - z * sinY;
            double rz = x * sinY + z * cosY;
            double ry = y * cosX - rz * sinX;
            rz = y * sinX + rz * cosX;
            rotated[i] = new double[]{rx + cx, ry + midY, rz + cz};
        }

        // Вершинные частицы (яркие)
        for (int i = 0; i < 6; i++) {
            float shimmerOff = (float) Math.sin(Math.toRadians(iShimmer + i * 60)) * 0.3f + 0.7f;
            renderGlow(ms, rotated[i][0] - camX, rotated[i][1] - camY, rotated[i][2] - camZ,
                    iRotY + i * 60, 0.18f * anim * shimmerOff, show * 0.9f);
            renderGlow(ms, rotated[i][0] - camX, rotated[i][1] - camY, rotated[i][2] - camZ,
                    iRotY + i * 60 + 30, 0.3f * anim * shimmerOff, show * 0.2f);
        }

        // Рёбра — частицы вдоль линий между вершинами
        int[][] edges = {{0,2},{0,3},{0,4},{0,5},{1,2},{1,3},{1,4},{1,5},{2,4},{4,3},{3,5},{5,2}};
        for (int[] edge : edges) {
            double[] a = rotated[edge[0]], b = rotated[edge[1]];
            int edgeDots = 6;
            for (int d = 1; d < edgeDots; d++) {
                float t = d / (float) edgeDots;
                double ex = a[0] + (b[0] - a[0]) * t;
                double ey = a[1] + (b[1] - a[1]) * t;
                double ez = a[2] + (b[2] - a[2]) * t;
                float edgeShimmer = (float) Math.sin(Math.toRadians(iShimmer * 2 + t * 360 + edge[0] * 45)) * 0.3f + 0.7f;
                renderGlow(ms, ex - camX, ey - camY, ez - camZ, iRotY + t * 180, 0.05f * anim * edgeShimmer, show * 0.5f);
            }
        }

        // Грани — центральные частицы треугольников
        int[][] faces = {{0,2,4},{0,4,3},{0,3,5},{0,5,2},{1,2,4},{1,4,3},{1,3,5},{1,5,2}};
        for (int[] face : faces) {
            double fcx = (rotated[face[0]][0] + rotated[face[1]][0] + rotated[face[2]][0]) / 3;
            double fcy = (rotated[face[0]][1] + rotated[face[1]][1] + rotated[face[2]][1]) / 3;
            double fcz = (rotated[face[0]][2] + rotated[face[1]][2] + rotated[face[2]][2]) / 3;
            float fShim = (float) Math.sin(Math.toRadians(iShimmer + face[0] * 45 + face[1] * 90)) * 0.4f + 0.6f;
            renderGlow(ms, fcx - camX, fcy - camY, fcz - camZ, iRotY + face[0] * 30 + face[1] * 60, 0.06f * anim * fShim, show * 0.25f);
        }

        // Внутреннее свечение
        renderGlow(ms, cx - camX, midY - camY, cz - camZ, iRotY, 0.4f * anim * p, show * 0.2f);

        // Парящие осколки вокруг
        for (int i = 0; i < 8; i++) {
            float fAngle = iRotY * 0.7f + i * 45f;
            float fR = w * 2.0f * anim;
            float fY = (float)(midY + Math.sin(Math.toRadians(fAngle * 1.3f + iPulse)) * h * 0.3f * anim);
            double fx = cx + Math.cos(Math.toRadians(fAngle)) * fR;
            double fz = cz + Math.sin(Math.toRadians(fAngle)) * fR;
            float fShim = (float) Math.sin(Math.toRadians(iShimmer * 1.5f + i * 45)) * 0.3f + 0.7f;
            renderGlow(ms, fx - camX, fY - camY, fz - camZ, fAngle, 0.06f * anim * fShim, show * 0.45f);
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
        float a2 = Math.min(1f, Math.max(0f, alpha));
        float[] c = ColorUtil.normalize(ColorUtil.setAlpha(UIColors.gradient((int) colorAngle), (int)(255 * a2)));
        buffer.vertex(matrix, -size, size, 0).texture(0f, 1f).color(c[0], c[1], c[2], c[3]);
        buffer.vertex(matrix, size, size, 0).texture(1f, 1f).color(c[0], c[1], c[2], c[3]);
        buffer.vertex(matrix, size, -size, 0).texture(1f, 0f).color(c[0], c[1], c[2], c[3]);
        buffer.vertex(matrix, -size, -size, 0).texture(0f, 0f).color(c[0], c[1], c[2], c[3]);
        BufferRenderer.drawWithGlobalProgram(buffer.end()); stack.pop();
    }
}