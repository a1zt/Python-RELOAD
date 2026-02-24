package python.reload.client.features.modules.render;

import lombok.Getter;
import python.reload.api.module.Category;
import python.reload.api.module.Module;
import python.reload.api.module.ModuleRegister;
import python.reload.api.module.setting.SliderSetting;

@ModuleRegister(name = "AspectRatio", category = Category.RENDER)
    public class AspectRatioModule extends Module {
@Getter
    private static final AspectRatioModule instance = new AspectRatioModule();
        public final SliderSetting ratio = new SliderSetting("Ratio").value(1.33f).range(0.5f, 3.0f).step(0.01f);

    public AspectRatioModule() {
        addSettings(ratio);
    }   
@Override
    public void onEvent() {
    }
}