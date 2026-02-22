package python.reload.client.features.modules.render.targetesp.modes;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import python.reload.api.event.events.render.Render3DEvent;
import python.reload.api.system.files.FileUtil;
import python.reload.api.utils.color.ColorUtil;
import python.reload.api.utils.color.UIColors;
import python.reload.api.utils.render.RenderUtil;
import python.reload.client.features.modules.render.targetesp.TargetEspMode;
import python.reload.client.features.modules.render.targetesp.TargetEspModule;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
//crystal modes
public class TargetEspCrystals extends TargetEspMode {

    private final List<CrystalData> crystals = new ArrayList<>();
    private final Random random = new Random();
    private Object lastTarget = null;
    private float animationTick = 0f;

    @Override
    public void onUpdate() {
        if (aura().target != null) {
            currentTarget = aura().target;
        } else if (mc.targetedEntity instanceof net.minecraft.entity.LivingEntity living) {
            currentTarget = living;
        }

        animationTick += TargetEspModule.instance.getCrystalsSpeed();

        if (currentTarget != lastTarget) {
            crystals.clear();
            if (currentTarget != null) generateCrystals();
            lastTarget = currentTarget;
        }
    }

    @Override
    public void onRender3D(Render3DEvent.Render3DEventData event) {
        if (currentTarget == null || !canDraw() || crystals.isEmpty()) return;

        MatrixStack ms = event.matrixStack();
        float alphaAnim = (float) showAnimation.getValue();
        RenderUtil.WORLD.startRender(ms);

        double tx = getTargetX() - mc.getEntityRenderDispatcher().camera.getPos().getX();
        double ty = getTargetY() - mc.getEntityRenderDispatcher().camera.getPos().getY();
        double tz = getTargetZ() - mc.getEntityRenderDispatcher().camera.getPos().getZ();

        float orbitSpeed = 0.02f * TargetEspModule.instance.getCrystalsSpeed();

        for (CrystalData crystal : crystals) {
            float floatAnim = (float) Math.sin(Math.toRadians(animationTick + crystal.index * 35)) * 0.06f;
            float orbitAngle = crystal.baseAngle + animationTick * orbitSpeed;

            double rx = tx + (Math.cos(orbitAngle) * crystal.baseRadius);
            double ry = ty + crystal.pos.y() + floatAnim;
            double rz = tz + (Math.sin(orbitAngle) * crystal.baseRadius);

            Color finalColor = UIColors.gradient(crystal.index * 30);

            renderCrystal(ms, rx, ry, rz, crystal, finalColor, alphaAnim);
            renderGlow(ms, rx, ry, rz, finalColor, alphaAnim * 0.45f, crystal.scale);
        }

        RenderUtil.WORLD.endRender(ms);
    }

    private void renderCrystal(MatrixStack ms, double x, double y, double z, CrystalData data, Color color, float alpha) {
        ms.push();
        ms.translate(x, y, z);
        float s = data.scale * 0.12f;
        ms.scale(s, s, s);
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(data.rot.x()));
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(data.rot.y() + animationTick * 1.5f));
        ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(data.rot.z() + animationTick * 0.7f));

        float[] c = ColorUtil.normalize(ColorUtil.setAlpha(color, (int) (220 * alpha)));
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = ms.peek().getPositionMatrix();
        float w = 1.0f; float h = 2.0f;

        drawFace(buffer, matrix, 0, h, 0, -w, 0, w, w, 0, w, c);
        drawFace(buffer, matrix, 0, h, 0, w, 0, w, w, 0, -w, c);
        drawFace(buffer, matrix, 0, h, 0, w, 0, -w, -w, 0, -w, c);
        drawFace(buffer, matrix, 0, h, 0, -w, 0, -w, -w, 0, w, c);
        drawFace(buffer, matrix, 0, -h, 0, w, 0, w, -w, 0, w, c);
        drawFace(buffer, matrix, 0, -h, 0, w, 0, -w, w, 0, w, c);
        drawFace(buffer, matrix, 0, -h, 0, -w, 0, -w, w, 0, -w, c);
        drawFace(buffer, matrix, 0, -h, 0, -w, 0, w, -w, 0, -w, c);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        ms.pop();
    }

    private void drawFace(BufferBuilder b, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float[] c) {
        b.vertex(m, x1, y1, z1).color(c[0], c[1], c[2], c[3]);
        b.vertex(m, x2, y2, z2).color(c[0], c[1], c[2], c[3]);
        b.vertex(m, x3, y3, z3).color(c[0], c[1], c[2], c[3]);
    }

    private void renderGlow(MatrixStack ms, double x, double y, double z, Color color, float alpha, float scale) {
        ms.push();
        ms.translate(x, y, z);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw() + 180.0F));
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-mc.gameRenderer.getCamera().getPitch() + 180.0F));

        float size = 0.5f * scale;
        float[] c = ColorUtil.normalize(ColorUtil.setAlpha(color, (int) (255 * alpha)));

        RenderSystem.setShaderTexture(0, FileUtil.getImage("particles/glow"));
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        Matrix4f matrix = ms.peek().getPositionMatrix();
        buffer.vertex(matrix, -size, size, 0).texture(0, 1).color(c[0], c[1], c[2], c[3]);
        buffer.vertex(matrix, size, size, 0).texture(1, 1).color(c[0], c[1], c[2], c[3]);
        buffer.vertex(matrix, size, -size, 0).texture(1, 0).color(c[0], c[1], c[2], c[3]);
        buffer.vertex(matrix, -size, -size, 0).texture(0, 0).color(c[0], c[1], c[2], c[3]);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        ms.pop();
    }

    private void generateCrystals() {
        crystals.clear();
        int count = TargetEspModule.instance.getCrystalsCount();
        float height = currentTarget.getHeight();
        float width = currentTarget.getWidth();
        float baseRadius = Math.max(0.75f, width * 0.9f + 0.35f);
        float minY = 0.15f;
        float maxY = Math.max(minY + 0.2f, height - 0.15f);

        for (int i = 0; i < count; i++) {
            float angle = (float) (((float) i / count) * Math.PI * 2.0);
            float y = minY + (maxY - minY) * (float) (i % 3) / 2f;
            crystals.add(new CrystalData(
                    new Vector3f(0, y, 0),
                    new Vector3f(20f + random.nextFloat() * 40f, random.nextFloat() * 360f, random.nextFloat() * 360f),
                    0.75f + random.nextFloat() * 0.35f, i, angle, baseRadius + (random.nextFloat() - 0.5f) * 0.18f
            ));
        }
    }

    private static class CrystalData {
        private final Vector3f pos, rot;
        private final float scale, baseAngle, baseRadius;
        private final int index;
        public CrystalData(Vector3f pos, Vector3f rot, float scale, int index, float baseAngle, float baseRadius) {
            this.pos = pos; this.rot = rot; this.scale = scale; this.index = index; this.baseAngle = baseAngle; this.baseRadius = baseRadius;
        }
    }
}