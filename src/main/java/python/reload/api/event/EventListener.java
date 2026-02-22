package python.reload.api.event;

public record EventListener(Runnable action) {
    public void unsubscribe() {
        action.run();
    }
}
