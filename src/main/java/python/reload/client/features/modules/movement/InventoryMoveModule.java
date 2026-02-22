package python.reload.client.features.modules.movement;

import lombok.Getter;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import python.reload.api.event.Listener;
import python.reload.api.event.EventListener;
import python.reload.api.event.events.client.PacketEvent;
import python.reload.api.event.events.player.other.CloseScreenEvent;
import python.reload.api.event.events.player.other.UpdateEvent;
import python.reload.api.module.Category;
import python.reload.api.module.Module;
import python.reload.api.module.ModuleRegister;
import python.reload.api.module.setting.ModeSetting;
import python.reload.api.system.client.ThreadManager;
import python.reload.api.utils.math.TimerUtil;
import python.reload.api.utils.player.MoveUtil;
import python.reload.api.utils.other.SlownessManager;

import java.util.LinkedList;

@ModuleRegister(name = "Inventory Move", category = Category.MOVEMENT)
public class InventoryMoveModule extends Module {
    @Getter private static final InventoryMoveModule instance = new InventoryMoveModule();

    public final ModeSetting swapMode = new ModeSetting("Swap mode").value("Grim").values("Basic", "Grim", "Legit");

    private final LinkedList<Packet<?>> packet = new LinkedList<>();
    private final TimerUtil timerUtil = new TimerUtil();
    private boolean slowed = false;

    public InventoryMoveModule() {
        addSettings(swapMode);
    }

    public boolean isLegit() {
        return swapMode.is("Legit");
    }
    public boolean isGrim() {
        return swapMode.is("Grim");
    }
    public boolean isBasic() {
        return swapMode.is("Basic");
    }

    @Override
    public void onEvent() {
        EventListener closeScreenEvent = CloseScreenEvent.getInstance().subscribe(new Listener<>(event -> {
            closeScreenEvent();
        }));
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            updateEvent();
        }));
        EventListener packetEvent = PacketEvent.getInstance().subscribe(new Listener<>(event -> {
            packetEvent(event);
        }));

        addEvents(closeScreenEvent, updateEvent, packetEvent);
    }

    private void closeScreenEvent() {
        if (mc.currentScreen instanceof InventoryScreen && !packet.isEmpty() && isLegit()) {
            ThreadManager.run(() -> {
                slowed = true;
                timerUtil.reset();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                while (!packet.isEmpty()) {
                    sendPacket(packet.removeLast());
                }
                slowed = false;
            });
            CloseScreenEvent.getInstance().setCancel(true);
        }
    }
    
    private void updateEvent() {
        if (!timerUtil.finished(100) && isLegit()) {
            slowed = true;
            for (KeyBinding keyBinding : MoveUtil.getMovementKeys()) {
                keyBinding.setPressed(false);
            }
            slowed = false;
            return;
        }


        if (
                mc.currentScreen instanceof ChatScreen
                        || mc.currentScreen instanceof SignEditScreen
                        || mc.currentScreen instanceof AnvilScreen
                        || mc.currentScreen instanceof AbstractCommandBlockScreen
                        || mc.currentScreen instanceof StructureBlockScreen
                        || slowed
                        || SlownessManager.slowed
        ) {
            return;
        }

        MoveUtil.updateMovementKeys();
    }
    
    private void packetEvent(PacketEvent.PacketEventData event) {
        if (event.isSend() && isLegit()) {
            if (MoveUtil.isMoving() || mc.options.jumpKey.isPressed()) {
                Packet<?> pacl = event.packet();
                if (pacl instanceof ClickSlotC2SPacket
                        || pacl instanceof ButtonClickC2SPacket
                        || pacl instanceof CreativeInventoryActionC2SPacket
                        || pacl instanceof SlotChangedStateC2SPacket
                ) {
                    if (mc.currentScreen instanceof InventoryScreen) {
                        packet.add(pacl);
                        PacketEvent.getInstance().setCancel(true);
                    }
                }

                if (pacl instanceof CloseHandledScreenC2SPacket) {
                    packet.add(pacl);
                    PacketEvent.getInstance().setCancel(true);
                }
            }
        }
    }
}
