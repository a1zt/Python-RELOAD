package python.reload.client.features.modules.render.particles;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import python.reload.api.event.EventListener;
import python.reload.api.event.Listener;
import python.reload.api.event.events.player.other.UpdateEvent;
import python.reload.api.event.events.render.Render3DEvent;
import python.reload.api.module.setting.SliderSetting;
import python.reload.api.system.files.FileUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CubeFly extends ParticlesModule.BaseSettings {
    private final SliderSetting amount = new SliderSetting(prefix + "Amount").value(30f).range(10f, 100f).step(5f);
    private final SliderSetting size = new SliderSetting(prefix + "Size").value(0.15f).range(0.05f, 0.5f).step(0.01f);

    private final List<Cube> cubes = new ArrayList<>();
    private static final Identifier icon = FileUtil.getImage("particles/glow");

    public CubeFly() {
        super("CubeFly");
        addSettings(amount, size);
    }

    public void toggle() {
        cubes.clear();
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null || mc.world == null) {
                cubes.clear();
                return;
            }

            Vec3d playerPos = mc.player.getPos();


            Iterator<Cube> iterator = cubes.iterator();
            while (iterator.hasNext()) {
                Cube cube = iterator.next();
                if (cube.isDead() || cube.pos.distanceTo(playerPos) > 30) {
                    iterator.remove();
                    continue;
                }
                BlockPos blockPos = new BlockPos((int) cube.pos.x, (int) cube.pos.y, (int) cube.pos.z);
                if (!mc.world.getBlockState(blockPos).isAir()) {
                    iterator.remove();
                }
            }


            int max = amount.getValue().intValue();
            int toSpawn = max - cubes.size();
            if (toSpawn > 0) {
                float yaw = mc.player.getYaw();
                double lx = -Math.sin(Math.toRadians(yaw));
                double lz = Math.cos(Math.toRadians(yaw));

                for (int i = 0; i < toSpawn; i++) {
                    double cx, cz;
                    if (Math.random() < 0.7) {
                        // Front cone spawn
                        double frontDistance = 5 + Math.random() * 15;
                        double sideDistance = (Math.random() - 0.5) * 15;
                        cx = playerPos.x + lx * frontDistance + lz * sideDistance;
                        cz = playerPos.z + lz * frontDistance - lx * sideDistance;
                    } else {
                        // Random spawn around player
                        cx = playerPos.x + (Math.random() - 0.5) * 30;
                        cz = playerPos.z + (Math.random() - 0.5) * 30;
                    }

                    double cy = playerPos.y + Math.random() * 17;
                    BlockPos spawnPos = new BlockPos((int) cx, (int) cy, (int) cz);

                    if (!mc.world.getBlockState(spawnPos).isAir()) continue;

                    cubes.add(new Cube(new Vec3d(cx, cy, cz), size.getValue()));
                }
            }
        }));

        EventListener renderEvent = Render3DEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null || mc.world == null || cubes.isEmpty()) return;

            Camera camera = mc.getEntityRenderDispatcher().camera;
            Vec3d cameraPos = camera.getPos();
            MatrixStack matrixStack = event.matrixStack();

            for (Cube cube : cubes) {
                cube.tick();
                if (cube.isDead()) continue;

                Vec3d relativePos = cube.pos.subtract(cameraPos);
                float alpha = cube.getAlpha();

                matrixStack.push();
                matrixStack.translate(relativePos.x, relativePos.y, relativePos.z);
                matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(cube.rx));
                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(cube.ry));
                matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(cube.rz));

                drawCube(matrixStack, cube.size, (int)(alpha * 35), false);
                drawOutline(matrixStack, cube.size, (int)(alpha * 200));
                drawCube(matrixStack, cube.size * 0.35f, (int)(alpha * 255), true);
                matrixStack.pop();

                drawGlow(matrixStack, relativePos, cube.size * 3f, (int)(alpha * 120), camera);
            }
        }));

        addEvents(updateEvent, renderEvent);
    }

    private void drawCube(MatrixStack matrixStack, float size, int alpha, boolean solid) {
        if (alpha <= 0) return;

        float half = size / 2f;
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Matrix4f matrix = matrixStack.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);


        float[][] vertices = {
                {-half, -half, -half, half, -half, -half, half, half, -half, -half, half, -half}, // back
                {-half, -half, half, -half, half, half, half, half, half, half, -half, half}, // front
                {-half, half, -half, half, half, -half, half, half, half, -half, half, half}, // top
                {-half, -half, -half, -half, -half, half, half, -half, half, half, -half, -half}, // bottom
                {-half, -half, -half, -half, -half, half, -half, half, half, -half, half, -half}, // left
                {half, -half, -half, half, -half, half, half, half, half, half, half, -half} // right
        };

        for (float[] face : vertices) {
            for (int i = 0; i < 12; i += 3) {
                buffer.vertex(matrix, face[i], face[i+1], face[i+2]).color(255, 255, 255, alpha);
            }
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private void drawOutline(MatrixStack matrixStack, float size, int alpha) {
        if (alpha <= 0) return;

        float half = size / 2f;
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.lineWidth(1.5f);
        RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
        MatrixStack.Entry entry = matrixStack.peek();


        float[][] edges = {
                {-half, -half, -half, half, -half, -half},
                {half, -half, -half, half, -half, half},
                {half, -half, half, -half, -half, half},
                {-half, -half, half, -half, -half, -half},
                {-half, half, -half, half, half, -half},
                {half, half, -half, half, half, half},
                {half, half, half, -half, half, half},
                {-half, half, half, -half, half, -half},
                {-half, -half, -half, -half, half, -half},
                {half, -half, -half, half, half, -half},
                {half, -half, half, half, half, half},
                {-half, -half, half, -half, half, half}
        };

        for (float[] edge : edges) {
            drawEdge(buffer, entry, edge[0], edge[1], edge[2], edge[3], edge[4], edge[5], alpha);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableBlend();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
    }

    private void drawEdge(BufferBuilder buffer, MatrixStack.Entry entry, float x1, float y1, float z1, float x2, float y2, float z2, int alpha) {
        Vector3f normal = new Vector3f(x2 - x1, y2 - y1, z2 - z1);
        if (normal.lengthSquared() > 0) {
            normal.normalize();
        } else {
            normal.set(0, 1, 0);
        }

        buffer.vertex(entry, x1, y1, z1).color(255, 255, 255, alpha).normal(entry, normal.x, normal.y, normal.z);
        buffer.vertex(entry, x2, y2, z2).color(255, 255, 255, alpha).normal(entry, normal.x, normal.y, normal.z);
    }

    private void drawGlow(MatrixStack matrixStack, Vec3d pos, float size, int alpha, Camera camera) {
        if (alpha <= 0) return;

        matrixStack.push();
        matrixStack.translate(pos.x, pos.y, pos.z);
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA,
                GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SrcFactor.ONE,
                GlStateManager.DstFactor.ZERO
        );
        RenderSystem.depthMask(false);
        RenderSystem.setShaderTexture(0, icon);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        Matrix4f matrix = matrixStack.peek().getPositionMatrix();
        float half = size / 2f;
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        float[][] quad = {
                {-half, -half, 0, 0},
                {-half, half, 0, 1},
                {half, half, 1, 1},
                {half, -half, 1, 0}
        };

        for (float[] vertex : quad) {
            buffer.vertex(matrix, vertex[0], vertex[1], 0)
                    .texture(vertex[2], vertex[3])
                    .color(255, 255, 255, alpha);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        matrixStack.pop();
    }

    @Getter
    private static class Cube {
        Vec3d pos, velocity;
        float rx, ry, rz, rsx, rsy, rsz, size;
        long spawnTime;

        Cube(Vec3d position, float cubeSize) {
            pos = position;
            size = cubeSize;


            double angle = Math.random() * Math.PI * 2;
            double speed = 0.02 + Math.random() * 0.02;
            velocity = new Vec3d(Math.cos(angle) * speed, (Math.random() - 0.5) * 0.02, Math.sin(angle) * speed);


            rx = (float)(Math.random() * 360);
            ry = (float)(Math.random() * 360);
            rz = (float)(Math.random() * 360);


            rsx = (float)((Math.random() - 0.5) * 3);
            rsy = (float)((Math.random() - 0.5) * 3);
            rsz = (float)((Math.random() - 0.5) * 3);

            spawnTime = System.currentTimeMillis();
        }

        void tick() {
            velocity = velocity.multiply(0.98);
            pos = pos.add(velocity);
            rx += rsx;
            ry += rsy;
            rz += rsz;
        }

        boolean isDead() {
            return System.currentTimeMillis() - spawnTime > 10000;
        }

        float getAlpha() {
            long timeAlive = System.currentTimeMillis() - spawnTime;
            if (timeAlive < 100) {
                return timeAlive / 100f;
            }
            if (timeAlive > 7000) {
                return 1f - ((timeAlive - 7000) / 3000f);
            }
            return 1f;
        }
    }
}