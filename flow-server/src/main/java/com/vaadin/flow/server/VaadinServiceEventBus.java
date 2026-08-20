/*
 * Copyright 2000-2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.shared.Registration;

/**
 * An event bus for {@link VaadinService}.
 * <p>
 * Anything that wants to notify listeners registered on a service can define an
 * event type and fire it through this bus, instead of the service having to
 * grow a listener collection and a {@code fireXyz} method for every new event
 * type:
 *
 * <pre>
 * service.getEventBus().addListener(MyEvent.class, event -&gt; doSomething());
 * service.getEventBus().fireEvent(new MyEvent(service));
 * </pre>
 * <p>
 * The API mirrors {@link com.vaadin.flow.component.ComponentEventBus}, but
 * unlike that one this bus is safe to use from several threads at once, which a
 * service-wide bus has to be: listeners can be added and removed while events
 * are being fired from other request threads.
 * <p>
 * Events are dispatched by their exact runtime type; a listener registered for
 * a supertype is not notified of subtype events.
 *
 * @see VaadinService#getEventBus()
 */
public class VaadinServiceEventBus implements Serializable {

    private final VaadinService service;

    private final Map<Class<? extends EventObject>, CopyOnWriteArrayList<SerializableConsumer<?>>> listeners = new ConcurrentHashMap<>();

    /**
     * Creates an event bus for the given service.
     *
     * @param service
     *            the service owning this event bus, not {@code null}
     */
    public VaadinServiceEventBus(VaadinService service) {
        this.service = Objects.requireNonNull(service);
    }

    /**
     * Gets the service this event bus belongs to.
     *
     * @return the service, not {@code null}
     */
    public VaadinService getService() {
        return service;
    }

    /**
     * Adds a listener for the given event type.
     * <p>
     * Listeners are notified in registration order. The same listener can be
     * added several times, in which case it is notified once per registration.
     *
     * @param <E>
     *            the event type
     * @param eventType
     *            the type of event to listen to, not {@code null}
     * @param listener
     *            the listener to call when an event of the given type is fired,
     *            not {@code null}
     * @return a handle that can be used for removing the listener
     */
    public <E extends EventObject> Registration addListener(Class<E> eventType,
            SerializableConsumer<E> listener) {
        Objects.requireNonNull(eventType, "Event type cannot be null");
        Objects.requireNonNull(listener, "Listener cannot be null");

        // The list is created and appended to inside the same atomic map
        // operation, so that a concurrent removal can't detach it from the map
        // in between and leave the listener registered in an orphaned list
        listeners.compute(eventType, (type, registered) -> {
            CopyOnWriteArrayList<SerializableConsumer<?>> updated = registered == null
                    ? new CopyOnWriteArrayList<>()
                    : registered;
            updated.add(listener);
            return updated;
        });

        return Registration.once(() -> removeListener(eventType, listener));
    }

    private void removeListener(Class<? extends EventObject> eventType,
            SerializableConsumer<?> listener) {
        // Drop the whole entry once the last listener is gone so that
        // hasListener stays cheap and accurate
        listeners.computeIfPresent(eventType, (type, registered) -> {
            registered.remove(listener);
            return registered.isEmpty() ? null : registered;
        });
    }

    /**
     * Checks whether at least one listener is registered for the given event
     * type.
     * <p>
     * Callers on hot code paths can use this to skip building an event that
     * nobody would receive.
     *
     * @param eventType
     *            the event type to check, not {@code null}
     * @return {@code true} if at least one listener is registered for the event
     *         type, {@code false} otherwise
     */
    public boolean hasListener(Class<? extends EventObject> eventType) {
        Objects.requireNonNull(eventType, "Event type cannot be null");
        return listeners.containsKey(eventType);
    }

    /**
     * Gets the listeners registered for the given event type, in registration
     * order.
     *
     * @param <E>
     *            the event type
     * @param eventType
     *            the event type, not {@code null}
     * @return an unmodifiable collection of listeners, empty if none are
     *         registered
     */
    @SuppressWarnings("unchecked")
    public <E extends EventObject> Collection<SerializableConsumer<E>> getListeners(
            Class<E> eventType) {
        Objects.requireNonNull(eventType, "Event type cannot be null");
        List<SerializableConsumer<?>> registered = listeners.get(eventType);
        if (registered == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(
                (List<SerializableConsumer<E>>) (List<?>) new ArrayList<>(
                        registered));
    }

    /**
     * Fires an event to the listeners registered for its exact type, in
     * registration order.
     * <p>
     * An exception thrown by a listener is propagated to the caller and the
     * remaining listeners are not notified. Use
     * {@link #fireEvent(EventObject, SerializableBiConsumer)} to keep notifying
     * the other listeners instead.
     *
     * @param event
     *            the event to fire, not {@code null}
     */
    public void fireEvent(EventObject event) {
        fireEvent(event, null);
    }

    /**
     * Fires an event to the listeners registered for its exact type, in
     * registration order, handing a listener that threw to the given error
     * handler.
     * <p>
     * The remaining listeners are notified even if one of them throws, so one
     * misbehaving listener cannot hide the event from the others.
     *
     * @param <E>
     *            the event type
     * @param event
     *            the event to fire, not {@code null}
     * @param errorHandler
     *            invoked with the event and the exception when a listener
     *            throws a {@link RuntimeException}, or {@code null} to let the
     *            exception propagate to the caller
     */
    public <E extends EventObject> void fireEvent(E event,
            SerializableBiConsumer<? super E, RuntimeException> errorHandler) {
        Objects.requireNonNull(event, "Event cannot be null");
        for (SerializableConsumer<E> listener : listenersFor(event)) {
            notifyListener(listener, event, errorHandler);
        }
    }

    /**
     * Fires an event to the listeners registered for its exact type in reverse
     * registration order.
     * <p>
     * This is meant for the "closing" half of a pair of events, so that
     * listeners nest: the listener registered first is notified last, in the
     * same way that it was notified first of the "opening" event.
     *
     * @param <E>
     *            the event type
     * @param event
     *            the event to fire, not {@code null}
     * @param errorHandler
     *            invoked with the event and the exception when a listener
     *            throws a {@link RuntimeException}, or {@code null} to let the
     *            exception propagate to the caller
     */
    public <E extends EventObject> void fireEventInReverseOrder(E event,
            SerializableBiConsumer<? super E, RuntimeException> errorHandler) {
        Objects.requireNonNull(event, "Event cannot be null");
        List<SerializableConsumer<E>> registered = listenersFor(event);
        ListIterator<SerializableConsumer<E>> iterator = registered
                .listIterator(registered.size());
        while (iterator.hasPrevious()) {
            notifyListener(iterator.previous(), event, errorHandler);
        }
    }

    @SuppressWarnings("unchecked")
    private <E extends EventObject> List<SerializableConsumer<E>> listenersFor(
            E event) {
        List<SerializableConsumer<?>> registered = listeners
                .get(event.getClass());
        return registered == null ? Collections.emptyList()
                : (List<SerializableConsumer<E>>) (List<?>) registered;
    }

    private <E extends EventObject> void notifyListener(
            SerializableConsumer<E> listener, E event,
            SerializableBiConsumer<? super E, RuntimeException> errorHandler) {
        if (errorHandler == null) {
            listener.accept(event);
            return;
        }
        try {
            listener.accept(event);
        } catch (RuntimeException e) {
            errorHandler.accept(event, e);
        }
    }
}
