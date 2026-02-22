package python.reload.client.features.modules.movement.noslow;

import lombok.Getter;
import python.reload.api.event.Listener;
import python.reload.api.event.EventListener;
import python.reload.api.event.events.client.TickEvent;
import python.reload.api.event.events.player.other.UpdateEvent;
import python.reload.api.module.Category;
import python.reload.api.module.Module;
import python.reload.api.module.ModuleRegister;
import python.reload.api.module.setting.ModeSetting;
import python.reload.api.system.backend.Choice;
import python.reload.client.features.modules.movement.noslow.modes.NoSlowCancel;
import python.reload.client.features.modules.movement.noslow.modes.NoSlowGrim;
import python.reload.client.features.modules.movement.noslow.modes.NoSlowSlotUpdate;
import python.reload.client.features.modules.movement.noslow.modes.*;

@ModuleRegister(name = "No Slow", category = Category.MOVEMENT)
public class NoSlowModule extends Module {
    @Getter private static final NoSlowModule instance = new NoSlowModule();

    private final NoSlowCancel noSlowCancel = new NoSlowCancel();
    private final NoSlowSlotUpdate noSlowSlotUpdate = new NoSlowSlotUpdate();
    private final NoSlowGrim noSlowGrim = new NoSlowGrim();

    private final NoSlowMode[] modes = new NoSlowMode[]{
            noSlowCancel, noSlowSlotUpdate, noSlowGrim
    };

    private NoSlowMode currentMode = noSlowCancel;

    @Getter private final ModeSetting mode = new ModeSetting("Mode").value("Cancel").values(
            Choice.getValues(modes)
    ).onAction(() -> {
        currentMode = (NoSlowMode) Choice.getChoiceByName(getMode().getValue(), modes);
    });
    @Getter private final ModeSetting grimMode = new ModeSetting("Grim mode").value("Tick").values("Tick", "Old").setVisible(() -> mode.is("Grim")).onAction(() -> {
        noSlowGrim.bypassType = switch (getGrimMode().getValue()) {
            case "Tick" -> NoSlowGrim.BypassType.TICK;

            default -> NoSlowGrim.BypassType.OLD;
        };
    });

    public NoSlowModule() {
        addSettings(mode, grimMode);
    }

    public boolean doUseNoSlow() {
        return isEnabled() && mc.player.isUsingItem() && currentMode.slowingCancel();
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            currentMode.onUpdate();
        }));

        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            currentMode.onTick();
        }));

        addEvents(updateEvent, tickEvent);
    }
}
