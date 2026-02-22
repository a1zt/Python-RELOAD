package python.reload.client.features.modules.other;

import lombok.Getter;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import python.reload.api.event.Listener;
import python.reload.api.event.EventListener;
import python.reload.api.event.events.client.PacketEvent;
import python.reload.api.module.Category;
import python.reload.api.module.Module;
import python.reload.api.module.ModuleRegister;
import python.reload.api.system.configs.FriendManager;

@ModuleRegister(name = "TP Accept", category = Category.OTHER)
public class TPAcceptModule extends Module {
    @Getter private static final TPAcceptModule instance = new TPAcceptModule();

    @Override
    public void onEvent() {
        EventListener packetEvent = PacketEvent.getInstance().subscribe(new Listener<>(event -> {
            if (event.isReceive() && event.packet() instanceof GameMessageS2CPacket packet) {
                String message = packet.content().getString();

                if (message.contains("телепортироваться") || message.contains("tpaccept")) {
                    for (String name : FriendManager.getInstance().getData()) {
                        if (message.toLowerCase().contains(name.toLowerCase())) {
                            mc.player.networkHandler.sendChatCommand("tpaccept " + name);
                            break;
                        }
                    }
                }
            }
        }));

        addEvents(packetEvent);
    }
}
