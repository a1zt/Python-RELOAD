package python.reload.client.ui.widget.overlay;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
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

public class PotionsWidget extends Widget {
    private AnimationUtil animation = new AnimationUtil();
    private AnimationUtil widthAnimation = new AnimationUtil();
    private AnimationUtil heightAnimation = new AnimationUtil();

    private float cachedBgWidth = 0;
    private float cachedTotalHeight = 0;

    private static final Identifier[] BAD_EFFECTS = {
            Identifier.of("minecraft", "wither"), Identifier.of("minecraft", "poison"), Identifier.of("minecraft", "slowness"),
            Identifier.of("minecraft", "weakness"), Identifier.of("minecraft", "mining_fatigue"), Identifier.of("minecraft", "nausea"),
            Identifier.of("minecraft", "blindness"), Identifier.of("minecraft", "hunger"), Identifier.of("minecraft", "levitation"),
            Identifier.of("minecraft", "unluck")
    };

    private static final Identifier[] COOL_EFFECTS = {
            Identifier.of("minecraft", "speed"), Identifier.of("minecraft", "strength"), Identifier.of("minecraft", "regeneration")
    };

    public PotionsWidget() {
        super(3f, 120f);
        widthAnimation.setValue(0);
        heightAnimation.setValue(0);
    }

    @Override
    public String getName() { return "Potions"; }

    @Override
    public void render(MatrixStack matrixStack) {
        if (mc.player == null) return;
        Collection<StatusEffectInstance> effects = mc.player.getActiveStatusEffects().values();
        List<StatusEffectInstance> effectList = new ArrayList<>(effects);
        effectList.sort(Comparator.comparingInt(e -> Language.getInstance().get(e.getTranslationKey()).length()));
        Collections.reverse(effectList);

        boolean shouldShow = !effectList.isEmpty() || mc.currentScreen instanceof ChatScreen;
        animation.update();
        widthAnimation.update();
        heightAnimation.update();

        updateEffectAnimations(effectList);

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

        String title = "Potions";
        float titleWidth = font.getWidth(title, fontSize);
        float iconTitleWidth = iconWidth + textIconSpacing + titleWidth;

        float maxEffectWidth = 0;
        float maxDurationWidth = 0;
        for (StatusEffectInstance effect : effectList) {
            Identifier id = effect.getEffectType().getKey().get().getValue();
            String level = effect.getAmplifier() > 0 ? " " + (effect.getAmplifier() + 1) : "";
            String name = Language.getInstance().get(effect.getTranslationKey()) + level;
            String durationText = TextUtil.getDurationText(effect.getDuration());
            float nameWidth = font.getWidth(name, fontSize);
            float durationWidth = font.getWidth(durationText, fontSize);
            if (nameWidth > maxEffectWidth) maxEffectWidth = nameWidth;
            if (durationWidth > maxDurationWidth) maxDurationWidth = durationWidth;
        }

        float totalLineWidth = maxEffectWidth + maxDurationWidth + scaled(10f);
        float maxWidth = Math.max(iconTitleWidth, totalLineWidth);
        float targetBgWidth = maxWidth + padding * 2f;
        float targetTotalHeight = bgHeight * (effectList.size() + 1) + lineSpacing * effectList.size();

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

        RenderUtil.BLUR_RECT.draw(matrixStack, bg1X, bg1Y, bgWidth, bgHeight, round - 1, new Color(18, 18, 18, (int)(240 * animValue)));

        float textY1 = bg1Y + (bgHeight - fontSize) / 2f + 0.5f;
        float iconY1 = bg1Y + (bgHeight - iconSize) / 2f;
        float currentX = bg1X + padding - 2;
        Fonts.ICON_ESSENS.drawGradientText(matrixStack, "h", currentX, iconY1, iconSize,
                ColorUtil.setAlpha(UIColors.primary(), (int)(UIColors.primary().getAlpha() * animValue)),
                ColorUtil.setAlpha(UIColors.secondary(), (int)(UIColors.secondary().getAlpha() * animValue)),
                iconWidth / 4f);
        currentX += iconWidth + textIconSpacing;
        font.drawText(matrixStack, title, currentX, textY1, fontSize, new Color(185, 185, 185, (int)(255 * animValue)));
        currentY += bgHeight + lineSpacing * 1.1f;

        for (int i = 0; i < effectList.size(); i++) {
            StatusEffectInstance effect = effectList.get(i);
            if (animValue <= 0.01f) continue;

            Identifier id = effect.getEffectType().getKey().get().getValue();
            String level = effect.getAmplifier() > 0 ? " " + (effect.getAmplifier() + 1) : "";
            String name = Language.getInstance().get(effect.getTranslationKey()) + level;
            String durationText = TextUtil.getDurationText(effect.getDuration());

            float effectAnim = getEffectAnimation(name);
            if (effectAnim <= 0.01f) continue;

            float slideOffset = (1f - effectAnim) * 5f;

            float bgX = x;
            float bgY = currentY - slideOffset;

            float bgAlpha = animValue * effectAnim;
            RenderUtil.BLUR_RECT.draw(matrixStack, bgX, bgY, bgWidth, bgHeight, round - 1, new Color(18, 18, 18, (int)(240 * bgAlpha)));

            float textY = bgY + (bgHeight - fontSize) / 2f + 0.5f;
            currentX = bgX + padding;

            Color effectColor = isBadEffect(id) ? new Color(200, 100, 100) : isCoolEffect(id) ? new Color(100, 200, 100) : new Color(185, 185, 185);
            font.drawText(matrixStack, name, currentX, textY, fontSize,
                    new Color(effectColor.getRed(), effectColor.getGreen(), effectColor.getBlue(),
                            (int)(effectColor.getAlpha() * bgAlpha)));

            Color durationColor = isBadEffect(id) ? ColorUtil.flashingColor(new Color(200, 100, 100), new Color(185, 185, 185)) : isCoolEffect(id) ? ColorUtil.flashingColor(new Color(100, 200, 100), new Color(185, 185, 185)) : new Color(185, 185, 185);
            float durationX = bgX + bgWidth - padding - font.getWidth(durationText, fontSize);
            font.drawText(matrixStack, durationText, durationX, textY, fontSize,
                    new Color(durationColor.getRed(), durationColor.getGreen(), durationColor.getBlue(),
                            (int)(durationColor.getAlpha() * bgAlpha)));

            currentY += bgHeight + lineSpacing;
        }

        getDraggable().setWidth(bgWidth);
        getDraggable().setHeight(totalHeight);
    }

