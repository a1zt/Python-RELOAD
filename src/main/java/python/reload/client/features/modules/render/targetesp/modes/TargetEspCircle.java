package python.reload.client.features.modules.render.targetesp.modes;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import python.reload.api.event.events.render.Render3DEvent;
import python.reload.api.system.files.FileUtil;
import python.reload.api.utils.color.UIColors;
import python.reload.api.utils.math.MathUtil;
import python.reload.client.features.modules.render.targetesp.TargetEspMode;
import python.reload.client.features.modules.render.targetesp.TargetEspModule;

import java.awt.Color;

public class TargetEspCircle extends TargetEspMode {
    private float moving = 0f;
    private float prevMoving = 0f;
    private float verticalTime = 0f;
    private float prevVerticalTime = 0f;
    private float impactProgress = 0f;
    private int prevHurtTime = 0;

    @Override
    public void onUpdate() {
        if (currentTarget == null || !canDraw()) return;

        TargetEspModule module = TargetEspModule.getInstance();
        prevMoving = moving;
        moving += module.circleSpeed.getValue();
        // Не сбрасываем moving для плавной интерполяции

        prevVerticalTime = verticalTime;
        verticalTime += module.circleSpeed.getValue();

        updateImpactAnimation();
    }

    private void updateImpactAnimation() {
        TargetEspModule module = TargetEspModule.getInstance();

        if (!module.circleRedOnImpact.getValue() || currentTarget == null) {
            impactProgress = 0f;
            prevHurtTime = 0;
            return;
        }

        float fadeInSpeed = module.circleImpactFadeIn.getValue();
        float fadeOutSpeed = module.circleImpactFadeOut.getValue();
        float maxIntensity = module.circleImpactIntensity.getValue();

        int currentHurtTime = currentTarget.hurtTime;

        if (currentHurtTime > prevHurtTime || (currentHurtTime > 0 && prevHurtTime == 0)) {
            impactProgress = Math.min(maxIntensity, impactProgress + fadeInSpeed);
        } else if (currentHurtTime > 0) {
            impactProgress = Math.min(maxIntensity, impactProgress + fadeInSpeed * 0.5f);
        } else {
            impactProgress = Math.max(0f, impactProgress - fadeOutSpeed);
        }

        prevHurtTime = currentHurtTime;
    }

    @Override
    public void onRender3D(Render3DEvent.Render3DEventData event) {
        if (currentTarget == null || !canDraw()) return;

        TargetEspModule module = TargetEspModule.getInstance();
        MatrixStack matrixStack = event.matrixStack();
        Camera camera = mc.gameRenderer.getCamera();

        float alphaPC = (float) showAnimation.getValue();

        Vec3d vec = new Vec3d(
            MathUtil.interpolate(currentTarget.prevX, currentTarget.getX()),
            MathUtil.interpolate(currentTarget.prevY, currentTarget.getY()),
            MathUtil.interpolate(currentTarget.prevZ, currentTarget.getZ())
        );

        double x = vec.x - camera.getPos().x;
        double y = vec.y - camera.getPos().y;
        double z = vec.z - camera.getPos().z;

        float width = currentTarget.getWidth() * 1.45F + (1f - alphaPC) / 2.5F;
        float baseVal = Math.max(0.5F, 0.7F - 0.1F * impactProgress + 0.1F - 0.1F * alphaPC);
        float movingAngle = MathUtil.interpolate(prevMoving, moving);
        int step = 3;

        float size = 0.4F * module.circleSize.getValue();
        float bigSize = (0.5F + module.circleBloomSize.getValue()) * module.circleSize.getValue();

        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.setShaderTexture(0, FileUtil.getImage("particles/glow"));
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        for (int i = 0; i < 360; i += step) {
            if ((int)(i / 45.0F) % 2 != 0) {
                double rad = Math.toRadians(i + movingAngle);
                float sin = (float)(x + Math.sin(rad) * width * baseVal);
                float cos = (float)(z + Math.cos(rad) * width * baseVal);

                // Плавная волна от ног до головы используя непрерывное время
                float interpolatedVerticalTime = MathUtil.interpolate(prevVerticalTime, verticalTime);
                double radAngle = Math.toRadians(interpolatedVerticalTime);
                float waveValue = (float)((1.0 - Math.cos(radAngle)) / 2.0);
                float yPos = (float)(y + currentTarget.getHeight() * waveValue);

                matrixStack.push();
                matrixStack.translate(sin, yPos, cos);
                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
                matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

                var matrix = matrixStack.peek().getPositionMatrix();

                int alpha = (int) (alphaPC * 255);
                Color color = UIColors.gradient(i * 3, alpha);

                if (impactProgress > 0) {
                    Color impactColor = new Color(255, 32, 32, alpha);
                    color = interpolateColor(color, impactColor, impactProgress);
                }

                int colorRGB = color.getRGB();

                if (module.circleBloom.getValue()) {
                    var buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                    int bloomAlpha = (int) (alphaPC * 255 * 0.1F);
                    Color bloomColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), bloomAlpha);
                    int bloomRGB = bloomColor.getRGB();

                    buffer.vertex(matrix, -bigSize / 2.0F, bigSize / 2.0F, -size / 2.0F).texture(0f, 1f).color(bloomRGB);
                    buffer.vertex(matrix, bigSize / 2.0F, bigSize / 2.0F, -size / 2.0F).texture(1f, 1f).color(bloomRGB);
                    buffer.vertex(matrix, bigSize / 2.0F, -bigSize / 2.0F, -size / 2.0F).texture(1f, 0f).color(bloomRGB);
                    buffer.vertex(matrix, -bigSize / 2.0F, -bigSize / 2.0F, -size / 2.0F).texture(0f, 0f).color(bloomRGB);
                    BufferRenderer.drawWithGlobalProgram(buffer.end());
                }

                var buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                buffer.vertex(matrix, -size / 2.0F, size / 2.0F, -size / 2.0F).texture(0f, 1f).color(colorRGB);
                buffer.vertex(matrix, size / 2.0F, size / 2.0F, -size / 2.0F).texture(1f, 1f).color(colorRGB);
                buffer.vertex(matrix, size / 2.0F, -size / 2.0F, -size / 2.0F).texture(1f, 0f).color(colorRGB);
                buffer.vertex(matrix, -size / 2.0F, -size / 2.0F, -size / 2.0F).texture(0f, 0f).color(colorRGB);
                BufferRenderer.drawWithGlobalProgram(buffer.end());

                matrixStack.pop();
            }
        }

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private Color interpolateColor(Color color1, Color color2, float progress) {
        progress = Math.max(0f, Math.min(1f, progress));

        int r1 = color1.getRed();
        int g1 = color1.getGreen();
        int b1 = color1.getBlue();
        int a1 = color1.getAlpha();

        int r2 = color2.getRed();
        int g2 = color2.getGreen();
        int b2 = color2.getBlue();
        int a2 = color2.getAlpha();

        float smoothProgress = progress * progress * (3f - 2f * progress);

        int r = (int) (r1 + (r2 - r1) * smoothProgress);
        int g = (int) (g1 + (g2 - g1) * smoothProgress);
        int b = (int) (b1 + (b2 - b1) * smoothProgress);
        int a = (int) (a1 + (a2 - a1) * smoothProgress);

        return new Color(r, g, b, a);
    }

    @Override
    public void updateTarget() {
        super.updateTarget();
        if (currentTarget == null) {
            impactProgress = 0f;
            prevHurtTime = 0;
        }
    }
}
