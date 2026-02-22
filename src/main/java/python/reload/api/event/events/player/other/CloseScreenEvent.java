package python.reload.api.event.events.player.other;

import lombok.Getter;
import python.reload.api.event.events.Event;

public class CloseScreenEvent extends Event<CloseScreenEvent> {
    @Getter private static final CloseScreenEvent instance = new CloseScreenEvent();
}
