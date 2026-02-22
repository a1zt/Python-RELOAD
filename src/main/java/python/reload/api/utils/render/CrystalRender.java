package python.reload.api.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.awt.Color;

public final class CrystalRender {

    private static final Vector3f[] VERTICES = new Vector3f[]{
            new Vector3f(0.0F, 1.5F, 0.0F),   // верх
            new Vector3f(0.0F, -1.5F, 0.0F),  // низ
            new Vector3f(1.0F, 0.0F, 0.0F),   // право
            new Vector3f(-1.0F, 0.0F, 0.0F),  // лево
            new Vector3f(0.0F, 0.0F, 1.0F),   // перед
            new Vector3f(0.0F, 0.0F, -1.0F)   // зад
    };

    private static final int[][] FACES = new int[][]{
            {0, 2, 4}, {0, 4, 3}, {0, 3, 5}, {0, 5, 2},
            {1, 4, 2}, {1, 3, 4}, {1, 5, 3}, {1, 2, 5}
    };

    private static final float[] FACE_BRIGHTNESS = new float[]{
            1.0F, 0.8F, 0.6F, 0.9F, 0.7F, 0.5F, 0.4F, 0.6F
    };

    public static void render(MatrixStack matrices, BufferBuilder buffer, float x, float y, float z, float size, Color color) {
        matrices.push();
        matrices.translate(x, y, z);
        matrices.scale(size, size, size);
        Matrix4f transformationMatrix = matrices.peek().getPositionMatrix();

        for (int i = 0; i < FACES.length; i++) {
            int[] face = FACES[i];
            float brightness = FACE_BRIGHTNESS[i];
            Vector3f v1 = VERTICES[face[0]];
            Vector3f v2 = VERTICES[face[1]];
            Vector3f v3 = VERTICES[face[2]];
            int shadedColor = applyBrightness(color.getRGB(), brightness);
            buffer.vertex(transformationMatrix, v1.x, v1.y, v1.z).color(shadedColor);
            buffer.vertex(transformationMatrix, v2.x, v2.y, v2.z).color(shadedColor);
            buffer.vertex(transformationMatrix, v3.x, v3.y, v3.z).color(shadedColor);
        }

        matrices.pop();
    }

    public static BufferBuilder createBuffer() {
        setupRenderState();
        return Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
    }

    private static void setupRenderState() {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static int applyBrightness(int color, float brightness) {
        int alpha = (color >> 24) & 255;
        int red = (int) ((float) ((color >> 16) & 255) * brightness);
        int green = (int) ((float) ((color >> 8) & 255) * brightness);
        int blue = (int) ((float) (color & 255) * brightness);
        red = Math.min(255, Math.max(0, red));
        green = Math.min(255, Math.max(0, green));
        blue = Math.min(255, Math.max(0, blue));
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private CrystalRender() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}