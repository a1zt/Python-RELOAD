package python.reload.client.features.modules.movement.nitrofirework;

import lombok.Getter;
import python.reload.api.module.Category;
import python.reload.api.module.Module;
import python.reload.api.module.ModuleRegister;
import python.reload.api.module.setting.ModeSetting;
import python.reload.api.system.backend.Choice;
import python.reload.client.features.modules.movement.nitrofirework.modes.NitroFireworkCustom;
import python.reload.client.features.modules.movement.nitrofirework.modes.NitroFireworkLG;
import python.reload.client.features.modules.movement.nitrofirework.modes.*;

@ModuleRegister(name = "Nitro Firework", category = Category.MOVEMENT)
public class NitroFireworkModule extends Module {
    @Getter private static final NitroFireworkModule instance = new NitroFireworkModule();

    private final NitroFireworkCustom nitroFireworkCustom = new NitroFireworkCustom(() -> getMode().is("Custom"));
    private final NitroFireworkLG nitroFireworkLG = new NitroFireworkLG(() -> getMode().is("Grim"));

    private final NitroFireworkMode[] modes = new NitroFireworkMode[]{
            nitroFireworkCustom, nitroFireworkLG
    };

    public NitroFireworkMode currentMode = nitroFireworkCustom;

    @Getter private final ModeSetting mode = new ModeSetting("Mode").value("Custom").values(
            Choice.getValues(modes)
    ).onAction(() -> {
        currentMode = (NitroFireworkMode) Choice.getChoiceByName(getMode().getValue(), modes);
    });

    public NitroFireworkModule() {
        addSettings(mode);
        getSettings().addAll(nitroFireworkCustom.getSettings());
        getSettings().addAll(nitroFireworkLG.getSettings());
    }

    @Override
    public void onEvent() {

    }
}
