package python.reload.client.features.modules.other;

import lombok.Getter;
import net.minecraft.client.gui.screen.DeathScreen;
import python.reload.api.event.Listener;
import python.reload.api.event.EventListener;
import python.reload.api.event.events.client.TickEvent;
import python.reload.api.module.Category;
import python.reload.api.module.Module;
import python.reload.api.module.ModuleRegister;

@ModuleRegister(name = "Auto Respawn", category = Category.OTHER)
public class AutoRespawnModule extends Module {
    @Getter private static final AutoRespawnModule instance = new AutoRespawnModule();

    @Override
    public void onEvent() {
        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.currentScreen instanceof DeathScreen) {
                if (mc.player.deathTime > 2) {
                    mc.player.requestRespawn();
                    mc.setScreen(null);
                }
            }
        }));

        addEvents(tickEvent);
    }
}
