package python.reload.client.ui.widget;

import lombok.Getter;
import lombok.Setter;
import python.reload.api.event.events.render.Render2DEvent;
import python.reload.api.system.draggable.Draggable;
import python.reload.api.system.draggable.DraggableManager;
import python.reload.api.system.interfaces.IRenderer;
import python.reload.api.system.interfaces.QuickImports;
import python.reload.api.utils.animation.Easing;
import python.reload.api.utils.render.fonts.Font;
import python.reload.api.utils.render.fonts.Fonts;
import python.reload.client.features.modules.render.InterfaceModule;
import python.reload.client.services.RenderService;

@Getter
@Setter
public abstract class Widget implements QuickImports, IRenderer {
    protected Widget(float x, float y) {
        this.draggable = create(x, y, getName());
    }

    private final Easing easing = Easing.SINE_OUT;
    private final long duration = 100;

    public abstract String getName();
    private final Draggable draggable;
    private boolean enabled;

    private Draggable create(float x, float y, String name) {
        return DraggableManager.getInstance().create(InterfaceModule.getInstance(), name, x, y);
    }

    public void render(Render2DEvent.Render2DEventData event) {
        render(event.matrixStack());
    }

    public float scaled(float value) {
        return RenderService.getInstance().scaled(value);
    }

    public float getScale() { return RenderService.getInstance().getScale(); }
    public float getGap() { return scaled(3f); }
    public Font getMediumFont() { return Fonts.PS_MEDIUM; }
    public Font getSemiBoldFont() { return Fonts.PS_BOLD; }
}
