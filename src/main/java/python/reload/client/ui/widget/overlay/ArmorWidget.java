package python.reload.client.ui.widget.overlay;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import python.reload.api.event.events.render.Render2DEvent;
import python.reload.api.utils.color.ColorUtil;
import python.reload.api.utils.color.UIColors;
import python.reload.api.utils.render.RenderUtil;
import python.reload.client.ui.widget.Widget;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ArmorWidget extends Widget {
    public ArmorWidget() {
        super(30f, 100f);
    }

    @Override
    public String getName() {
        return "Armor";
    }

    private final List<ItemStack> ITEMS = new ArrayList<>();

    @Override
    public void render(Render2DEvent.Render2DEventData event) {
        MatrixStack matrixStack = event.matrixStack();
        DrawContext context = event.context();

        updateItems();
        if (ITEMS.isEmpty()) return;

        float x = getDraggable().getX();
        float y = getDraggable().getY();

        float itemSize = scaled(13f);
        float gap = scaled(3f);

        int screenWidth = mc.getWindow().getScaledWidth();

        float longSide = (itemSize + gap) * 4f + gap;
        float shortSide = itemSize + gap * 2f;

        float threshold = longSide + gap;

        boolean isVertical = x < threshold || x > screenWidth - threshold;

        float currentWidth;
        float currentHeight;

        if (isVertical) {
            currentWidth = shortSide;
            currentHeight = longSide;
        } else {
            currentWidth = longSide;
            currentHeight = shortSide;
        }

        updateDraggable(currentHeight, currentWidth);

        RenderUtil.BLUR_RECT.draw(matrixStack, x, y, currentWidth, currentHeight, scaled(5f), new Color(18, 18, 18, 240));

        float currentX = x + gap;
        float currentY = y + gap;

        float scaleFactor = itemSize / 16f;

        for (int i = 0; i < ITEMS.size(); i++) {
            ItemStack item = ITEMS.get(i);

            matrixStack.push();
            matrixStack.translate(currentX, currentY, 0f);
            matrixStack.scale(scaleFactor, scaleFactor, 1f);
            context.drawItem(item, 0, 0);
            matrixStack.pop();

            matrixStack.push();
            matrixStack.translate(currentX, currentY, 0f);
            float barHeight = scaleFactor * 2f;
            drawBar(matrixStack, item, 0, itemSize - barHeight, itemSize, barHeight, barHeight * 0.7f);
            matrixStack.pop();

            if (i < ITEMS.size() - 1) {
                if (isVertical) {
                    float lineX = currentX - gap / 2f;
                    float lineY = currentY + itemSize + gap / 2f;
                    float lineHeight = 1;
                    RenderUtil.RECT.draw(matrixStack, lineX, lineY, itemSize + gap, lineHeight, 0, new Color(255, 255, 255, 40));
                } else {
                    float lineX = currentX + itemSize + gap / 2f;
                    float lineY = currentY - gap / 2f;
                    float lineWidth = 1;
                    RenderUtil.RECT.draw(matrixStack, lineX, lineY, lineWidth, itemSize + gap, 0, new Color(255, 255, 255, 40));
                }
            }

            float next = itemSize + gap;
            if (isVertical) currentY += next;
            else currentX += next;
        }
    }

    private void drawBar(MatrixStack matrixStack, ItemStack item, float x, float y, float width, float height, float offset) {
        if (!item.isDamageable()) return;

        float maxDamage = item.getMaxDamage();
        float currentDamage = item.getDamage();
        float progress = (maxDamage - currentDamage) / maxDamage;

        Color color = ColorUtil.interpolate(UIColors.positiveColor(), UIColors.negativeColor(), progress);

        RenderUtil.RECT.draw(matrixStack, x + offset, y,
                (width - offset * 2f) * progress, height,
                height * 0.2f, color);
    }

    private void updateDraggable(float height, float width) {
        getDraggable().setHeight(height);
        getDraggable().setWidth(width);
    }

    private void updateItems() {
        ITEMS.clear();
        if (mc.player == null) return;
        PlayerEntity player = mc.player;

        List<ItemStack> armor = player.getInventory().armor;
        for (int i = armor.size() - 1; i >= 0; i--) {
            ItemStack stack = armor.get(i);
            ITEMS.add(stack);
        }
    }

    @Override
    public void render(MatrixStack matrixStack) {}
}