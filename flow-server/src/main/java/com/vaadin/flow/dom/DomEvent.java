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

import java.util.Arrays;
import java.util.Comparator;
import java.util.EventObject;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.internal.JacksonCodec;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.NodeOwner;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.flow.shared.JsonConstants;

/**
 * Server-side representation of a DOM event fired in the browser.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class DomEvent extends EventObject {

    /*
     * A simplified JavaScript identifier: a letter, underscore or dollar sign,
     * followed by any number of letters, digits, underscores or dollar signs.
     * Event data keys made of such identifiers separated by dots, e.g.
     * event.detail.r, are property paths that are also made available as nested
     * objects.
     */
    private static final Pattern IDENTIFIER_PATTERN = Pattern
            .compile("[\\p{L}_$][\\p{L}\\p{N}_$]*");

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
        if (!eventData.has(key)) {
            return null;
        }
        final JsonNode reportedStateNodeId = eventData.get(key);
        if (reportedStateNodeId == null || reportedStateNodeId.isNull()) {
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
     * @see DomListenerRegistration#addEventDataPath(String)
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
     * Event data expressions that are property paths, such as
     * {@code event.clientX}, are also made available as nested objects, so that
     * a type mirroring the structure of the event, as used with
     * {@link DomListenerRegistration#addEventData(Class)}, can be decoded.
     * Maps, {@link Object} and {@link JsonNode} types get the event data as is,
     * with one key per expression.
     * <p>
     * Example usage:
     *
     * <pre>
     * MyDto dto = domEvent.getEventData(MyDto.class);
     * </pre>
     *
     * @param <T>
     *            the type to deserialize to
     * @param type
     *            the class to deserialize the event data to, not
     *            <code>null</code>
     * @return the event data deserialized as the given type
     * @see DomListenerRegistration#addEventDataPath(String)
     * @since 25.0
     */
    public <T> T getEventData(Class<T> type) {
        return JacksonCodec.decodeAs(getEventDataForType(
                JacksonUtils.getMapper().constructType(type)), type);
    }

    /**
     * Gets the event data deserialized as the type specified by the
     * {@link TypeReference}. This method supports generic types such as
     * {@code List<MyBean>} and {@code Map<String, MyBean>} through Jackson's
     * TypeReference mechanism.
     * <p>
     * Event data expressions that are property paths, such as
     * {@code event.clientX}, are also made available as nested objects, so that
     * a type mirroring the structure of the event, as used with
     * {@link DomListenerRegistration#addEventData(TypeReference)}, can be
     * decoded. Maps, {@link Object} and {@link JsonNode} types get the event
     * data as is, with one key per expression.
     * <p>
     * Example usage:
     *
     * <pre>
     * TypeReference&lt;List&lt;MyDto&gt;&gt; typeRef = new TypeReference&lt;List&lt;MyDto&gt;&gt;() {
     * };
     * List&lt;MyDto&gt; dtos = domEvent.getEventData(typeRef);
     * </pre>
     *
     * @param <T>
     *            the type to deserialize to
     * @param typeReference
     *            the type reference describing the target type, not
     *            <code>null</code>
     * @return the event data deserialized as the given type
     * @see DomListenerRegistration#addEventDataPath(String)
     * @since 25.0
     */
    public <T> T getEventData(TypeReference<T> typeReference) {
        return JacksonCodec.decodeAs(
                getEventDataForType(
                        JacksonUtils.getMapper().constructType(typeReference)),
                typeReference);
    }

    /**
     * Gets the value of the given event data. The browser sends the value
     * together with this event. The value has the type that the declaring
     * method defines. For example, for a method that returns
     * <code>DomEventData&lt;String&gt;</code>, the value is a
     * <code>String</code>. For <code>DomEventData&lt;Element&gt;</code>, the
     * value is the element, like with
     * {@link #getEventDataElement(DomEventData)}, or <code>null</code>.
     * <p>
     * Pass the instance that you added to the listener. If all arguments of the
     * event data are plain values (strings, numbers, booleans, characters,
     * enums or <code>null</code>), another instance created with the same
     * method and equal arguments also works. With any other argument, such as
     * an element, a record or a list, another instance gets <code>null</code>.
     *
     * @param <T>
     *            the type of the value
     * @param eventData
     *            event data added to the listener with
     *            {@link DomListenerRegistration#addEventData(DomEventData)},
     *            not <code>null</code>
     * @return the value, or <code>null</code> if the browser sent no value for
     *         the event data, or if the event data was not added to the
     *         listener
     * @throws IllegalArgumentException
     *             if the value cannot be read as the declared type
     */
    @SuppressWarnings("unchecked")
    public <T> T getEventData(DomEventData<T> eventData) {
        Objects.requireNonNull(eventData, "Event data cannot be null");
        String key = ElementListenerMap.getEventDataKey(eventData);
        if (eventData.isElementValue()) {
            return (T) extractElement(this.eventData, getSource(), key, true);
        }
        JsonNode value = this.eventData.get(key);
        if (value == null) {
            return null;
        }
        return JacksonCodec.decodeAs(value, eventData.getValueType());
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
     * {@link DomListenerRegistration#addEventDataElementPath(String)} or
     * {@link DomListenerRegistration#addEventDataElement(String)}.
     * <p>
     * If the evaluated JS expression returned an element that is not created or
     * controlled by the server side, the closest parent element that is
     * controlled is returned instead. Invisible elements are not reported.
     * <p>
     * In case you want the {@code event.target} element, use
     * {@link #getEventTarget()} instead.
     * <p>
     * The browser compiles the expression from a string, so it does not work
     * with a Content Security Policy that does not allow
     * <code>unsafe-eval</code>, unless it is a property path. For an element
     * from declared event data, which works without that, use
     * {@link #getEventDataElement(DomEventData)}.
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
     * Gets the closest {@link Element} corresponding to the DOM node that the
     * given declared event data returned in the browser. This works for event
     * data added with
     * {@link DomListenerRegistration#addEventData(DomEventData)}.
     * <p>
     * If the DOM node is not created or controlled by the server side, the
     * closest parent element that is controlled is returned instead. Invisible
     * elements are not reported.
     * <p>
     * Unlike {@link #getEventDataElement(String)}, the browser runs a function
     * from the bundle instead of compiling an expression from a string, so this
     * also works with a Content Security Policy that does not allow
     * <code>unsafe-eval</code>.
     *
     * <pre>
     * &#64;JsDefinition
     * public interface PathJs extends Serializable {
     *
     *     &#64;JsExpression("return event.composedPath()[0];")
     *     DomEventData&lt;Element&gt; originalTarget();
     * }
     *
     * DomEventData&lt;Element&gt; target = JsSnippets.of(PathJs.class)
     *         .originalTarget();
     * element.addEventListener("click",
     *         event -&gt; event.getEventDataElement(target).ifPresent(this::show))
     *         .addEventData(target);
     * </pre>
     *
     * @param eventData
     *            event data that returns an element, added to the listener, not
     *            <code>null</code>
     * @return the element that corresponds to the returned DOM node, or an
     *         empty optional if there is none
     * @see #getEventData(DomEventData)
     */
    public Optional<Element> getEventDataElement(
            DomEventData<Element> eventData) {
        return Optional.ofNullable(getEventData(eventData));
    }

    /**
     * Gets the {@code event.detail} property from the event, deserialized as
     * the given type. This method supports arbitrary bean types and Java
     * records through Jackson deserialization.
     * <p>
     * The {@code event.detail} property must have been included in the event
     * data using {@link DomListenerRegistration#addEventDetail()}, or its
     * properties using {@link DomListenerRegistration#addEventDetail(Class)}.
     * <p>
     * Detail properties that are property paths, such as {@code color.r}, are
     * also made available as nested objects, so that a type with nested
     * properties, as used with
     * {@link DomListenerRegistration#addEventDetail(Class)}, can be decoded.
     * Maps, {@link Object} and {@link JsonNode} types get the detail as is,
     * with one key per property.
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
        JsonNode detailNode = getDetailNodeForType(
                JacksonUtils.getMapper().constructType(type));
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
     * data using {@link DomListenerRegistration#addEventDetail()}, or its
     * properties using {@link DomListenerRegistration#addEventDetail(Class)}.
     * <p>
     * Detail properties that are property paths, such as {@code color.r}, are
     * also made available as nested objects, so that a type with nested
     * properties, as used with
     * {@link DomListenerRegistration#addEventDetail(Class)}, can be decoded.
     * Maps, {@link Object} and {@link JsonNode} types get the detail as is,
     * with one key per property.
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
        JsonNode detailNode = getDetailNodeForType(
                JacksonUtils.getMapper().constructType(typeReference));
        if (detailNode == null) {
            return null;
        }
        return JacksonCodec.decodeAs(detailNode, typeReference);
    }

    /**
     * Gets the event.detail node to decode as the given type. Maps,
     * {@link Object} and {@link JsonNode} types get the detail as is, other
     * types get it with nested objects for property paths.
     *
     * @param type
     *            the type to decode the event detail as
     * @return the detail node to decode, or null if no detail data is present
     */
    private JsonNode getDetailNodeForType(JavaType type) {
        JsonNode detailNode = getDetailNode();
        if (detailNode == null || isDecodedAsIs(type)) {
            return detailNode;
        }
        return withNestedPaths(detailNode);
    }

    /**
     * Gets or reconstructs the event.detail node from event data. Handles two
     * cases:
     * <ul>
     * <li>Case 1: event.detail sent as a complete object
     * <li>Case 2: event.detail properties sent individually (e.g.,
     * event.detail.r, event.detail.g)
     * </ul>
     *
     * @return the detail node, or null if no detail data is present
     */
    private JsonNode getDetailNode() {
        JsonNode detailNode = eventData.get("event.detail");
        if (detailNode != null && !detailNode.isNull()) {
            // Case 1: event.detail sent as a complete object
            return detailNode;
        }

        // Case 2: event.detail properties sent individually
        boolean hasDetailProperties = JacksonUtils.getKeys(eventData).stream()
                .anyMatch(key -> key.startsWith("event.detail."));

        if (!hasDetailProperties) {
            return null;
        }

        // Reconstruct the detail object from individual properties by
        // stripping the "event.detail." prefix
        ObjectNode detailObject = JacksonUtils.createObjectNode();
        JacksonUtils.getKeys(eventData).forEach(key -> {
            if (key.startsWith("event.detail.")) {
                String propertyName = key.substring("event.detail.".length());
                detailObject.set(propertyName, eventData.get(key));
            }
        });

        return detailObject;
    }

    /**
     * Gets the event data to decode as the given type. Maps, {@link Object} and
     * {@link JsonNode} types get the event data as is, other types get it with
     * nested objects for property paths.
     *
     * @param type
     *            the type to decode the event data as
     * @return the event data to decode
     */
    private JsonNode getEventDataForType(JavaType type) {
        if (isDecodedAsIs(type)) {
            return eventData;
        }
        return withNestedPaths(eventData);
    }

    private static boolean isDecodedAsIs(JavaType type) {
        Class<?> rawClass = type.getRawClass();
        return type.isMapLikeType() || rawClass == Object.class
                || JsonNode.class.isAssignableFrom(rawClass);
    }

    /**
     * Creates a copy of the given data where the values of property path keys,
     * e.g. {@code event.clientX}, are additionally available as nested objects,
     * e.g. {@code {"event": {"clientX": ...}}}. The original keys are kept as
     * is, and an existing value is never replaced.
     *
     * @param data
     *            the data to add nested objects to
     * @return a copy of the data with nested objects for property paths, or the
     *         data itself if it is not an object
     */
    private static JsonNode withNestedPaths(JsonNode data) {
        if (!(data instanceof ObjectNode dataObject)) {
            return data;
        }
        ObjectNode result = dataObject.deepCopy();
        // Shorter paths first so that e.g. a complete event.detail object is
        // used as the base for the event.detail.* properties
        JacksonUtils.getKeys(data).stream().filter(DomEvent::isPropertyPath)
                .sorted(Comparator.comparingInt(String::length))
                .forEach(key -> setNestedValue(result, key.split("\\."),
                        data.get(key)));
        return result;
    }

    private static boolean isPropertyPath(String key) {
        String[] parts = key.split("\\.", -1);
        return parts.length > 1 && Arrays.stream(parts)
                .allMatch(part -> IDENTIFIER_PATTERN.matcher(part).matches());
    }

    private static void setNestedValue(ObjectNode target, String[] path,
            JsonNode value) {
        ObjectNode current = target;
        for (int i = 0; i < path.length - 1; i++) {
            JsonNode child = current.get(path[i]);
            if (child == null || child.isNull()) {
                child = current.putObject(path[i]);
            } else if (!(child instanceof ObjectNode)) {
                return;
            }
            current = (ObjectNode) child;
        }
        String name = path[path.length - 1];
        if (!current.has(name)) {
            current.set(name, value.deepCopy());
        }
    }
}
