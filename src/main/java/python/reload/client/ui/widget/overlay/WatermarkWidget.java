package python.reload.client.ui.widget.overlay;

import net.minecraft.client.util.math.MatrixStack;
import python.reload.api.utils.math.MathUtil;
import python.reload.api.utils.color.UIColors;
import python.reload.api.utils.render.RenderUtil;
import python.reload.api.utils.render.fonts.Font;
import python.reload.api.utils.render.fonts.Fonts;
import python.reload.client.ui.widget.Widget;

import java.awt.*;

public class WatermarkWidget extends Widget {
    private float animFps;
    private float animPing;
    private float animTps;

    @Override
    public String getName() {
        return "Watermark";
    }

    public WatermarkWidget() {
        super(3f, 3f);
    }

    @Override
    public void render(MatrixStack matrixStack) {
        float x = getDraggable().getX();
        float y = getDraggable().getY();

        updateAnimatedValues();

        String clientName = "Python";
        String version = " v8";
        String fpsText = (int) animFps + " fps";
        String pingText = (int) animPing + " ms";
        String tpsText = String.format("%.1f", animTps) + " tps";

        String coordText = getCoordinates();
        String bpsText = getBPS();

        Font font = getSemiBoldFont();
        float fontSize = scaled(7f);

        float backgroundHeight = fontSize + scaled(3f);
        float padding = scaled(4f);
        float round = backgroundHeight * 0.3f;
        float lineSpacing = scaled(1f);

        float clientWidth = font.getWidth(clientName, fontSize);
        float versionWidth = font.getWidth(version, fontSize);
        float fpsWidth = font.getWidth(fpsText, fontSize);
        float pingWidth = font.getWidth(pingText, fontSize);
        float tpsWidth = font.getWidth(tpsText, fontSize);

        float iconWidth = 8f;
        float iconSize = fontSize;
        float textIconSpacing = 2f;
        float linePadding = 3f;

        float line1Width = clientWidth + versionWidth + linePadding * 2 +
                iconWidth + textIconSpacing + fpsWidth + linePadding * 2 +
                iconWidth + textIconSpacing + pingWidth + linePadding * 2 +
                iconWidth + textIconSpacing + tpsWidth;

        float coordWidth = font.getWidth(coordText, fontSize);
        float bpsWidth = font.getWidth(bpsText, fontSize);
        float line2Width = iconWidth + textIconSpacing + coordWidth + linePadding * 2 +
                iconWidth + textIconSpacing + bpsWidth;

        float totalWidth = Math.max(line1Width, line2Width);

        float bg1Width = line1Width + padding * 2f;
        float bg2Width = line2Width + padding * 2f;

        float bgHeight = backgroundHeight + 2;

        float bg1X = x;
        float bg1Y = y;

        RenderUtil.BLUR_RECT.draw(matrixStack, bg1X, bg1Y, bg1Width, bgHeight, round - 1, new Color(18, 18, 18, 240));

        float bg2X = x;
        float bg2Y = bg1Y + bgHeight + lineSpacing;

        RenderUtil.BLUR_RECT.draw(matrixStack, bg2X, bg2Y, bg2Width, bgHeight, round - 1, new Color(18, 18, 18, 240));

        Color textColor = new Color(185, 185, 185);
        Color grayColor = new Color(150, 150, 150);
        Color primaryColor = UIColors.primary();
        Color secondaryColor = UIColors.secondary();

        float currentX = bg1X + padding;
        float textY1 = bg1Y + (bgHeight - fontSize) / 2f + 0.5f;
        float iconY1 = bg1Y + (bgHeight - iconSize) / 2f;

        font.drawGradientText(matrixStack, clientName, currentX, textY1, fontSize,
                primaryColor, secondaryColor, clientWidth / 4f);
        currentX += clientWidth;

        font.drawText(matrixStack, version, currentX, textY1, fontSize, grayColor);
        currentX += versionWidth;

        drawVerticalLine(matrixStack, currentX + linePadding, bg1Y, bgHeight);
        currentX += linePadding * 2;

        Fonts.ICON_ESSENS.drawGradientText(matrixStack, "m", currentX, iconY1, iconSize,
                primaryColor, secondaryColor, iconWidth / 4f);
        currentX += iconWidth;

        currentX += textIconSpacing;
        font.drawText(matrixStack, fpsText, currentX, textY1, fontSize, getStaticFPSColor((int) animFps));
        currentX += fpsWidth;

        drawVerticalLine(matrixStack, currentX + linePadding, bg1Y, bgHeight);
        currentX += linePadding * 2;

        Fonts.ICON_V1.drawGradientText(matrixStack, "I", currentX, iconY1, iconSize,
                primaryColor, secondaryColor, iconWidth / 4f);
        currentX += iconWidth;

        currentX += textIconSpacing;
        font.drawText(matrixStack, pingText, currentX, textY1, fontSize, getStaticPingColor((int) animPing));
        currentX += pingWidth;

        drawVerticalLine(matrixStack, currentX + linePadding, bg1Y, bgHeight);
        currentX += linePadding * 2;

        Fonts.ICON_V1.drawGradientText(matrixStack, "J", currentX, iconY1, iconSize,
                primaryColor, secondaryColor, iconWidth / 4f);
        currentX += iconWidth;

        currentX += textIconSpacing;
        font.drawText(matrixStack, tpsText, currentX, textY1, fontSize, getStaticTPSColor(animTps));

        float textY2 = bg2Y + (bgHeight - fontSize) / 2f + 0.5f;
        float iconY2 = bg2Y + (bgHeight - iconSize) / 2f;
        currentX = bg2X + padding;

        Fonts.ICON_V1.drawGradientText(matrixStack, "x", currentX - 1.5f, iconY2, iconSize,
                primaryColor, secondaryColor, iconWidth / 4f);
        currentX += iconWidth;

        currentX += textIconSpacing;
        font.drawText(matrixStack, coordText, currentX, textY2, fontSize, textColor);
        currentX += coordWidth;

        drawVerticalLine(matrixStack, currentX + linePadding, bg2Y, bgHeight);
        currentX += linePadding * 2;

        Fonts.ICON_V1.drawGradientText(matrixStack, "c", currentX, iconY2, iconSize,
                primaryColor, secondaryColor, iconWidth / 4f);
        currentX += iconWidth;

        currentX += textIconSpacing;
        font.drawText(matrixStack, bpsText, currentX, textY2, fontSize, textColor);

        float finalWidth = Math.max(bg1Width, bg2Width);
        float finalHeight = bgHeight * 2f + lineSpacing;

        getDraggable().setWidth(finalWidth);
        getDraggable().setHeight(finalHeight);
    }

