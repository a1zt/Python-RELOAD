package python.reload.client.ui.widget.overlay;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.gui.screen.ChatScreen;
import python.reload.api.module.Module;
import python.reload.api.module.ModuleManager;
import python.reload.api.system.backend.KeyStorage;
import python.reload.api.utils.color.ColorUtil;
import python.reload.api.utils.color.UIColors;
import python.reload.api.utils.render.RenderUtil;
import python.reload.api.utils.render.fonts.Font;
import python.reload.api.utils.render.fonts.Fonts;
import python.reload.client.ui.widget.Widget;
import python.reload.api.utils.animation.AnimationUtil;
import python.reload.api.utils.animation.Easing;

import java.awt.*;
import java.util.*;
import java.util.List;

public class KeybindsWidget extends Widget {
    private AnimationUtil animation = new AnimationUtil();

    public KeybindsWidget() { super(3f, 120f); }

    @Override
    public String getName() { return "Keybinds"; }

    @Override
    public void render(MatrixStack matrixStack) {
        List<Module> modules = new ArrayList<>();
        for (Module m : ModuleManager.getInstance().getModules()) {
            if (m.isEnabled() && m.hasBind()) modules.add(m);
        }
        modules.sort(Comparator.comparingInt(m -> m.getName().length()));
        Collections.reverse(modules);

        boolean shouldShow = !modules.isEmpty() || mc.currentScreen instanceof ChatScreen;
        animation.update();
        float animValue = (float) animation.getValue();

        if (shouldShow && !animation.isAlive()) animation.run(1f, 200L, Easing.SINE_OUT);
        else if (!shouldShow && animation.isAlive() && animation.getToValue() != 0) animation.run(0f, 200L, Easing.SINE_OUT);
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

        String title = "Keybinds";
        float titleWidth = font.getWidth(title, fontSize);
        float iconTitleWidth = iconWidth + textIconSpacing + titleWidth;

        float maxModuleWidth = 0;
        float maxKeyWidth = 0;
        for (Module module : modules) {
            float moduleWidth = font.getWidth(module.getName(), fontSize);
            String keyName = KeyStorage.getBind(module.getBind());
            float keyWidth = font.getWidth(keyName, fontSize);
            if (moduleWidth > maxModuleWidth) maxModuleWidth = moduleWidth;
            if (keyWidth > maxKeyWidth) maxKeyWidth = keyWidth;
        }

        float totalLineWidth = maxModuleWidth + maxKeyWidth + scaled(10f);
        float maxWidth = Math.max(iconTitleWidth, totalLineWidth);
        float bgWidth = maxWidth + padding * 2f;
        float totalHeight = bgHeight * (modules.size() + 1) + lineSpacing * modules.size();

        float currentY = y;

        float bg1X = x;
        float bg1Y = currentY;

        RenderUtil.BLUR_RECT.draw(matrixStack, bg1X, bg1Y, bgWidth, bgHeight, round - 1, new Color(18, 18, 18, (int)(240 * animValue)));

        float textY1 = bg1Y + (bgHeight - fontSize) / 2f + 0.5f;
        float iconY1 = bg1Y + (bgHeight - iconSize) / 2f;
        float currentX = bg1X + padding - 2;
        Fonts.ICON_DESHUX.drawGradientText(matrixStack, "g", currentX, iconY1, iconSize,
                ColorUtil.setAlpha(UIColors.primary(), (int)(UIColors.primary().getAlpha() * animValue)),
                ColorUtil.setAlpha(UIColors.secondary(), (int)(UIColors.secondary().getAlpha() * animValue)),
                iconWidth / 4f);
        currentX += iconWidth + textIconSpacing + 1;
        font.drawText(matrixStack, title, currentX, textY1, fontSize, new Color(185, 185, 185, (int)(255 * animValue)));
        currentY += bgHeight + lineSpacing;

        for (Module module : modules) {
            if (animValue <= 0.01f) continue;
            String keyName = KeyStorage.getBind(module.getBind());
            float bgX = x;
            float bgY = currentY;
            RenderUtil.BLUR_RECT.draw(matrixStack, bgX, bgY, bgWidth, bgHeight, round - 1, new Color(18, 18, 18, (int)(240 * animValue)));

            float textY = bgY + (bgHeight - fontSize) / 2f + 0.5f;
            currentX = bgX + padding;
            font.drawText(matrixStack, module.getName(), currentX, textY, fontSize, new Color(185, 185, 185, (int)(255 * animValue)));
            Color primaryColor = UIColors.primary();
            font.drawText(matrixStack, keyName, bgX + bgWidth - padding - font.getWidth(keyName, fontSize), textY, fontSize, new Color(primaryColor.getRed(), primaryColor.getGreen(), primaryColor.getBlue(), (int)(primaryColor.getAlpha() * animValue)));
            currentY += bgHeight + lineSpacing;
        }

        getDraggable().setWidth(bgWidth);
        getDraggable().setHeight(totalHeight);
    }
}