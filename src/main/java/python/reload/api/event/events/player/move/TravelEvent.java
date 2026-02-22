package python.reload.api.event.events.player.move;

import lombok.Getter;
import python.reload.api.event.events.Event;

public class TravelEvent extends Event<TravelEvent> {
    @Getter private static final TravelEvent instance = new TravelEvent();
}
