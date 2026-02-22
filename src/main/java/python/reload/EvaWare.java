package python.reload;

import lombok.Getter;
import net.fabricmc.api.ClientModInitializer;
import python.reload.api.command.CommandManager;
import python.reload.api.module.ModuleManager;
import python.reload.api.system.DiscordHook;
import python.reload.api.system.configs.ConfigManager;
import python.reload.api.system.configs.ConfigSkin;
import python.reload.api.system.configs.FriendManager;
import python.reload.api.system.configs.MacroManager;
import python.reload.api.system.draggable.DraggableManager;
import python.reload.api.system.files.FileManager;
import python.reload.api.utils.other.SoundUtil;
import python.reload.api.utils.render.KawaseBlurProgram;
import python.reload.api.utils.render.fonts.Fonts;
import python.reload.api.utils.rotation.manager.RotationManager;
import python.reload.client.services.HeartbeatService;
import python.reload.client.services.RenderService;
import python.reload.client.ui.theme.ThemeEditor;
import python.reload.client.ui.widget.WidgetManager;

public class EvaWare implements ClientModInitializer {
    @Getter
	 private static EvaWare instance = new EvaWare();
    @Getter
    private static String version="v10";

    @Override
	public void onInitializeClient() {
        instance = this;

        SoundUtil.load();

        loadManagers();
        loadServices();
        loadFiles();
    }

    public void postLoad() {
        ModuleManager.getInstance().getModules().sort((a, b) -> Float.compare(
                Fonts.PS_MEDIUM.getWidth(b.getName(), 7f),
                Fonts.PS_MEDIUM.getWidth(a.getName(), 7f)
        ));

        KawaseBlurProgram.load();
    }

    private void loadFiles() {
        ConfigManager.getInstance().load("autoConfig");
        DraggableManager.getInstance().load();
        FriendManager.getInstance().load();
        MacroManager.getInstance().load();
    }

    private void loadManagers() {
        WidgetManager.getInstance().load();
        RotationManager.getInstance().load();

        ModuleManager.getInstance().load();
        CommandManager.getInstance().load();

        ThemeEditor.getInstance().load();
    }

    private void loadServices() {
        HeartbeatService.getInstance().load();
        RenderService.getInstance().load();
        ConfigSkin.getInstance().load();

        DiscordHook.startRPC();
    }

    public void onClose() {
        ConfigManager.getInstance().save("autoConfig");
        FileManager.getInstance().save();
        ThemeEditor.getInstance().save(true);
        DraggableManager.getInstance().save();
        MacroManager.getInstance().save();

        DiscordHook.stopRPC();
    }
}