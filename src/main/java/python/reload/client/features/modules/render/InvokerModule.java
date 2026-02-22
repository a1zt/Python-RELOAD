package python.reload.client.features.modules.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import python.reload.api.event.EventListener;
import python.reload.api.event.Listener;
import python.reload.api.event.events.render.Render3DEvent;
import python.reload.api.module.Category;
import python.reload.api.module.Module;
import python.reload.api.module.ModuleRegister;
import python.reload.api.module.setting.BooleanSetting;
import python.reload.api.module.setting.ColorSetting;
import python.reload.api.module.setting.SliderSetting;
import python.reload.api.system.files.FileUtil;
import python.reload.api.utils.color.ColorUtil;
import python.reload.api.utils.render.RenderUtil;

import java.awt.*;

@ModuleRegister(name = "Invoker", category = Category.RENDER)
public class InvokerModule extends Module {
    @Getter private static final InvokerModule instance = new InvokerModule();

    private final ColorSetting sphere1Color = new ColorSetting("Exort color").value(new Color(255, 200, 100, 200));
    private final ColorSetting sphere2Color = new ColorSetting("Wex color").value(new Color(255, 150, 255, 200));
    private final ColorSetting sphere3Color = new ColorSetting("Quas color").value(new Color(150, 150, 255, 200));

    private final SliderSetting sphereSize = new SliderSetting("Размер сфер").value(0.3f).range(0.1f, 0.8f).step(0.05f);
    private final SliderSetting orbitRadius = new SliderSetting("Радиус орбиты").value(0.8f).range(0.3f, 2.0f).step(0.1f);
    private final SliderSetting height = new SliderSetting("Высота").value(0.7f).range(-0.5f, 1.5f).step(0.1f);

    private final BooleanSetting renderSelf = new BooleanSetting("На себе").value(true);
    private final BooleanSetting renderOthers = new BooleanSetting("На других").value(false);
    private final BooleanSetting renderInFirstPerson = new BooleanSetting("От первого лица").value(true);

    public InvokerModule() {
        addSettings(sphere1Color, sphere2Color, sphere3Color, sphereSize, orbitRadius, height,
                renderSelf, renderOthers, renderInFirstPerson);
    }

    @Override
    public void onEvent() {
        EventListener renderEvent = Render3DEvent.getInstance().subscribe(new Listener<>(event -> {
            MatrixStack ms = event.matrixStack();

            for (Entity e : mc.world.getEntities()) {
                if (!(e instanceof PlayerEntity p)) continue;

                if (p == mc.player && !renderSelf.getValue()) continue;
                if (p != mc.player && !renderOthers.getValue()) continue;
                if (p.isInvisible()) continue;

                renderSpheres(ms, p, event.partialTicks());
            }
        }));

        addEvents(renderEvent);
    }

    private void renderSpheres(MatrixStack ms, PlayerEntity player, float partialTicks) {
        if (player == mc.player && mc.options.getPerspective().isFirstPerson() && !renderInFirstPerson.getValue()) {
            return;
        }

        double x = MathHelper.lerp(partialTicks, player.prevX, player.getX());
        double y = MathHelper.lerp(partialTicks, player.prevY, player.getY());
        double z = MathHelper.lerp(partialTicks, player.prevZ, player.getZ());

        float bodyYaw = MathHelper.lerpAngleDegrees(partialTicks, player.prevBodyYaw, player.bodyYaw);

        Vec3d cam = mc.getEntityRenderDispatcher().camera.getPos();
        float sphereY = player.getHeight() * 0.5f + height.getValue();
        float radius = orbitRadius.getValue();
        float size = sphereSize.getValue();

        Color[] colors = {sphere1Color.getValue(), sphere2Color.getValue(), sphere3Color.getValue()};
        float[] angleOffsets = {180f, -90f, 360f};

        for (int i = 0; i < 3; i++) {
            float angle = angleOffsets[i];

            double angleRad = Math.toRadians(bodyYaw + angle);

            double sphereX = x + Math.cos(angleRad) * radius;
            double sphereY_pos = y + sphereY;
            double sphereZ = z + Math.sin(angleRad) * radius;

            renderSphere(ms, sphereX, sphereY_pos, sphereZ, cam, size, colors[i]);
        }
    }

    private void renderSphere(MatrixStack ms, double x, double y, double z, Vec3d cam, float size, Color color) {
        RenderUtil.WORLD.startRender(ms);

        ms.push();

        double renderX = x - cam.x;
        double renderY = y - cam.y;
        double renderZ = z - cam.z;

        ms.translate(renderX, renderY, renderZ);

        Camera camera = mc.gameRenderer.getCamera();
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderTexture(0, FileUtil.getImage("particles/glow"));
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        Matrix4f matrix = ms.peek().getPositionMatrix();
        float[] col = ColorUtil.normalize(color);

        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        buf.vertex(matrix, -size, size, 0).texture(0f, 1f).color(col[0], col[1], col[2], col[3]);
        buf.vertex(matrix, size, size, 0).texture(1f, 1f).color(col[0], col[1], col[2], col[3]);
        buf.vertex(matrix, size, -size, 0).texture(1f, 0f).color(col[0], col[1], col[2], col[3]);
        buf.vertex(matrix, -size, -size, 0).texture(0f, 0f).color(col[0], col[1], col[2], col[3]);

        BufferRenderer.drawWithGlobalProgram(buf.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);

        ms.pop();

        RenderUtil.WORLD.endRender(ms);
    }
}