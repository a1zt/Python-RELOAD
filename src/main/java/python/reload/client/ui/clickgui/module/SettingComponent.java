package python.reload.client.ui.clickgui.module;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import python.reload.api.module.setting.Setting;
import python.reload.api.utils.animation.AnimationUtil;
import python.reload.client.ui.UIComponent;

@Getter
@RequiredArgsConstructor
public abstract class SettingComponent extends UIComponent {
    private final Setting<?> setting;
    private final AnimationUtil visibleAnimation = new AnimationUtil();

    public void updateHeight(float value) {
        setHeight(scaled(value));
    }
}