    private final Map<String, AnimationUtil> effectAnimations = new HashMap<>();
    private final Map<String, Long> effectAddedTime = new HashMap<>();

    private void updateEffectAnimations(List<StatusEffectInstance> currentEffects) {
        Set<String> currentEffectNames = new HashSet<>();

        for (StatusEffectInstance effect : currentEffects) {
            String level = effect.getAmplifier() > 0 ? " " + (effect.getAmplifier() + 1) : "";
            String name = Language.getInstance().get(effect.getTranslationKey()) + level;
            currentEffectNames.add(name);

            if (!effectAnimations.containsKey(name)) {
                AnimationUtil anim = new AnimationUtil();
                anim.setValue(0.0);
                anim.run(1.0, 300L, Easing.SINE_OUT);
                effectAnimations.put(name, anim);
                effectAddedTime.put(name, System.currentTimeMillis());
            }
        }

        Iterator<Map.Entry<String, AnimationUtil>> it = effectAnimations.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, AnimationUtil> entry = it.next();
            if (!currentEffectNames.contains(entry.getKey())) {
                entry.getValue().run(0.0, 200L, Easing.SINE_OUT);
                if (entry.getValue().getValue() <= 0.01) {
                    it.remove();
                    effectAddedTime.remove(entry.getKey());
                }
            }
        }

        for (AnimationUtil anim : effectAnimations.values()) {
            anim.update();
        }
    }

    private float getEffectAnimation(String effectName) {
        AnimationUtil anim = effectAnimations.get(effectName);
        if (anim != null) {
            return (float) anim.getValue();
        }
        return 1.0f;
    }

    private boolean isBadEffect(Identifier id) {
        for (Identifier badId : BAD_EFFECTS) if (badId.equals(id)) return true;
        return false;
    }

    private boolean isCoolEffect(Identifier id) {
        for (Identifier coolId : COOL_EFFECTS) if (coolId.equals(id)) return true;
        return false;
    }
}