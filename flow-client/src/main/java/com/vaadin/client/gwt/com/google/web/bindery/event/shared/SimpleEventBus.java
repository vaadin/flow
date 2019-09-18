/*
 * Copyright 2011 Google Inc.
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
package com.vaadin.client.gwt.com.google.web.bindery.event.shared;

import com.google.web.bindery.event.shared.Event;
import com.google.web.bindery.event.shared.Event.Type;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.HandlerRegistration;

import com.vaadin.client.flow.collection.JsArray;
import com.vaadin.client.flow.collection.JsCollections;
import com.vaadin.client.flow.collection.JsMap;

/**
 * Basic implementation of {@link EventBus}.
 *
 * Copied from the GWT project to use JsArray and JsMap instead of ArrayList and
 * HashMap
 *
 * @since 1.0
 */
public class SimpleEventBus extends EventBus {
    @FunctionalInterface
    private interface Command {
        void execute();
    }

    private int firingDepth = 0;

    /**
     * Add and remove operations received during dispatch.
     */
    private JsArray<Command> deferredDeltas;

    /**
     * Map of event type to map of event source to list of their handlers.
     */
    private final JsMap<Event.Type<?>, JsMap<Object, JsArray<?>>> map = JsCollections
            .map();

    /**
     * Create an instance of the event bus.
     */
    public SimpleEventBus() {
        // Intentionally left empty
    }

    @Override
    public <H> HandlerRegistration addHandler(Type<H> type, H handler) {
        return doAdd(type, null, handler);
    }

    @Override
    public <H> HandlerRegistration addHandlerToSource(final Event.Type<H> type,
            final Object source, final H handler) {
        if (source == null) {
            throw new NullPointerException(
                    "Cannot add a handler with a null source");
        }

        return doAdd(type, source, handler);
    }

    @Override
    public void fireEvent(Event<?> event) {
        doFire(event, null);
    }

    @Override
    public void fireEventFromSource(Event<?> event, Object source) {
        if (source == null) {
            throw new NullPointerException("Cannot fire from a null source");
        }
        doFire(event, source);
    }

    /**
     * Not documented in GWT, required by legacy features in GWT's old
     * HandlerManager.
     *
     * @param type
     *            the type
     * @param source
     *            the source
     * @param handler
     *            the handler
     * @param <H>
     *            the handler type
     * @deprecated required by legacy features in GWT's old HandlerManager
     */
    @Deprecated
    protected <H> void doRemove(Event.Type<H> type, Object source, H handler) {
        if (firingDepth > 0) {
            enqueueRemove(type, source, handler);
        } else {
            doRemoveNow(type, source, handler);
        }
    }

    /**
     * Not documented in GWT, required by legacy features in GWT's old
     * HandlerManager.
     * 
     * @param type
     *            the type
     * @param index
     *            the index
     * @param <H>
     *            the handler type
     * @return the handler
     *
     * @deprecated required by legacy features in GWT's old HandlerManager
     */
    @Deprecated
    protected <H> H getHandler(Event.Type<H> type, int index) {
        assert index < getHandlerCount(type) : "handlers for " + type.getClass()
                + " have size: " + getHandlerCount(type)
                + " so do not have a handler at index: " + index;

        JsArray<H> l = getHandlerList(type, null);
        return l.get(index);
    }

    /**
     * Not documented in GWT, required by legacy features in GWT's old
     * HandlerManager.
     *
     * @param eventKey
     *            the event type
     * @return the handlers count
     *
     * @deprecated required by legacy features in GWT's old HandlerManager
     */
    @Deprecated
    protected int getHandlerCount(Event.Type<?> eventKey) {
        return getHandlerList(eventKey, null).length();
    }

    /**
     * Not documented in GWT, required by legacy features in GWT's old
     * HandlerManager.
     *
     * @param eventKey
     *            the event type
     * @return {@code true} if the event is handled, {@code false} otherwise
     * @deprecated required by legacy features in GWT's old HandlerManager
     */
    @Deprecated
    protected boolean isEventHandled(Event.Type<?> eventKey) {
        return map.has(eventKey);
    }

    private void defer(Command command) {
        if (deferredDeltas == null) {
            deferredDeltas = JsCollections.array();
        }
        deferredDeltas.push(command);
    }

