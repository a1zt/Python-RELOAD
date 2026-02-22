package sweetie.evaware.client.ui.widget.overlay;

import net.minecraft.client.util.math.MatrixStack;
import sweetie.evaware.api.system.backend.ClientInfo;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.render.RenderUtil;
import sweetie.evaware.api.utils.render.fonts.Font;
import sweetie.evaware.client.ui.widget.Widget;

import java.awt.*;

public class WatermarkWidget extends Widget {
    private float watermarkWidth = 0f;

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
        float gap = getGap();

        float width = 0f;
        float height = 0f;

        float headSize = scaled(29f);
        boolean isRight = x > mc.getWindow().getScaledWidth() / 2f;

        float headX = !isRight ? x : x + getDraggable().getWidth() - headSize;

        RenderUtil.TEXTURE_RECT.drawHead(matrixStack, mc.player, headX, y, headSize, headSize, getGap() / 2f, 0f, Color.WHITE);

        width += headSize + gap;
        height += headSize;

        float pillsStartX = !isRight ? x + headSize + gap : x;

        float namePillX = !isRight ? pillsStartX : headX - gap - watermarkWidth;
        float[] namePill = drawPill(matrixStack, namePillX, y, getClientName() + getClientVersion());

        float pillsBottomStartY = y + headSize - namePill[3];

        float namePillWidth = namePill[2];
        watermarkWidth = namePillWidth;

        float[] prefixPill = drawPill(matrixStack, pillsStartX, pillsBottomStartY, getTitle());
        float prefixPillWidth = prefixPill[2];

        width += Math.max(prefixPillWidth, namePillWidth);

        getDraggable().setWidth(width);
        getDraggable().setHeight(height);
    }

    private float[] drawPill(MatrixStack matrixStack, float x, float y, String content) {
        boolean watermark = content.contains(ClientInfo.NAME);

        Font font = !watermark ? getMediumFont() : getSemiBoldFont();

        float fontSize = scaled(7.5f);
        float contentWidth = font.getWidth(content, fontSize);
        float contentHeight = fontSize;

        float gap = getGap() * 0.9f;
        float backgroundWidth = contentWidth + gap * 2f;
        float backgroundHeight = contentHeight + gap * 2f;
        float round = backgroundHeight * 0.3f;

        RenderUtil.BLUR_RECT.draw(matrixStack, x, y, backgroundWidth, backgroundHeight, round, UIColors.widgetBlur());

        float textX = x + gap;
        float textY = y + gap;
        if (!watermark) {
            font.drawText(matrixStack, content, textX, textY, fontSize, UIColors.textColor());
        } else {
            String pre = getClientName();
            String pro = getClientVersion();
            float preWidth = font.getWidth(pre, fontSize);
            font.drawGradientText(matrixStack, pre, textX, textY, fontSize, UIColors.primary(), UIColors.secondary(), contentWidth / 4f);
            font.drawText(matrixStack, pro, textX + preWidth, textY, fontSize, UIColors.inactiveTextColor());
        }
        return new float[]{x, y, backgroundWidth, backgroundHeight};
    }

    private String getTitle() {
        return "Сладких снов";
    }

    private String getClientVersion() {
        return " v" + ClientInfo.VERSION;
    }

    private String getClientName() {
        return ClientInfo.NAME;
    }
}