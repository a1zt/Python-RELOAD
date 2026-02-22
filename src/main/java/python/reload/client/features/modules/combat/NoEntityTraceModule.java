package python.reload.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.entity.Entity;
import python.reload.api.module.Category;
import python.reload.api.module.Module;
import python.reload.api.module.ModuleRegister;
import python.reload.api.system.configs.FriendManager;

@ModuleRegister(name = "No Entity Trace", category = Category.COMBAT)
public class NoEntityTraceModule extends Module {
    @Getter private static final NoEntityTraceModule instance = new NoEntityTraceModule();

    @Override
    public void onEvent() {

    }

    public boolean shouldCancelResult(Entity entity) {
        boolean noFriendHurt = NoFriendHurtModule.getInstance().isEnabled() &&
                entity != null && FriendManager.getInstance().contains(entity.getName().getString());
        return noFriendHurt || isEnabled();
    }
}
