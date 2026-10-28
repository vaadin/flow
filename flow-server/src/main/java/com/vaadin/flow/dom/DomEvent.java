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
package com.vaadin.flow.dom;

import java.util.Comparator;
import java.util.EventObject;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.internal.JacksonCodec;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.NodeOwner;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.shared.JsonConstants;

/**
 * Server-side representation of a DOM event fired in the browser.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class DomEvent extends EventObject {

    private final JsonNode eventData;

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
     * @since 25.0
     */
    public DomEvent(Element source, String eventType, JsonNode eventData) {
        super(source);
        assert source != null;
        assert eventType != null;
        assert eventData != null;

        this.eventType = eventType;
        this.eventData = eventData;

        phase = extractPhase(eventData);
        eventTarget = extractEventTarget(eventData, source);
    }

    private static DebouncePhase extractPhase(JsonNode eventData) {
        JsonNode jsonValue = eventData.get(JsonConstants.EVENT_DATA_PHASE);
        if (jsonValue == null) {
            return DebouncePhase.LEADING;
        } else {
            return DebouncePhase.forIdentifier(jsonValue.asString());
        }
    }

    private static Element extractEventTarget(JsonNode eventData,
            Element currentTarget) {
        return extractElement(eventData, currentTarget,
                JsonConstants.MAP_STATE_NODE_EVENT_DATA, false);
    }

    static Element extractElement(JsonNode eventData, Element source,
            String key, boolean lookUnderUI) {
        assert key.startsWith(JsonConstants.MAP_STATE_NODE_EVENT_DATA);
        if (!eventData.has(key)) {
            return null;
        }
        final JsonNode reportedStateNodeId = eventData.get(key);
        if (reportedStateNodeId == null) {
            return null;
        }
        int id = reportedStateNodeId.intValue();
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
        // — use the state-node check that also walks ancestors so an
        // element whose own flag is true but whose ancestor was hidden is
        // also rejected.
        if (mappedElementOrNull != null
                && !mappedElementOrNull.getNode().isVisible()) {
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
    public JsonNode getEventData() {
        return eventData;
    }

    /**
     * Gets the event data deserialized as the given type. This method supports
     * arbitrary bean types through Jackson deserialization.
     * <p>
     * Example usage:
     *
     * <pre>
     * MyDto dto = domEvent.getEventData(MyDto.class);
     * </pre>
     * <p>
     * Event data keys that are dotted property paths, such as
     * {@code event.clientX}, are also expanded into nested objects, so that
     * data registered with {@link DomListenerRegistration#addEventData(Class)}
     * can be read back using the same type. The original dotted keys are kept
     * as well, so a {@code Map} target sees both the dotted keys and the
     * expanded top-level entries.
     *
     * @param <T>
     *            the type to deserialize to
     * @param type
     *            the class to deserialize the event data to, not
     *            <code>null</code>
     * @return the event data deserialized as the given type
     * @see DomListenerRegistration#addEventData(String)
     * @since 25.0
     */
    public <T> T getEventData(Class<T> type) {
        return JacksonCodec.decodeAs(getExpandedEventData(), type);
    }

    /**
     * Gets the event data deserialized as the type specified by the
     * {@link TypeReference}. This method supports generic types such as
     * {@code List<MyBean>} and {@code Map<String, MyBean>} through Jackson's
     * TypeReference mechanism.
     * <p>
     * Example usage:
     *
     * <pre>
     * TypeReference&lt;List&lt;MyDto&gt;&gt; typeRef = new TypeReference&lt;List&lt;MyDto&gt;&gt;() {
     * };
     * List&lt;MyDto&gt; dtos = domEvent.getEventData(typeRef);
     * </pre>
     * <p>
     * Event data keys that are dotted property paths, such as
     * {@code event.clientX}, are also expanded into nested objects. The
     * original dotted keys are kept as well, so a {@code Map} target sees both
     * the dotted keys and the expanded top-level entries, e.g. {@code event}.
     *
     * @param <T>
     *            the type to deserialize to
     * @param typeReference
     *            the type reference describing the target type, not
     *            <code>null</code>
     * @return the event data deserialized as the given type
     * @see DomListenerRegistration#addEventData(String)
     * @since 25.0
     */
    public <T> T getEventData(TypeReference<T> typeReference) {
        return JacksonCodec.decodeAs(getExpandedEventData(), typeReference);
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

    /**
     * Gets the {@code event.detail} property from the event, deserialized as
     * the given type. This method supports arbitrary bean types and Java
     * records through Jackson deserialization.
     * <p>
     * The {@code event.detail} property must have been included in the event
     * data using {@link DomListenerRegistration#addEventDetail()}.
     * <p>
     * Example usage:
     *
     * <pre>
     * record RgbColor(int r, int g, int b) {
     * }
     *
     * element.addEventListener("color-change", e -&gt; {
     *     RgbColor color = e.getEventDetail(RgbColor.class);
     *     System.out.println("R: " + color.r() + ", G: " + color.g() + ", B: "
     *             + color.b());
     * }).addEventDetail();
     * </pre>
     *
     * @param <T>
     *            the type to deserialize to
     * @param type
     *            the class to deserialize the event detail to, not
     *            <code>null</code>
     * @return the event detail deserialized as the given type, or
     *         <code>null</code> if event detail is not present or is null
     * @see DomListenerRegistration#addEventDetail()
     * @since 25.0
     */
    public <T> T getEventDetail(Class<T> type) {
        JsonNode detailNode = getDetailNode();
        if (detailNode == null) {
            return null;
        }
        return JacksonCodec.decodeAs(detailNode, type);
    }

    /**
     * Gets the {@code event.detail} property from the event, deserialized as
     * the type specified by the {@link TypeReference}. This method supports
     * generic types such as {@code List<MyBean>} and
     * {@code Map<String, MyBean>} through Jackson's TypeReference mechanism.
     * <p>
     * The {@code event.detail} property must have been included in the event
     * data using {@link DomListenerRegistration#addEventDetail()}.
     * <p>
     * Example usage:
     *
     * <pre>
     * element.addEventListener("list-change", e -&gt; {
     *     List&lt;String&gt; items = e
     *             .getEventDetail(new TypeReference&lt;List&lt;String&gt;&gt;() {
     *             });
     *     System.out.println("Items: " + items);
     * }).addEventDetail();
     * </pre>
     *
     * @param <T>
     *            the type to deserialize to
     * @param typeReference
     *            the type reference describing the target type, not
     *            <code>null</code>
     * @return the event detail deserialized as the given type, or
     *         <code>null</code> if event detail is not present or is null
     * @see DomListenerRegistration#addEventDetail()
     * @since 25.0
     */
    public <T> T getEventDetail(TypeReference<T> typeReference) {
        JsonNode detailNode = getDetailNode();
        if (detailNode == null) {
            return null;
        }
        return JacksonCodec.decodeAs(detailNode, typeReference);
    }

    /**
     * Gets or reconstructs the event.detail node from event data. Handles two
     * cases:
     * <ul>
     * <li>Case 1: event.detail sent as a complete object
     * <li>Case 2: event.detail properties sent individually (e.g.,
     * event.detail.r, event.detail.g, event.detail.start.x)
     * </ul>
     *
     * @return the detail node, or null if no detail data is present
     */
    private JsonNode getDetailNode() {
        JsonNode detailNode = getExpandedEventData().path("event")
                .path("detail");
        if (detailNode.isMissingNode() || detailNode.isNull()) {
            return null;
        }
        return detailNode;
    }

    /**
     * Gets a copy of the event data where keys that are dotted property paths,
     * e.g. {@code event.detail.r}, are additionally expanded into nested
     * objects, e.g. <code>{"event": {"detail": {"r": ...}}}</code>. The
     * original keys are retained. Shorter paths are expanded first so that a
     * complete object sent for e.g. {@code event.detail} is merged with any
     * individually sent properties. A path is not expanded if it would replace
     * an existing non-object value.
     *
     * @return the expanded event data
     */
    private JsonNode getExpandedEventData() {
        if (!(eventData instanceof ObjectNode)) {
            return eventData;
        }
        ObjectNode expanded = (ObjectNode) eventData.deepCopy();
        JacksonUtils.getKeys(eventData).stream()
                .filter(DomEvent::isPropertyPath)
                .sorted(Comparator.comparingLong(
                        key -> key.chars().filter(c -> c == '.').count()))
                .forEach(key -> setNestedIfAbsent(expanded, key.split("\\."),
                        expanded.get(key)));
        return expanded;
    }

    /**
     * Checks whether the given event data key is a plain dotted property path,
     * such as {@code event.clientX}, consisting of at least two identifier
     * segments.
     */
    private static boolean isPropertyPath(String key) {
        String[] segments = key.split("\\.", -1);
        if (segments.length < 2) {
            return false;
        }
        for (String segment : segments) {
            if (segment.isEmpty()
                    || !Character.isJavaIdentifierStart(segment.charAt(0))
                    || !segment.chars().skip(1)
                            .allMatch(Character::isJavaIdentifierPart)) {
                return false;
            }
        }
        return true;
    }

    private static void setNestedIfAbsent(ObjectNode root, String[] keyPath,
            JsonNode value) {
        ObjectNode current = root;
        for (int i = 0; i < keyPath.length - 1; i++) {
            JsonNode child = current.get(keyPath[i]);
            if (child == null || child.isNull()) {
                ObjectNode created = JacksonUtils.createObjectNode();
                current.set(keyPath[i], created);
                current = created;
            } else if (child instanceof ObjectNode childObject) {
                current = childObject;
            } else {
                return;
            }
        }
        String leaf = keyPath[keyPath.length - 1];
        if (current.get(leaf) == null) {
            current.set(leaf, value);
        }
    }
}
