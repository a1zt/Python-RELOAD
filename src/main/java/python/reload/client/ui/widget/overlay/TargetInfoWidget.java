package python.reload.client.ui.widget.overlay;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import python.reload.api.utils.animation.AnimationUtil;
import python.reload.api.utils.color.ColorUtil;
import python.reload.api.utils.color.UIColors;
import python.reload.api.utils.math.MathUtil;
import python.reload.api.utils.render.RenderUtil;
import python.reload.client.features.modules.combat.AuraModule;
import python.reload.client.ui.widget.Widget;

import java.awt.*;

public class TargetInfoWidget extends Widget {
    @Override
    public String getName() {
        return "Target info";
    }

    public TargetInfoWidget() {
        super(30f, 30f);
    }

    private final AnimationUtil showAnimation = new AnimationUtil();
    private float healthAnimation = 0f;
    private LivingEntity target;

    @Override
    public void render(MatrixStack matrixStack) {
        update();
        LivingEntity pretendTarget = getTarget();

        if (pretendTarget != null) {
            target = pretendTarget;
        }

        if (showAnimation.getValue() <= 0.0 || target == null) return;

        healthAnimation = MathHelper.clamp(MathUtil.interpolate(healthAnimation, target.getHealth() / target.getMaxHealth(), 0.3f), 0f, 1f);
        float x = getDraggable().getX();
        float y = getDraggable().getY();

        float anim = (float) showAnimation.getValue();

        float padding = scaled(4f);
        float innerPadding = scaled(4f);
        float avatarSize = scaled(20f);
        float nameSize = scaled(7f);
        float hpSize = scaled(6f);
        float spacing = scaled(2.5f);
        float healthBarHeight = scaled(3.5f);

        String nameText = target.getName().getString();
        float nameWidth = getMediumFont().getWidth(nameText, nameSize);
        float nameHeight = getMediumFont().getHeight(nameSize);

        float currentHealth = target.getHealth();
        float maxHealth = Math.max(target.getMaxHealth(), 1.0f);
        float absorptionAmount = target.getAbsorptionAmount();

        String hpText = String.format("HP: %.1f", currentHealth);
        float hpWidth = getMediumFont().getWidth(hpText, hpSize);
        float hpHeight = getMediumFont().getHeight(hpSize);

        boolean hasAbsorption = absorptionAmount > 0;
        String absorptionText = hasAbsorption ? String.format("%.1f", absorptionAmount) : "";
        float absorptionTextWidth = hasAbsorption ? getMediumFont().getWidth(absorptionText, hpSize) : 0.0f;
        float combinedHpWidth = hasAbsorption ? hpWidth + absorptionTextWidth + scaled(12f) : hpWidth;

        float contentWidth = Math.max(nameWidth, combinedHpWidth);
        float healthBarWidth = Math.max(contentWidth, scaled(55f));

        float totalWidth = padding + avatarSize + innerPadding + healthBarWidth + padding;
        float totalHeight = padding + nameHeight + spacing + hpHeight + spacing + healthBarHeight + padding;

        float healthRatio = MathHelper.clamp(currentHealth / maxHealth, 0.0f, 1.0f);
        float absorptionRatio = MathHelper.clamp(absorptionAmount / maxHealth, 0.0f, 1.0f);

        int bgAlpha = (int) (240 * anim);
        Color backgroundColor = new Color(18, 18, 18, bgAlpha);
        float round = scaled(3f);
        RenderUtil.BLUR_RECT.draw(matrixStack, x, y, totalWidth, totalHeight, round + 1, backgroundColor);

        float avatarX = x + padding;
        float avatarY = y + (totalHeight - avatarSize) / 2.0f;

        if (target instanceof PlayerEntity player) {
            Color headColor = ColorUtil.setAlpha(Color.WHITE, (int)(255 * anim));
            float headRadius = avatarSize * 0.15f - 2;
            RenderUtil.TEXTURE_RECT.drawRoundedHead(matrixStack, player, avatarX, avatarY, avatarSize, avatarSize, headRadius, headColor);
        } else {
            float headRadius = avatarSize * 0.15f - 2;
            float headFontSize = avatarSize * 0.8f;
            Color bgColor = new Color(60, 60, 60, (int)(255 * anim));
            RenderUtil.RECT.draw(matrixStack, avatarX, avatarY, avatarSize, avatarSize, headRadius, bgColor);
            getSemiBoldFont().drawCenteredText(matrixStack, "?", avatarX + avatarSize / 2f, avatarY + avatarSize / 2f - headFontSize / 2f, headFontSize, UIColors.textColor((int)(255 * anim)));
        }

        float textX = avatarX + avatarSize + innerPadding;
        float nameY = y + padding;
        Color nameColor = new Color(185, 185, 185, (int)(255 * anim));
        getMediumFont().drawText(matrixStack, nameText, textX, nameY, nameSize, nameColor);

        float hpY = nameY + nameHeight + spacing;
        getMediumFont().drawText(matrixStack, hpText, textX, hpY, hpSize, nameColor);

        if (hasAbsorption && anim > 0.01f) {
            float absorptionDrawX = x + totalWidth - padding - absorptionTextWidth;
            Color goldColor = new Color(255, 217, 13, (int)(255 * anim));
            getMediumFont().drawText(matrixStack, absorptionText, absorptionDrawX, hpY, hpSize, goldColor);
        }

        if (anim > 0.01f) {
            float healthBarX = textX;
            float healthBarY = hpY + hpHeight + spacing;
            Color healthBarBg = new Color(0, 0, 0, (int)(150 * anim));
            RenderUtil.RECT.draw(matrixStack, healthBarX, healthBarY, healthBarWidth, healthBarHeight, scaled(1f), healthBarBg);

            float healthFill = healthBarWidth * healthRatio;
            if (healthFill > 0) {
                Color primaryColor = UIColors.primary();
                Color secondaryColor = UIColors.secondary();
                Color primAlpha = ColorUtil.setAlpha(primaryColor, (int)(255 * anim));
                Color secAlpha = ColorUtil.setAlpha(secondaryColor, (int)(255 * anim));
                RenderUtil.GRADIENT_RECT.draw(matrixStack, healthBarX, healthBarY, healthFill, healthBarHeight, scaled(1f), primAlpha, secAlpha, primAlpha, secAlpha);
            }

            if (hasAbsorption && absorptionRatio > 0 && anim > 0.01f) {
                float absorptionOverlay = healthBarWidth * MathHelper.clamp(absorptionAmount / maxHealth, 0.0f, 1.0f);
                if (absorptionOverlay > 0) {
                    Color goldStart = new Color(57, 34, 4, (int)(200 * anim));
                    Color goldEnd = new Color(255, 217, 13, (int)(255 * anim));
                    RenderUtil.GRADIENT_RECT.draw(matrixStack, healthBarX, healthBarY, Math.min(absorptionOverlay, healthBarWidth), healthBarHeight, scaled(1f), goldStart, goldEnd, goldStart, goldEnd);
                }
            }
        }

        getDraggable().setWidth(totalWidth);
        getDraggable().setHeight(totalHeight);
    }

    private void update() {
        showAnimation.update();
        showAnimation.run(getTarget() != null ? 1.0 : 0.0, getDuration(), getEasing());
    }

    private LivingEntity getTarget() {
        AuraModule aura = AuraModule.getInstance();

        if (aura.isEnabled() && aura.target != null) {
            return aura.target;
        }

        if (mc.currentScreen instanceof ChatScreen) {
            return mc.player;
        }

        return null;
    }
}