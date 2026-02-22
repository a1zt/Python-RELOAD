package python.reload.client.features.modules.combat;

import lombok.Getter;
import python.reload.api.module.Category;
import python.reload.api.module.Module;
import python.reload.api.module.ModuleRegister;

@ModuleRegister(name = "No Friend Hurt", category = Category.COMBAT)
public class NoFriendHurtModule extends Module {
    @Getter private static final NoFriendHurtModule instance = new NoFriendHurtModule();

    @Override
    public void onEvent() {

    }
}
