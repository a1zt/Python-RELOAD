package python.reload.client.features.modules.movement;

import lombok.Getter;
import python.reload.api.event.EventListener;
import python.reload.api.event.Listener;
import python.reload.api.event.events.player.other.UpdateEvent;
import python.reload.api.module.Category;
import python.reload.api.module.Module;
import python.reload.api.module.ModuleRegister;
import python.reload.api.utils.player.MoveUtil;
import python.reload.api.utils.player.PlayerUtil;

@ModuleRegister(name = "No Web", category = Category.MOVEMENT)
public class NoWebModule extends Module {
    @Getter private static final NoWebModule instance = new NoWebModule();

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (PlayerUtil.isInWeb()) {
                mc.player.setVelocity(0, 0, 0);

                double verticalSpeed = 0.995;
                double horizantalSpeed = 0.19175;

                if (mc.options.jumpKey.isPressed()) {
                    mc.player.getVelocity().y = verticalSpeed;
                } else if (mc.options.sneakKey.isPressed()) {
                    mc.player.getVelocity().y = -verticalSpeed;
                }

                MoveUtil.setSpeed(horizantalSpeed);
            }
        }));

        addEvents(updateEvent);
    }
}
