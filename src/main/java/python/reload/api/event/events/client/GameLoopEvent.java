package python.reload.api.event.events.client;

import lombok.Getter;
import python.reload.api.event.events.Event;

public class GameLoopEvent extends Event<GameLoopEvent> {
    @Getter private static final GameLoopEvent instance = new GameLoopEvent();
}
