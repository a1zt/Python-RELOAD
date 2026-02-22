package python.reload.api.utils.rotation.rotations;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import python.reload.api.utils.rotation.manager.Rotation;
import python.reload.api.utils.rotation.manager.RotationMode;

public class InstantRotation extends RotationMode {
    public InstantRotation() {
        super("Instant");
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3d vec3d, Entity entity) {
        return targetRotation;
    }
}
