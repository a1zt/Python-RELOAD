package python.reload.client.features.modules.movement.spider;

import lombok.Getter;
import python.reload.api.event.EventListener;
import python.reload.api.event.Listener;
import python.reload.api.event.events.player.move.MotionEvent;
import python.reload.api.event.events.player.other.UpdateEvent;
import python.reload.api.module.Category;
import python.reload.api.module.Module;
import python.reload.api.module.ModuleRegister;
import python.reload.api.module.setting.ModeSetting;
import python.reload.api.system.backend.Choice;
import python.reload.client.features.modules.movement.spider.modes.SpiderFunTime;
import python.reload.client.features.modules.movement.spider.modes.SpiderMatrix;

@ModuleRegister(name = "Spider", category = Category.MOVEMENT)
public class SpiderModule extends Module {
    @Getter private static final SpiderModule instance = new SpiderModule();

    private final SpiderFunTime spiderFunTime = new SpiderFunTime();
    private final SpiderMatrix spiderMatrix = new SpiderMatrix(() -> getMode().is("Matrix"));

    private final SpiderMode[] modes = new SpiderMode[]{
            spiderFunTime, spiderMatrix
    };

    private SpiderMode currentMode = spiderFunTime;

    @Getter private final ModeSetting mode = new ModeSetting("Mode").value(spiderFunTime.getName())
            .values(Choice.getValues(modes))
            .onAction(() -> {
                currentMode = (SpiderMode) Choice.getChoiceByName(getMode().getValue(), modes);
            });

    public SpiderModule() {
        addSettings(mode);

        for (SpiderMode spiderMode : modes) {
            addSettings(spiderMode.getSettings());
        }
    }

    @Override
    public void onEvent() {
        EventListener motionEvent = MotionEvent.getInstance().subscribe(new Listener<>(event -> {
            currentMode.onMotion(event);
        }));

        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            currentMode.onUpdate();
        }));

        addEvents(motionEvent, updateEvent);
    }
}
