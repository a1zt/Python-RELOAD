package python.reload.client.features.modules.movement.nitrofirework;

import python.reload.api.system.backend.Choice;
import python.reload.api.system.backend.Pair;

public abstract class NitroFireworkMode extends Choice {
    public abstract Pair<Float, Float> velocityValues();
}
