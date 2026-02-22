package python.reload.client.features.modules.other;

import lombok.Getter;
import python.reload.api.event.EventListener;
import python.reload.api.event.Listener;
import python.reload.api.event.events.player.other.UpdateEvent;
import python.reload.api.module.Category;
import python.reload.api.module.Module;
import python.reload.api.module.ModuleRegister;

@ModuleRegister(name = "Fast Break", category = Category.OTHER)
public class FastBreakModule extends Module {
    @Getter private static final FastBreakModule instance = new FastBreakModule();

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            mc.interactionManager.blockBreakingCooldown = 0;
            mc.interactionManager.cancelBlockBreaking();
        }));

        addEvents(updateEvent);
    }
}
