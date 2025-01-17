/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.dom;

import java.util.EventObject;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.vaadin.flow.internal.NodeOwner;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.shared.JsonConstants;

import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Server-side representation of a DOM event fired in the browser.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class DomEvent extends EventObject {

    private final JsonObject eventData;

    private final String eventType;

    private final DebouncePhase phase;

    private final Element eventTarget;

    /**
     * Creates a new DOM event.
     *
     * @param source
     *            the element on which the listener has been attached, not
     *            <code>null</code>
     * @param eventType
     *            the type of the event, not <code>null</code>
     * @param eventData
     *            additional data related to the event, not <code>null</code>
     *
     * @see Element#addEventListener(String, DomEventListener)
     * @see DomEventListener
     */
    public DomEvent(Element source, String eventType, JsonObject eventData) {
        super(source);
        assert source != null;
        assert eventType != null;
        assert eventData != null;

        this.eventType = eventType;
        this.eventData = eventData;

        phase = extractPhase(eventData);
        eventTarget = extractEventTarget(eventData, source);
    }

    private static DebouncePhase extractPhase(JsonObject eventData) {
        JsonValue jsonValue = eventData.get(JsonConstants.EVENT_DATA_PHASE);
        if (jsonValue == null) {
            return DebouncePhase.LEADING;
        } else {
            return DebouncePhase.forIdentifier(jsonValue.asString());
        }
    }

    private static Element extractEventTarget(JsonObject eventData,
            Element currentTarget) {
        return extractElement(eventData, currentTarget,
                JsonConstants.MAP_STATE_NODE_EVENT_DATA, false);
    }

    static Element extractElement(JsonObject eventData, Element source,
            String key, boolean lookUnderUI) {
        assert key.startsWith(JsonConstants.MAP_STATE_NODE_EVENT_DATA);
        if (!eventData.hasKey(key)) {
            return null;
        }
        final JsonValue reportedStateNodeId = eventData.get(key);
        if (reportedStateNodeId == null) {
            return null;
        }
        int id = (int) reportedStateNodeId.asNumber();
        if (id == -1) {
            return null;
        }
        AtomicReference<Element> matchingNode = new AtomicReference<>();
        final Consumer<StateNode> visitor = node -> {
            if (node.getId() == id) {
                matchingNode.set(Element.get(node));
            }
        };
        // first look under event source
        source.getNode().visitNodeTree(visitor);
        if (lookUnderUI && matchingNode.get() == null) {
            // widen search to look under UI too
            final NodeOwner owner = source.getNode().getOwner();
            if (owner instanceof StateTree) {
                ((StateTree) owner).getRootNode().visitNodeTree(visitor);
            }
        }
        final Element mappedElementOrNull = matchingNode.get();
        // prevent spoofing invisible elements by sending bad state node ids
        if (mappedElementOrNull != null && !mappedElementOrNull.isVisible()) {
            return null;
        }
        return mappedElementOrNull;
    }

    /**
     * Returns the element on which the listener has been attached.
     *
     * @return The element on which the listener has been attached.
     *
     * @see Element#addEventListener(String, DomEventListener)
     * @see #getEventTarget() for event target element
     */
    @Override
    public Element getSource() {
        return (Element) super.getSource();
    }

    /**
     * Gets the type of the event.
     *
     * @return the type of the event
     */
    public String getType() {
        return eventType;
    }

    /**
     * Gets additional data related to the event. An empty JSON object is
     * returned if no event data is available.
     *
     * @see DomListenerRegistration#addEventData(String)
     *
     * @return a JSON object containing event data, never <code>null</code>
     */
    public JsonObject getEventData() {
        return eventData;
    }

    /**
     * Gets the debounce phase for which this event is fired. This is used
     * internally to only deliver the event to the appropriate listener in cases
     * where there are multiple listeners for the same event with different
     * debounce settings.
     *
     * @return the debounce phase
     */
    public DebouncePhase getPhase() {
        return phase;
    }

    /**
     * Gets the closest {@link Element} that corresponds to the
     * {@code event.target} for the DOM event. This is always inside the child
     * hierarchy of the element returned by {@link #getSource()}.
     * <p>
     * To get this reported, you need to call
     * {@link DomListenerRegistration#mapEventTargetElement()} or an empty
     * optional is always returned.
     * <p>
     * The returned element is the same as {@link #getSource()} <em>only if</em>
     * the event originated from that element on the browser (and not from its
     * child).
     *
     * @return the element that corresponds to {@code event.target} or an empty
     *         optional
     * @since 9.0
     */
    public Optional<Element> getEventTarget() {
        return Optional.ofNullable(eventTarget);
    }

    /**
     * Gets the closest {@link Element} corresponding to the given event data
     * expression. <em>NOTE:</em> this only works if you have added the
     * expression using
     * {@link DomListenerRegistration#addEventDataElement(String)}.
     * <p>
     * If the evaluated JS expression returned an element that is not created or
     * controlled by the server side, the closest parent element that is
     * controlled is returned instead. Invisible elements are not reported.
     * <p>
     * In case you want the {@code event.target} element, use
     * {@link #getEventTarget()} instead.
     *
     * @param eventDataExpression
     *            the expression that was executed on the client to retrieve the
     *            element, not <code>null</code>
     * @return the element that corresponds to the given expression or an empty
     *         optional
     * @since 9.0
     */
    public Optional<Element> getEventDataElement(String eventDataExpression) {
        Objects.requireNonNull(eventDataExpression);
        if (Objects.equals(eventDataExpression, "event.target")) {
            return getEventTarget();
        } else {
            return Optional.ofNullable(extractElement(eventData, getSource(),
                    JsonConstants.MAP_STATE_NODE_EVENT_DATA
                            + eventDataExpression,
                    true));
        }
    }
}
