package python.reload.client.features.modules.other;

import lombok.Getter;
import net.minecraft.entity.Entity;
import python.reload.api.event.EventListener;
import python.reload.api.event.Listener;
import python.reload.api.event.events.player.world.AttackEvent;
import python.reload.api.event.events.player.other.UpdateEvent;
import python.reload.api.module.Category;
import python.reload.api.module.Module;
import python.reload.api.module.ModuleRegister;
import python.reload.api.utils.neuro.DataCollector;

@ModuleRegister(name = "Recorder", category = Category.OTHER)
public class RecorderModule extends Module {
    @Getter private static final RecorderModule instance = new RecorderModule();
    private final DataCollector dataCollector = new DataCollector();

    private Entity target;

    @Override
    public void onDisable() {
        target = null;
        dataCollector.stopCollecting();
    }

    @Override
    public void onEnable() {
        dataCollector.startCollecting();
    }

    @Override
    public void onEvent() {
        EventListener attackEvent = AttackEvent.getInstance().subscribe(new Listener<>(event -> {
            if (target != event.entity()) target = event.entity();

            if (target != null) dataCollector.onAttack(event);
        }));

        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (target != null) dataCollector.onUpdate();
        }));

        addEvents(attackEvent, updateEvent);
    }
}
