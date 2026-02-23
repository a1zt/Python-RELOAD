package python.reload.client.ui.widget.overlay;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.client.gui.screen.ChatScreen;
import python.reload.api.utils.color.ColorUtil;
import python.reload.api.utils.color.UIColors;
import python.reload.api.utils.other.TextUtil;
import python.reload.api.utils.render.RenderUtil;
import python.reload.api.utils.render.fonts.Font;
import python.reload.api.utils.render.fonts.Fonts;
import python.reload.client.ui.widget.Widget;
import python.reload.api.utils.animation.AnimationUtil;
import python.reload.api.utils.animation.Easing;

import java.awt.*;
import java.util.*;
import java.util.List;

public class CooldownsWidget extends Widget {
    private AnimationUtil animation = new AnimationUtil();
    private AnimationUtil widthAnimation = new AnimationUtil();
    private AnimationUtil heightAnimation = new AnimationUtil();

    private float cachedBgWidth = 0;
    private float cachedTotalHeight = 0;
    private boolean sizeChanged = false;

    @Override
    public String getName() { return "Cooldowns"; }

    public CooldownsWidget() {
        super(120f, 100f);
        widthAnimation.setValue(0);
        heightAnimation.setValue(0);
    }

    @Override
    public void render(MatrixStack matrixStack) {
        if (mc.player == null) return;
        List<ItemCooldownEntry> cooldownItems = getCooldownItems();
        cooldownItems.sort(Comparator.comparingInt(e -> e.itemName().length()));
        Collections.reverse(cooldownItems);

        boolean shouldShow = !cooldownItems.isEmpty() || mc.currentScreen instanceof ChatScreen;
        animation.update();
        widthAnimation.update();
        heightAnimation.update();

        float animValue = (float) animation.getValue();
        float widthAnimValue = (float) widthAnimation.getValue();
        float heightAnimValue = (float) heightAnimation.getValue();

        if (shouldShow && !animation.isAlive()) {
            animation.run(1f, 200L, Easing.SINE_OUT);
        } else if (!shouldShow && animation.isAlive() && animation.getToValue() != 0) {
            animation.run(0f, 200L, Easing.SINE_OUT);
        }

        if (animValue <= 0.01f) return;

        float x = getDraggable().getX();
        float y = getDraggable().getY();
        Font font = getSemiBoldFont();
        float fontSize = scaled(7f);
        float bgHeight = fontSize + scaled(5f);
        float padding = scaled(4f);
        float round = bgHeight * 0.3f;
        float lineSpacing = scaled(1f);
        float iconSize = fontSize + 1f;
        float iconWidth = 8f;
        float textIconSpacing = 2f;

        String title = "Cooldowns";
        float titleWidth = font.getWidth(title, fontSize);
        float iconTitleWidth = iconWidth + textIconSpacing + titleWidth;

        float maxItemWidth = 0;
        float maxTimeWidth = 0;
        for (ItemCooldownEntry entry : cooldownItems) {
            float itemWidth = font.getWidth(entry.itemName(), fontSize);
            float timeWidth = font.getWidth(entry.timeText(), fontSize);
            if (itemWidth > maxItemWidth) maxItemWidth = itemWidth;
            if (timeWidth > maxTimeWidth) maxTimeWidth = timeWidth;
        }

        float totalLineWidth = maxItemWidth + maxTimeWidth + scaled(10f);
        float maxWidth = Math.max(iconTitleWidth, totalLineWidth);
        float targetBgWidth = maxWidth + padding * 2f;
        float targetTotalHeight = bgHeight * (cooldownItems.size() + 1) + lineSpacing * cooldownItems.size();

        if (targetBgWidth != cachedBgWidth || targetTotalHeight != cachedTotalHeight) {
            cachedBgWidth = targetBgWidth;
            cachedTotalHeight = targetTotalHeight;
            sizeChanged = true;

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

        RenderUtil.BLUR_RECT.draw(matrixStack, bg1X, bg1Y, bgWidth, bgHeight, round - 1, new Color(18, 18, 18, (int)(240 * animValue)));

        float textY1 = bg1Y + (bgHeight - fontSize) / 2f + 0.5f;
        float iconY1 = bg1Y + (bgHeight - iconSize) / 2f;
        float currentX = bg1X + padding - 2;
        Fonts.ICON_DESHUX.drawGradientText(matrixStack, "i", currentX, iconY1, iconSize,
                ColorUtil.setAlpha(UIColors.primary(), (int)(UIColors.primary().getAlpha() * animValue)),
                ColorUtil.setAlpha(UIColors.secondary(), (int)(UIColors.secondary().getAlpha() * animValue)),
                iconWidth / 4f);
        currentX += iconWidth + textIconSpacing;
        font.drawText(matrixStack, title, currentX, textY1, fontSize, new Color(185, 185, 185, (int)(255 * animValue)));
        currentY += bgHeight + lineSpacing * 1.1f;

        for (ItemCooldownEntry entry : cooldownItems) {
            if (animValue <= 0.01f) continue;
            float bgX = x;
            float bgY = currentY;
            RenderUtil.BLUR_RECT.draw(matrixStack, bgX, bgY, bgWidth, bgHeight, round - 1, new Color(18, 18, 18, (int)(240 * animValue)));

            float textY = bgY + (bgHeight - fontSize) / 2f + 0.5f;
            currentX = bgX + padding;
            font.drawText(matrixStack, entry.itemName(), currentX, textY, fontSize, new Color(185, 185, 185, (int)(255 * animValue)));
            Color primaryColor = UIColors.primary();
            font.drawText(matrixStack, entry.timeText(), bgX + bgWidth - padding - font.getWidth(entry.timeText(), fontSize), textY, fontSize, new Color(primaryColor.getRed(), primaryColor.getGreen(), primaryColor.getBlue(), (int)(primaryColor.getAlpha() * animValue)));
            currentY += bgHeight + lineSpacing;
        }

        getDraggable().setWidth(bgWidth);
        getDraggable().setHeight(totalHeight);
    }

    private List<ItemCooldownEntry> getCooldownItems() {
        List<ItemCooldownEntry> entries = new ArrayList<>();
        if (mc.player == null) return entries;
        ItemCooldownManager manager = mc.player.getItemCooldownManager();
        float tickDelta = mc.getRenderTickCounter().getTickDelta(false);
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            Item item = stack.getItem();
            if (!manager.isCoolingDown(stack)) continue;
            int remaining = getRemainingCooldownTicks(stack, tickDelta);
            if (remaining > 0) {
                String name = item.getName().getString();
                String time = TextUtil.getDurationText(remaining);
                entries.add(new ItemCooldownEntry(name, time));
            }
        }
        return entries;
    }

    private int getRemainingCooldownTicks(ItemStack stack, float tickDelta) {
        ItemCooldownManager manager = mc.player.getItemCooldownManager();
        Identifier groupId = manager.getGroup(stack);
        ItemCooldownManager.Entry entry = manager.entries.get(groupId);
        if (entry != null) return Math.max(0, entry.endTick() - (manager.tick + (int) tickDelta));
        return 0;
    }

    private record ItemCooldownEntry(String itemName, String timeText) {}
}