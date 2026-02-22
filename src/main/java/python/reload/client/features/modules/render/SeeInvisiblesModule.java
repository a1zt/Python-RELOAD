package python.reload.client.features.modules.render;

import lombok.Getter;
import python.reload.api.event.EventListener;
import python.reload.api.event.Listener;
import python.reload.api.event.events.render.EntityColorEvent;
import python.reload.api.module.Category;
import python.reload.api.module.Module;
import python.reload.api.module.ModuleRegister;
import python.reload.api.module.setting.SliderSetting;
import python.reload.api.utils.color.ColorUtil;

import java.awt.*;

@ModuleRegister(name = "See Invisibles", category = Category.RENDER)
public class SeeInvisiblesModule extends Module {
    @Getter private static final SeeInvisiblesModule instance = new SeeInvisiblesModule();

    private final SliderSetting alpha = new SliderSetting("Alpha").value(0.3f).range(0.0f, 1f).step(0.1f);

    public SeeInvisiblesModule() {
        addSettings(alpha);
    }

    // TODO: пофиксить hurt.
    @Override
    public void onEvent() {
        EventListener entityColorEvent = EntityColorEvent.getInstance().subscribe(new Listener<>(event -> {
            int donichka = (int) (alpha.getValue() * 255);
            event.color(ColorUtil.setAlpha(new Color(event.color()), donichka).getRGB());
            EntityColorEvent.getInstance().setCancel(true);
        }));

        addEvents(entityColorEvent);
    }
}
