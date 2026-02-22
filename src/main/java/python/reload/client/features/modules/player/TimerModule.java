package python.reload.client.features.modules.player;

import lombok.Getter;
import python.reload.api.event.EventListener;
import python.reload.api.event.Listener;
import python.reload.api.event.events.client.TickEvent;
import python.reload.api.module.Category;
import python.reload.api.module.Module;
import python.reload.api.module.ModuleRegister;
import python.reload.api.module.setting.SliderSetting;
import python.reload.api.system.client.TimerManager;
import python.reload.api.utils.task.TaskPriority;

@ModuleRegister(name = "Timer", category = Category.PLAYER)
public class TimerModule extends Module {
    @Getter private static final TimerModule instance = new TimerModule();

    private final SliderSetting multiplier = new SliderSetting("Multiplier").value(2f).range(0.1f, 5f).step(0.1f);

    public TimerModule() {
        addSettings(multiplier);
    }

    @Override
    public void onEvent() {
        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            TimerManager.getInstance().addTimer(multiplier.getValue(), TaskPriority.NORMAL, this, 1);
        }));

        addEvents(tickEvent);
    }
}
