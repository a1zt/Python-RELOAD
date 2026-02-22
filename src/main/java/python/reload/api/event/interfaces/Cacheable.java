package python.reload.api.event.interfaces;

import python.reload.api.event.Listener;

public interface Cacheable<T> {
    Listener<T>[] getCache();
}