package edu.univ.erp.service;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Thread-safe, simple publish/subscribe event bus.
 * 
 * Used to notify UI panels (student overview, grades page,
 * instructor panels, transcript, etc.) whenever:
 *  - enrollment changes
 *  - grades change
 *  - instructor saves new scores
 *
 * Any panel that wants auto-refresh should register a listener:
 *
 * RegistrationEventBus.get().register(() -> reloadData());
 */
public class RegistrationEventBus {

    // Listener interface
    public interface Listener {
        void onRegistrationChanged();
    }

    // SINGLETON instance
    private static final RegistrationEventBus INSTANCE = new RegistrationEventBus();

    // Thread-safe listener set
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();

    // private constructor
    private RegistrationEventBus() {}

    // global access
    public static RegistrationEventBus get() {
        return INSTANCE;
    }

    public void addListener(Object o) {
    if (o instanceof Listener) {
        listeners.add((Listener) o);
    } else {
        System.err.println("Tried to add non-listener: " + o.getClass());
    }
}


    // register listener
    public void register(Listener l) {
        if (l != null) listeners.add(l);
    }

    // unregister listener
    public void unregister(Listener l) {
        if (l != null) listeners.remove(l);
    }

    // notify all listeners (UI auto-reloads)
    public void notifyChange() {
        for (Listener l : listeners) {
            try {
                l.onRegistrationChanged();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}
