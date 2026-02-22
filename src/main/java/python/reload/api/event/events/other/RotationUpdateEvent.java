package python.reload.api.event.events.other;

import lombok.Getter;
import python.reload.api.event.events.Event;

public class RotationUpdateEvent extends Event<RotationUpdateEvent> {
    @Getter private static final RotationUpdateEvent instance = new RotationUpdateEvent();
}
