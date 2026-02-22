package sweetie.evaware.client.ui.widget.overlay;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import sweetie.evaware.api.utils.color.ColorUtil;
import sweetie.evaware.api.utils.color.UIColors;
import sweetie.evaware.api.utils.other.TextUtil;
import sweetie.evaware.client.ui.widget.ContainerWidget;

import java.awt.*;
import java.util.*;

public class PotionsWidget extends ContainerWidget {
    public PotionsWidget() {
        super(3f, 120f);
    }

    @Override
    public String getName() {
        return "Potions";
    }

    private static final Identifier[] BAD_EFFECTS = {
            Identifier.of("minecraft", "wither"),
            Identifier.of("minecraft", "poison"),
            Identifier.of("minecraft", "slowness"),
            Identifier.of("minecraft", "weakness"),
            Identifier.of("minecraft", "mining_fatigue"),
            Identifier.of("minecraft", "nausea"),
            Identifier.of("minecraft", "blindness"),
            Identifier.of("minecraft", "hunger"),
            Identifier.of("minecraft", "levitation"),
            Identifier.of("minecraft", "unluck")
    };

    private static final Identifier[] COOL_EFFECTS = {
            Identifier.of("minecraft", "speed"),
            Identifier.of("minecraft", "strength"),
            Identifier.of("minecraft", "regeneration")
    };

    @Override
    protected Map<String, ContainerElement.ColoredString> getCurrentData() {
        Map<String, ContainerElement.ColoredString> map = new HashMap<>();
        for (StatusEffectInstance effect : mc.player.getActiveStatusEffects().values()) {
            Identifier id = effect.getEffectType().getKey().get().getValue();

            Color textColor = UIColors.textColor();
            Color miss_you = isBadEffect(id) ? ColorUtil.flashingColor(UIColors.negativeColor(), textColor) :
                                isCoolEffect(id) ? ColorUtil.flashingColor(UIColors.positiveColor(), textColor) :
                                UIColors.textColor();



            String level = effect.getAmplifier() > 0 ? " " + (effect.getAmplifier() + 1) : "";
            String name = Language.getInstance().get(effect.getTranslationKey()) + level;
            String durationText = TextUtil.getDurationText(effect.getDuration());
            map.put(name, new ContainerElement.ColoredString(durationText, miss_you));
        }
        return map;
    }

    private boolean isBadEffect(Identifier id) {
        for (Identifier badId : BAD_EFFECTS) {
            if (badId.equals(id)) return true;
        }
        return false;
    }

    private boolean isCoolEffect(Identifier id) {
        for (Identifier coolId : COOL_EFFECTS) {
            if (coolId.equals(id)) return true;
        }
        return false;
    }
}