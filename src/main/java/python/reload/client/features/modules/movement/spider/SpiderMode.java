package python.reload.client.features.modules.movement.spider;

import python.reload.api.event.events.player.move.MotionEvent;
import python.reload.api.system.backend.Choice;

public abstract class SpiderMode extends Choice {
    public void onUpdate() {}
    public void onMotion(MotionEvent.MotionEventData event) {}

    public boolean hozColl() {
        return mc.player.horizontalCollision;
    }
}
