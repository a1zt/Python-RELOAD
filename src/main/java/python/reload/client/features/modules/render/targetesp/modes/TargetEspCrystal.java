package python.reload.client.features.modules.render.targetesp.modes;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import python.reload.api.event.events.render.Render3DEvent;
import python.reload.api.system.files.FileUtil;
import python.reload.api.utils.color.UIColors;
import python.reload.api.utils.math.MathUtil;
import python.reload.api.utils.render.CrystalRender;
import python.reload.client.features.modules.render.targetesp.TargetEspMode;
import python.reload.client.features.modules.render.targetesp.TargetEspModule;

import java.awt.Color;

public class TargetEspCrystal extends TargetEspMode {

    private float moving = 0f;
    private float prevMoving = 0f;
    private float impactProgress = 0f;
    private int prevHurtTime = 0;

    @Override
    public void onUpdate() {
        if (currentTarget == null || !canDraw()) return;

        TargetEspModule module = TargetEspModule.getInstance();

        prevMoving = moving;
        moving += module.crystalSpeed.getValue();

        updateImpactAnimation();
    }

    private void updateImpactAnimation() {
        TargetEspModule module = TargetEspModule.getInstance();

        if (!module.crystalRedOnImpact.getValue() || currentTarget == null) {
            impactProgress = 0f;
            prevHurtTime = 0;
            return;
        }

        float fadeInSpeed = module.crystalImpactFadeIn.getValue();
        float fadeOutSpeed = module.crystalImpactFadeOut.getValue();
        float maxIntensity = module.crystalImpactIntensity.getValue();

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
        MatrixStack ms = event.matrixStack();
        Camera camera = mc.gameRenderer.getCamera();

        float alphaPC = (float) showAnimation.getValue();


        Vec3d renderPos = new Vec3d(
                MathUtil.interpolate(currentTarget.prevX, currentTarget.getX()),
                MathUtil.interpolate(currentTarget.prevY, currentTarget.getY()),
                MathUtil.interpolate(currentTarget.prevZ, currentTarget.getZ())
        );


        float width = currentTarget.getWidth() * 1.5F;

        float movingValue = MathUtil.interpolate(prevMoving, moving);

        ms.push();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);


        Vec3d cameraPos = camera.getPos();
        ms.translate(
                renderPos.x - cameraPos.x,
                renderPos.y - cameraPos.y,
                renderPos.z - cameraPos.z
        );


        BufferBuilder builder = CrystalRender.createBuffer();


        for (int i = 0; i < 360; i += 20) {

            float val = 1.2F - 0.5F * alphaPC;


            float rad = (float) Math.toRadians(i + movingValue * 0.3F);
            float sin = (float) (Math.sin(rad) * width * val);
            float cos = (float) (Math.cos(rad) * width * val);


            float size = 0.1F;

            ms.push();


            float verticalPos = 0.1F + currentTarget.getHeight() * (float) Math.abs(Math.sin(i));
            ms.translate(sin, verticalPos, cos);


            Vec3d crystalPos = renderPos.add(sin, 1.0, cos);
            Vec3d targetPos = currentTarget.getPos().add(0, currentTarget.getHeight() / 2.0, 0);

            Vector3f directionToTarget = new Vector3f(
                    (float) (targetPos.x - crystalPos.x),
                    (float) (targetPos.y - crystalPos.y),
                    (float) (targetPos.z - crystalPos.z)
            ).normalize();

            Vector3f initialDirection = new Vector3f(0.0F, 1.0F, 0.0F);
            Quaternionf rotation = new Quaternionf().rotationTo(initialDirection, directionToTarget);
            ms.multiply(rotation);


            int alpha = (int) (255 * alphaPC);
            Color color = UIColors.gradient(i * 3, alpha);


            if (impactProgress > 0) {
                Color impactColor = new Color(255, 32, 32, alpha);
                color = interpolateColor(color, impactColor, impactProgress);
            }

            CrystalRender.render(ms, builder, 0.0F, 0.0F, 0.0F, size, color);

            ms.pop();
        }

        BufferRenderer.drawWithGlobalProgram(builder.end());


        if (module.crystalBloom.getValue()) {
            RenderSystem.setShaderTexture(0, FileUtil.getImage("particles/glow"));
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);


            float bigSize = module.crystalBloomSize.getValue();

            for (int i = 0; i < 360; i += 20) {
                float val = 1.2F - 0.5F * alphaPC;
                float rad = (float) Math.toRadians(i + movingValue * 0.3F);
                float sin = (float) (Math.sin(rad) * width * val);
                float cos = (float) (Math.cos(rad) * width * val);

                float verticalPos = 0.1F + currentTarget.getHeight() * (float) Math.abs(Math.sin(i));

                ms.push();
                ms.translate(sin, verticalPos, cos);

                // Поворот к камере
                ms.multiply(camera.getRotation());

                var matrix = ms.peek().getPositionMatrix();

                // Оригинал: alpha * 0.2F
                int bloomAlpha = (int) (255 * alphaPC * 0.2F);
                Color color = UIColors.gradient(i * 3, bloomAlpha);

                // Red on Impact для bloom
                if (impactProgress > 0) {
                    Color impactColor = new Color(255, 32, 32, bloomAlpha);
                    color = interpolateColor(color, impactColor, impactProgress);
                }

                int colorRGB = color.getRGB();

                var buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                buffer.vertex(matrix, -bigSize / 2.0F, -bigSize / 2.0F, 0).texture(0f, 0f).color(colorRGB);
                buffer.vertex(matrix, -bigSize / 2.0F, bigSize / 2.0F, 0).texture(0f, 1f).color(colorRGB);
                buffer.vertex(matrix, bigSize / 2.0F, bigSize / 2.0F, 0).texture(1f, 1f).color(colorRGB);
                buffer.vertex(matrix, bigSize / 2.0F, -bigSize / 2.0F, 0).texture(1f, 0f).color(colorRGB);
                BufferRenderer.drawWithGlobalProgram(buffer.end());

                ms.pop();
            }
        }

        RenderSystem.depthMask(true);
        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.disableDepthTest();

        ms.pop();
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