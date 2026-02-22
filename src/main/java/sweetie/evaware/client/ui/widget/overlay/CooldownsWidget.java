package sweetie.evaware.client.ui.widget.overlay;

import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import sweetie.evaware.api.utils.other.TextUtil;
import sweetie.evaware.client.ui.widget.ContainerWidget;

import java.util.HashMap;
import java.util.Map;

public class CooldownsWidget extends ContainerWidget {
    @Override
    public String getName() {
        return "Cooldowns";
    }

    public CooldownsWidget() {
        super(120f, 100f);
    }

    @Override
    protected Map<String, ContainerElement.ColoredString> getCurrentData() {
        Map<String, ContainerElement.ColoredString> cooldownData = new HashMap<>();
        if (mc.player == null) return cooldownData;

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
                cooldownData.put(name, new ContainerElement.ColoredString(time));
            }
        }

        return cooldownData;
    }

    private int getRemainingCooldownTicks(ItemStack stack, float tickDelta) {
        ItemCooldownManager manager = mc.player.getItemCooldownManager();
        Identifier groupId = manager.getGroup(stack);
        ItemCooldownManager.Entry entry = manager.entries.get(groupId);

        if (entry != null) {
            return Math.max(0, entry.endTick() - (manager.tick + (int) tickDelta));
        }
        return 0;
    }
}