    private <H> HandlerRegistration doAdd(final Event.Type<H> type,
            final Object source, final H handler) {
        if (type == null) {
            throw new NullPointerException(
                    "Cannot add a handler with a null type");
        }
        if (handler == null) {
            throw new NullPointerException("Cannot add a null handler");
        }

        if (firingDepth > 0) {
            enqueueAdd(type, source, handler);
        } else {
            doAddNow(type, source, handler);
        }

        return () -> doRemove(type, source, handler);
    }

    @SuppressWarnings("unchecked")
    private <H> void doAddNow(Event.Type<H> type, Object source, H handler) {
        JsArray<H> l = ensureHandlerList(type, source);
        l.push(handler);
    }

    private <H> void doFire(Event<H> event, Object source) {
        if (event == null) {
            throw new NullPointerException("Cannot fire null event");
        }
        try {
            firingDepth++;

            if (source != null) {
                setSourceOfEvent(event, source);
            }

            JsArray<H> handlers = getDispatchList(event.getAssociatedType(),
                    source);
            JsArray<Throwable> causes = null;

            for (int i = 0; i < handlers.length(); i++) {
                H handler = handlers.get(i);

                try {
                    dispatchEvent(event, handler);
                } catch (Exception e) {
                    if (causes == null) {
                        causes = JsCollections.array();
                    }
                    causes.set(causes.length(), e);
                }
            }

            if (causes != null) {
                throw new RuntimeException(causes.get(0));
            }
        } finally {
            firingDepth--;
            if (firingDepth == 0) {
                handleQueuedAddsAndRemoves();
            }
        }
    }

    private <H> void doRemoveNow(Event.Type<H> type, Object source, H handler) {
        JsArray<H> l = getHandlerList(type, source);

        boolean removed = l.remove(handler);

        if (removed && l.isEmpty()) {
            prune(type, source);
        }
    }

    private <H> void enqueueAdd(final Event.Type<H> type, final Object source,
            final H handler) {
        defer(() -> doAddNow(type, source, handler));
    }

    private <H> void enqueueRemove(final Event.Type<H> type,
            final Object source, final H handler) {
        defer(() -> doRemoveNow(type, source, handler));
    }

    private <H> JsArray<H> ensureHandlerList(Event.Type<H> type,
            Object source) {
        JsMap<Object, JsArray<?>> sourceMap = map.get(type);
        if (sourceMap == null) {
            sourceMap = JsCollections.map();
            map.set(type, sourceMap);
        }

        // safe, we control the puts.
        @SuppressWarnings("unchecked")
        JsArray<H> handlers = (JsArray<H>) sourceMap.get(source);
        if (handlers == null) {
            handlers = JsCollections.array();
            sourceMap.set(source, handlers);
        }

        return handlers;
    }

    private <H> JsArray<H> getDispatchList(Event.Type<H> type, Object source) {
        JsArray<H> directHandlers = getHandlerList(type, source);
        if (source == null) {
            return directHandlers;
        }

        JsArray<H> globalHandlers = getHandlerList(type, null);

        JsArray<H> rtn = JsCollections.array();
        rtn.pushArray(directHandlers);
        rtn.pushArray(globalHandlers);
        return rtn;
    }

    private <H> JsArray<H> getHandlerList(Event.Type<H> type, Object source) {
        JsMap<Object, JsArray<?>> sourceMap = map.get(type);
        if (sourceMap == null) {
            return JsCollections.array();
        }

        // safe, we control the puts.
        @SuppressWarnings("unchecked")
        JsArray<H> handlers = (JsArray<H>) sourceMap.get(source);
        if (handlers == null) {
            return JsCollections.array();
        }

        return handlers;
    }

    private void handleQueuedAddsAndRemoves() {
        if (deferredDeltas != null) {
            try {
                for (int i = 0; i < deferredDeltas.length(); i++) {
                    Command c = deferredDeltas.get(i);
                    c.execute();
                }
            } finally {
                deferredDeltas = null;
            }
        }
    }

    private void prune(Event.Type<?> type, Object source) {
        JsMap<Object, JsArray<?>> sourceMap = map.get(type);

        JsArray<?> pruned = sourceMap.get(source);
        sourceMap.delete(source);

        assert pruned != null : "Can't prune what wasn't there";
        assert pruned.isEmpty() : "Pruned unempty list!";

        if (sourceMap.isEmpty()) {
            map.delete(type);
        }
    }
}
