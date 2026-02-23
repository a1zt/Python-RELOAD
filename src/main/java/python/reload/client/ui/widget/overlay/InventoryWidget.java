package python.reload.client.ui.widget.overlay;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import python.reload.api.event.events.render.Render2DEvent;
import python.reload.api.utils.animation.AnimationUtil;
import python.reload.api.utils.animation.Easing;
import python.reload.api.utils.render.RenderUtil;
import python.reload.api.utils.render.fonts.Font;
import python.reload.api.utils.render.fonts.Fonts;
import python.reload.api.utils.color.ColorUtil;
import python.reload.api.utils.color.UIColors;
import python.reload.client.ui.widget.Widget;

import java.awt.*;

public class InventoryWidget extends Widget {
    private AnimationUtil animation = new AnimationUtil();
    private AnimationUtil widthAnimation = new AnimationUtil();
    private AnimationUtil heightAnimation = new AnimationUtil();

    private float cachedBgWidth = 0;
    private float cachedTotalHeight = 0;

    private static final int SLOT_SIZE = 10;
    private static final int SLOT_SPACING = 1;
    private static final int COLUMNS = 9;
    private static final int INVENTORY_ROWS = 3;

    public InventoryWidget() {
        super(10f, 100f);
        animation.setValue(0);
        widthAnimation.setValue(0);
        heightAnimation.setValue(0);
    }

    @Override
    public String getName() {
        return "Inventory";
    }

    @Override
    public void render(Render2DEvent.Render2DEventData event) {
        MatrixStack matrixStack = event.matrixStack();
        DrawContext context = event.context();

        if (mc.player == null) return;

        boolean hasItems = false;
        for (int i = 9; i < 36; i++) {
            if (!mc.player.getInventory().getStack(i).isEmpty()) {
                hasItems = true;
                break;
            }
        }

        boolean isChatOpen = mc.currentScreen != null && mc.currentScreen.getClass().getName().contains("ChatScreen");
        boolean shouldRender = hasItems || isChatOpen;

        animation.update();
        widthAnimation.update();
        heightAnimation.update();

        if (shouldRender && !animation.isAlive()) {
            animation.run(1f, 200L, Easing.SINE_OUT);
        } else if (!shouldRender && animation.isAlive()) {
            animation.run(0f, 200L, Easing.SINE_OUT);
        }

        float animValue = (float) animation.getValue();
        if (animValue <= 0.01f) return;

        float x = getDraggable().getX();
        float y = getDraggable().getY();

        Font font = getSemiBoldFont();
        float fontSize = scaled(7f);
        float titleBgHeight = fontSize + scaled(5f);
        float padding = scaled(2f);
        float round = titleBgHeight * 0.3f;
        float lineSpacing = scaled(1f);
        float iconSize = fontSize + 1f;
        float iconWidth = 8f;
        float textIconSpacing = 2f;

        float scaledSlotSize = scaled(SLOT_SIZE);
        float scaledSlotSpacing = scaled(SLOT_SPACING);

        float gridWidth = (scaledSlotSize + scaledSlotSpacing) * COLUMNS - scaledSlotSpacing;
        float gridHeight = (scaledSlotSize + scaledSlotSpacing) * INVENTORY_ROWS - scaledSlotSpacing;

        String title = "Inventory";
        float titleWidth = font.getWidth(title, fontSize);
        float iconTitleWidth = iconWidth + textIconSpacing + titleWidth;

        float targetBgWidth = Math.max(iconTitleWidth, gridWidth) + padding * 2f;
        float targetTotalHeight = titleBgHeight + lineSpacing + gridHeight + padding * 2f;

        if (targetBgWidth != cachedBgWidth || targetTotalHeight != cachedTotalHeight) {
            cachedBgWidth = targetBgWidth;
            cachedTotalHeight = targetTotalHeight;

            if (!widthAnimation.isAlive()) {
                widthAnimation.run(targetBgWidth, 150L, Easing.SINE_OUT);
            }
            if (!heightAnimation.isAlive()) {
                heightAnimation.run(targetTotalHeight, 150L, Easing.SINE_OUT);
            }
        }

        float bgWidth = (float) widthAnimation.getValue();
        float totalHeight = (float) heightAnimation.getValue();

        float currentY = y;

        float bg1X = x;
        float bg1Y = currentY;

        RenderUtil.BLUR_RECT.draw(matrixStack, bg1X, bg1Y, bgWidth, titleBgHeight, round - 1,
                new Color(18, 18, 18, (int)(240 * animValue)));

        float textY1 = bg1Y + (titleBgHeight - fontSize) / 2f + 0.5f;
        float iconY1 = bg1Y + (titleBgHeight - iconSize) / 2f;
        float currentX = bg1X + padding;
        Fonts.ICON_ESSENS.drawGradientText(matrixStack, "q", currentX, iconY1, iconSize,
                ColorUtil.setAlpha(UIColors.primary(), (int)(UIColors.primary().getAlpha() * animValue)),
                ColorUtil.setAlpha(UIColors.secondary(), (int)(UIColors.secondary().getAlpha() * animValue)),
                iconWidth / 4f);
        currentX += iconWidth + textIconSpacing;
        font.drawText(matrixStack, title, currentX + 1, textY1, fontSize,
                new Color(185, 185, 185, (int)(255 * animValue)));
        currentY += titleBgHeight + lineSpacing;

        float gridBgX = x;
        float gridBgY = currentY;
        float gridBgHeight = totalHeight - (titleBgHeight + lineSpacing);

        RenderUtil.BLUR_RECT.draw(matrixStack, gridBgX, gridBgY, bgWidth, gridBgHeight, round - 1,
                new Color(18, 18, 18, (int)(240 * animValue)));

        float gridX = x + (bgWidth - gridWidth) / 2f;
        float gridY = gridBgY + (gridBgHeight - gridHeight) / 2f;

        drawInventoryGrid(matrixStack, context, gridX, gridY,
                scaledSlotSize, scaledSlotSpacing, animValue);

        getDraggable().setWidth(bgWidth);
        getDraggable().setHeight(totalHeight);
    }

