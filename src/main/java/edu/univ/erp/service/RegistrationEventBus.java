package edu.univ.erp.service;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Small thread-safe notifier for registration changes.
 */
public class RegistrationEventBus {
    public interface Listener {
        void onRegistrationChanged();
    }

    private static final RegistrationEventBus INSTANCE = new RegistrationEventBus();
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();

    private RegistrationEventBus() {}

    public static RegistrationEventBus get() { return INSTANCE; }

    public void register(Listener l) { if (l != null) listeners.add(l); }
    public void unregister(Listener l) { if (l != null) listeners.remove(l); }

    public void notifyChange() {
        for (Listener l : listeners) {
            try { l.onRegistrationChanged(); }
            catch (Throwable t) { t.printStackTrace(); }
        }
    }
}