    private void drawVerticalLine(MatrixStack matrixStack, float x, float y, float height) {
        float lineY = y + 3;
        float lineHeight = height - 6;
        RenderUtil.RECT.draw(matrixStack, x, lineY, 1, lineHeight, 0, new Color(255, 255, 255, 40));
    }

    private void updateAnimatedValues() {
        animFps = MathUtil.interpolate((int) animFps, mc.getCurrentFps(), 0.2f);
        int currentPing = getCurrentPing();
        animPing = MathUtil.interpolate((int) animPing, currentPing, 0.2f);
        animTps = MathUtil.interpolate(animTps, getEstimatedTPS(), 0.2f);
    }

    private String getCoordinates() {
        if (mc.player == null) return "0, 0, 0";
        String x = String.format("%.0f", mc.player.getX());
        String y = String.format("%.0f", mc.player.getY());
        String z = String.format("%.0f", mc.player.getZ());
        return x + ", " + y + ", " + z;
    }

    private String getBPS() {
        if (mc.player == null) return "0 bps";
        double bps = MathUtil.getEntityBPS(mc.player);
        return String.format("%.1f", bps) + " bps";
    }

    private int getCurrentPing() {
        if (mc.player == null || mc.getNetworkHandler() == null) return 0;

        if (mc.isInSingleplayer()) return 0;

        var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return entry != null ? entry.getLatency() : 0;
    }

    private float getEstimatedTPS() {
        try {
            if (mc.world != null && mc.world.getTime() > 0) {
                return 20.0f;
            }
        } catch (Exception e) {
        }
        return 20.0f;
    }
    private Color getStaticFPSColor(int fps) {
        return new Color(185, 185, 185);
    }
    private Color getStaticPingColor(int ping) {
        return new Color(185, 185, 185);
    }
    private Color getStaticTPSColor(float tps) {
        return new Color(185, 185, 185);
    }
}