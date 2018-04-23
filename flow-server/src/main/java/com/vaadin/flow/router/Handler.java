package com.vaadin.flow.router;

import java.util.*;

/**
 * Marker-interface for all Handlers, which are classes that
 * react to a certain type of {@see EventObject}.
 *
 * @param <T> the EventObject to handle
 */
public interface Handler<T extends EventObject> {
    void handle(T event);
}
