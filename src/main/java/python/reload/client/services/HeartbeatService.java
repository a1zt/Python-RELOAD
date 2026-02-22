package python.reload.client.services;

import lombok.Getter;
import net.minecraft.client.gui.screen.ChatScreen;
import python.reload.api.event.Listener;
import python.reload.api.event.events.client.KeyEvent;
import python.reload.api.event.events.other.ScreenEvent;
import python.reload.api.event.events.client.TickEvent;
import python.reload.api.event.events.render.Render2DEvent;
import python.reload.api.module.ModuleManager;
import python.reload.api.system.client.GpsManager;
import python.reload.api.system.configs.ConfigSkin;
import python.reload.api.system.configs.MacroManager;
import python.reload.api.system.draggable.DraggableManager;
import python.reload.api.system.interfaces.QuickImports;
import python.reload.api.utils.other.ScreenUtil;
import python.reload.api.utils.other.SlownessManager;

public class HeartbeatService implements QuickImports {
    @Getter private static final HeartbeatService instance = new HeartbeatService();

    public void load() {
        keyEvent();
        render2dEvent();
        tickEvent();
        screenEvent();
    }

    private void screenEvent() {
        ScreenEvent.getInstance().subscribe(new Listener<>(event -> {
            ScreenUtil.drawButton(event);
        }));
    }

    private void tickEvent() {
        TickEvent.getInstance().subscribe(new Listener<>(event -> {
            SlownessManager.tick();

            ConfigSkin.getInstance().fetchSkin();
        }));
    }

    private void render2dEvent() {
        Render2DEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.currentScreen instanceof ChatScreen) {
                DraggableManager.getInstance().getDraggables().forEach((s, draggable) -> {
                    if (draggable.getModule().isEnabled()) {
                        draggable.onDraw();
                    }
                });
            }

            GpsManager.getInstance().update(event.context());
        }));
    }

    private void keyEvent() {
        KeyEvent.getInstance().subscribe(new Listener<>(event -> {
            if (event.action() != 1 || event.key() == -999 || event.key() == -1) return;

            int action = event.action();
            int key = event.key() + (event.mouse() ? -100 : 0);

            if (mc.currentScreen == null) {
                ModuleManager.getInstance().getModules().forEach(module -> {
                    int bind = module.getBind();
                    if (bind == key && module.hasBind()) {
                        module.toggle();
                    }
                });

                MacroManager.getInstance().onKeyPressed(key);
            }
        }));
    }
}
