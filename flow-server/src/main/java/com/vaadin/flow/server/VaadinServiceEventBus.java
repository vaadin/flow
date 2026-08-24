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
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.LoggerFactory;

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
 * @since 25.3
 */
public class VaadinServiceEventBus implements Serializable {

    private static final SerializableBiConsumer<EventObject, Exception> LOG_ERRORS = (
            event, error) -> LoggerFactory
                    .getLogger(VaadinServiceEventBus.class)
                    .error("Error in a listener for "
                            + event.getClass().getSimpleName(), error);

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

        // An event type keeps its list once it has one, so that a concurrent
        // removal can't detach the list from the map and leave this listener
        // registered in an orphaned copy of it
        List<SerializableConsumer<?>> registered = listeners.computeIfAbsent(
                eventType, type -> new CopyOnWriteArrayList<>());
        registered.add(listener);

        // The registration closes over the list of listeners for this one
        // event type rather than over the bus, so that holding on to it does
        // not keep the whole service reachable
        return Registration.once(() -> registered.remove(listener));
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
        List<SerializableConsumer<?>> registered = listeners.get(eventType);
        return registered != null && !registered.isEmpty();
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
     * An exception thrown by a listener is logged and the remaining listeners
     * are notified regardless, so that one misbehaving listener can neither
     * disrupt the caller nor hide the event from the other listeners. Use
     * {@link #fireEvent(EventObject, SerializableBiConsumer)} to handle those
     * exceptions differently.
     *
     * @param event
     *            the event to fire, not {@code null}
     */
    public void fireEvent(EventObject event) {
        fireEvent(event, LOG_ERRORS);
    }

    /**
     * Fires an event to the listeners registered for its exact type, in
     * registration order, handing a listener that threw to the given error
     * handler instead of logging it.
     * <p>
     * The listeners that have not been notified yet are notified next unless
     * the error handler itself throws, in which case that exception is
     * propagated to the caller and the remaining listeners are skipped.
     *
     * @param <E>
     *            the event type
     * @param event
     *            the event to fire, not {@code null}
     * @param errorHandler
     *            invoked with the event and the exception when a listener
     *            throws, not {@code null}
     */
    public <E extends EventObject> void fireEvent(E event,
            SerializableBiConsumer<? super E, Exception> errorHandler) {
        Objects.requireNonNull(event, "Event cannot be null");
        Objects.requireNonNull(errorHandler, "Error handler cannot be null");
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
     * @param event
     *            the event to fire, not {@code null}
     */
    public void fireEventInReverseOrder(EventObject event) {
        fireEventInReverseOrder(event, LOG_ERRORS);
    }

    /**
     * Fires an event to the listeners registered for its exact type in reverse
     * registration order, handing a listener that threw to the given error
     * handler instead of logging it.
     * <p>
     * The listeners that have not been notified yet are notified next unless
     * the error handler itself throws, in which case that exception is
     * propagated to the caller and the remaining listeners are skipped.
     *
     * @param <E>
     *            the event type
     * @param event
     *            the event to fire, not {@code null}
     * @param errorHandler
     *            invoked with the event and the exception when a listener
     *            throws, not {@code null}
     */
    @SuppressWarnings("unchecked")
    public <E extends EventObject> void fireEventInReverseOrder(E event,
            SerializableBiConsumer<? super E, Exception> errorHandler) {
        Objects.requireNonNull(event, "Event cannot be null");
        Objects.requireNonNull(errorHandler, "Error handler cannot be null");
        List<SerializableConsumer<E>> listeners = listenersFor(event);
        if (listeners.isEmpty()) {
            return;
        }
        // Taken as one snapshot, since reading the size and then iterating
        // down from it would be two reads of a list that another thread can be
        // removing listeners from
        Object[] registered = listeners.toArray();
        for (int i = registered.length - 1; i >= 0; i--) {
            notifyListener((SerializableConsumer<E>) registered[i], event,
                    errorHandler);
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
            SerializableBiConsumer<? super E, Exception> errorHandler) {
        try {
            listener.accept(event);
        } catch (Exception e) {
            errorHandler.accept(event, e);
        }
    }
}
