package python.reload.client.features.modules.player;

import lombok.Getter;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import python.reload.api.event.Listener;
import python.reload.api.event.EventListener;
import python.reload.api.event.events.player.other.UpdateEvent;
import python.reload.api.module.Category;
import python.reload.api.module.Module;
import python.reload.api.module.ModuleRegister;
import python.reload.api.module.setting.SliderSetting;
import python.reload.api.module.setting.ModeSetting;
import python.reload.api.system.configs.FriendManager;

@ModuleRegister(name = "Auto Leave", category = Category.PLAYER)
public class AutoLeaveModule extends Module {
    @Getter private static final AutoLeaveModule instance = new AutoLeaveModule();

    private final SliderSetting distance = new SliderSetting("Distance").value(50f).range(1f, 100f).step(1f);
    private final ModeSetting action = new ModeSetting("Action").value("Spawn").values("Hub", "Spawn", "Home");

    public AutoLeaveModule() {
        addSettings(distance, action);
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            handleUpdateEvent();
        }));

        addEvents(updateEvent);
    }

    private void handleUpdateEvent() {
        for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            if (mc.player == player) continue;
            if (FriendManager.getInstance().contains(player.getName().getString())) continue;

            if (player.getPos().distanceTo(mc.player.getPos()) <= distance.getValue()) {
                handleLeave();
                toggle();
                break;
            }
        }
    }

    private void handleLeave() {
        switch (action.getValue()) {
            case "Hub" -> mc.player.networkHandler.sendChatCommand("hub");
            case "Spawn" -> mc.player.networkHandler.sendChatCommand("spawn");
            case "Home" -> mc.player.networkHandler.sendChatCommand("home home");
        }
    }
}
