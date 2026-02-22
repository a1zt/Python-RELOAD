package python.reload.client.features.modules.movement.noslow;

import python.reload.api.system.backend.Choice;

public abstract class NoSlowMode extends Choice {
    public abstract void onUpdate();
    public abstract void onTick();
    public abstract boolean slowingCancel();
}
