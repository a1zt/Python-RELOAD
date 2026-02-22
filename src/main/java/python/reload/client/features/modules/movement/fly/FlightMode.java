package python.reload.client.features.modules.movement.fly;

import python.reload.api.event.events.player.move.MotionEvent;
import python.reload.api.system.backend.Choice;

public abstract class FlightMode extends Choice {


    // events
    public void onUpdate() {}
    public void onMotion(MotionEvent.MotionEventData event) {}

    // module methods
    public void onEnable() {}
    public void onDisable() {}
    public void toggle() {}
}