    private void drawInventoryGrid(MatrixStack matrixStack, DrawContext context,
                                   float startX, float startY,
                                   float slotSize, float spacing, float alpha) {
        if (mc.player == null) return;

        for (int row = 0; row < INVENTORY_ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int slotIndex = col + (row * COLUMNS) + 9;

                float slotX = startX + col * (slotSize + spacing);
                float slotY = startY + row * (slotSize + spacing);

                RenderUtil.RECT.draw(matrixStack, slotX, slotY, slotSize, slotSize, 1f,
                        new Color(30, 30, 30, (int)(180 * alpha)));

                ItemStack stack = mc.player.getInventory().getStack(slotIndex);
                if (!stack.isEmpty()) {
                    matrixStack.push();
                    float scale = (slotSize - 1.1f) / 16f;
                    float itemOffset = 0.75f;
                    matrixStack.translate(slotX + itemOffset, slotY + itemOffset, 0);
                    matrixStack.scale(scale, scale, 1);
                    matrixStack.translate(0, 0, 100);
                    context.drawItem(stack, 0, 0);
                    matrixStack.pop();

                    if (stack.getCount() > 1) {
                        String count = String.valueOf(stack.getCount());
                        Font font = getSemiBoldFont();
                        float textSize = scaled(3.5f);
                        float textWidth = font.getWidth(count, textSize);

                        font.drawText(matrixStack, count,
                                slotX + slotSize - textWidth - 0.5f,
                                slotY + slotSize - textSize,
                                textSize,
                                new Color(255, 255, 255, (int)(255 * alpha)));
                    }
                }

                RenderUtil.RECT.draw(matrixStack, slotX, slotY, slotSize, slotSize, 0.5f,
                        new Color(50, 50, 50, (int)(80 * alpha)));
            }
        }
    }

    @Override
    public void render(MatrixStack matrixStack) {
    }
}