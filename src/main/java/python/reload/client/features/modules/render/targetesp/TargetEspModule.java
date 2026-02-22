package python.reload.client.features.modules.render.targetesp;

import lombok.Getter;
import python.reload.api.event.Listener;
import python.reload.api.event.events.player.other.UpdateEvent;
import python.reload.api.event.events.render.Render3DEvent;
import python.reload.api.module.Category;
import python.reload.api.module.Module;
import python.reload.api.module.ModuleRegister;
import python.reload.api.module.setting.BooleanSetting;
import python.reload.api.module.setting.ModeSetting;
import python.reload.api.module.setting.SliderSetting;
import python.reload.client.features.modules.render.targetesp.modes.TargetEspCrystals;
import python.reload.client.features.modules.render.targetesp.modes.TargetEspTexture;

@ModuleRegister(name = "Target Esp", category = Category.RENDER)
public class TargetEspModule extends Module {
    @Getter public static final TargetEspModule instance = new TargetEspModule();

    private final TargetEspTexture espTexture = new TargetEspTexture();
    private final TargetEspCrystals espCrystals = new TargetEspCrystals();
    private TargetEspMode currentMode = espTexture;

    @Getter private final ModeSetting mode = new ModeSetting("Mode").value("Crystals").values("Marker", "Crystals");

    private final ModeSetting animation = new ModeSetting("Animation").value("In").values("In", "Out", "None");
    private final SliderSetting duration = new SliderSetting("Duration").value(3f).range(1f, 20f).step(1f);

    private final SliderSetting crystalsCount = new SliderSetting("Amount").value(14f).range(1f, 20f).step(1f).setVisible(() -> mode.is("Crystals"));
    private final SliderSetting crystalsSpeed = new SliderSetting("Speed").value(3f).range(0f, 5f).step(0.5f).setVisible(() -> mode.is("Crystals"));
    public final BooleanSetting lastPosition = new BooleanSetting("Last position").value(true);

    public TargetEspModule() {
        addSettings(mode, animation, duration, crystalsCount, crystalsSpeed, lastPosition);

        mode.onAction(() -> {
            currentMode = mode.is("Crystals") ? espCrystals : espTexture;
        });
    }

    @Override
    public void onEvent() {
        addEvents(
                Render3DEvent.getInstance().subscribe(new Listener<>(event -> {
                    TargetEspMode.updatePositions();
                    currentMode.onRender3D(event);
                })),
                UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
                    currentMode.updateAnimation(duration.getValue().longValue() * 50, animation.getValue(), 1.0f, 0f, 2f);
                    currentMode.updateTarget();
                    currentMode.onUpdate();
                }))
        );
    }

    public int getCrystalsCount() { return crystalsCount.getValue().intValue(); }
    public float getCrystalsSpeed() { return crystalsSpeed.getValue().floatValue(); }
}