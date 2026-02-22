package sweetie.evaware.client.ui.widget.overlay;

import net.minecraft.text.Text;
import sweetie.evaware.api.module.Module;
import sweetie.evaware.api.module.ModuleManager;
import sweetie.evaware.api.system.backend.KeyStorage;
import sweetie.evaware.client.ui.widget.ContainerWidget;

import java.util.*;

public class KeybindsWidget extends ContainerWidget {
    public KeybindsWidget() {
        super(3f, 120f);
    }

    @Override
    public String getName() {
        return "Keybinds";
    }

    @Override
    protected Map<String, ContainerElement.ColoredString> getCurrentData() {
        Map<String, ContainerElement.ColoredString> map = new HashMap<>();
        for (Module m : ModuleManager.getInstance().getModules()) {
            if (m.isEnabled() && m.hasBind()) {
                map.put(m.getName(), new ContainerElement.ColoredString(KeyStorage.getBind(m.getBind())));
            }
        }
        return map;
    }
}