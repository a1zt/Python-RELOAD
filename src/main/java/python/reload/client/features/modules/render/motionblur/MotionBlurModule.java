package python.reload.client.features.modules.render.motionblur;

import lombok.Getter;
import python.reload.api.module.Category;
import python.reload.api.module.Module;
import python.reload.api.module.ModuleRegister;
import python.reload.api.module.setting.BooleanSetting;
import python.reload.api.module.setting.SliderSetting;

@ModuleRegister(name = "Motion Blur", category = Category.RENDER)
public class MotionBlurModule extends Module {
    @Getter private static final MotionBlurModule instance = new MotionBlurModule();
    public final ShaderMotionBlur shader;

    @Getter public final SliderSetting strength = new SliderSetting("Strength").value(-0.8f)
            .range(-2f, 2f).step(0.1f)
            .onAction(() -> setMotionBlurStrength(getStrength().getValue()));
    public final BooleanSetting useRRC = new BooleanSetting("Use refresh rate scaling").value(true);


    public MotionBlurModule() {
        shader = new ShaderMotionBlur(this);
        shader.registerShaderCallbacks();
        addSettings(strength, useRRC);
    }

    @Override
    public void onEvent() {

    }

    private void setMotionBlurStrength(float strength) {
        shader.updateBlurStrength(strength);
    }

    public enum BlurAlgorithm {BACKWARDS, CENTERED}
    public static BlurAlgorithm blurAlgorithm = BlurAlgorithm.CENTERED;
}
