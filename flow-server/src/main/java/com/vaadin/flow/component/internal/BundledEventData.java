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
package com.vaadin.flow.component.internal;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventBusUtil;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.internal.StringUtil;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.flow.shared.JsonConstants;

/**
 * Tells which {@link EventData} values of a {@link DomEvent} class the build
 * puts into the bundle as functions, and the identifier the server sends for
 * each of them instead of its JavaScript.
 * <p>
 * An {@link EventData} value is a JavaScript expression in an annotation, so it
 * is known when the application is built. The values that the client would
 * otherwise compile from a string, which a content security policy without
 * <code>unsafe-eval</code> does not allow, are generated into the bundle as
 * <code>(event, element) =&gt; (value)</code>, keyed by a hash of the value.
 * The build and the server both decide through this class which values those
 * are and what their identifiers are, so they always agree.
 * <p>
 * A value that maps to a component or an element is in the event data with the
 * prefix {@link JsonConstants#MAP_STATE_NODE_EVENT_DATA}. Its function is the
 * expression without the prefix, so it shares the function of the same
 * expression used as a plain value, and the server tells the client with
 * {@link JsonConstants#EVENT_SETTINGS_MAP_ELEMENT} to map what the function
 * returns.
 * <p>
 * The values of a {@link PrebuiltEventData} event, which are keys rather than
 * JavaScript, are not put into the bundle. Neither is a value that the client
 * reads without compiling it, such as a property path like
 * <code>event.clientX</code>.
 * <p>
 * The {@link DomEvent#filter()} of an event type is a JavaScript expression in
 * an annotation too, and the client runs it like event data, so a filter that
 * is not a property path is put into the bundle in the same way, see
 * {@link #getBundledFilter(Class)}. A filter and event data with the same
 * expression share one function.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class BundledEventData {

    private BundledEventData() {
        // Static methods only
    }

    /**
     * Gets the event data of the given event type that the build puts into the
     * bundle as functions, in constructor parameter order.
     * <p>
     * For the build, which loads the event type with a class loader it closes
     * afterwards: the event type is scanned without a cache, as a static map
     * would otherwise keep that class loader alive.
     *
     * @param eventType
     *            a component event type annotated with {@link DomEvent}, not
     *            <code>null</code>
     * @return an unmodifiable list of the {@link EventData} values that are
     *         sent as the identifier of a function of the bundle, empty if
     *         there are none
     * @throws IllegalArgumentException
     *             if the event type has no suitable DOM event constructor
     */
    public static List<String> getBundledEventData(
            Class<? extends ComponentEvent<?>> eventType) {
        return new DomEventInfo(eventType).getBundledEventData();
    }

    /**
     * Gets the {@link DomEvent#filter()} of the given event type if the build
     * puts it into the bundle as a function, which is a filter that is not a
     * property path. The event type is read like the build reads it, also when
     * it inherits the annotation.
     *
     * @param eventType
     *            a component event type annotated with {@link DomEvent}, not
     *            <code>null</code>
     * @return the filter, or <code>null</code> if the event type has no filter
     *         or the client reads it without compiling it
     */
    public static @Nullable String getBundledFilter(Class<?> eventType) {
        DomEvent domEvent = eventType.getAnnotation(DomEvent.class);
        return domEvent != null && isBundledFilter(domEvent.filter())
                ? domEvent.filter()
                : null;
    }

    /**
     * Checks whether the given {@link DomEvent#filter()} is put into the bundle
     * as a function, which is a filter that is not empty and not a property
     * path that the client reads without compiling it.
     *
     * @param filter
     *            the filter of a {@link DomEvent} annotation, not
     *            <code>null</code>
     * @return <code>true</code> if the server sends the identifier of the
     *         function of the bundle for the filter
     */
    public static boolean isBundledFilter(String filter) {
        return !filter.isEmpty() && !ElementListenerMap.isPropertyPath(filter);
    }

    /**
     * Selects the event data that the build puts into the bundle as functions
     * from the event data of the given event type.
     *
     * @param eventType
     *            a component event type annotated with {@link DomEvent}, not
     *            <code>null</code>
     * @param listenerEventData
     *            the event data of the event type, as
     *            {@link ComponentEventBusUtil#getListenerEventData(Class)}
     *            returns it, not <code>null</code>
     * @return an unmodifiable list of the values of the given event data that
     *         are sent as the identifier of a function of the bundle, in the
     *         same order, empty if there are none
     */
    public static List<String> selectBundled(Class<?> eventType,
            List<String> listenerEventData) {
        if (AnnotationReader
                .getAnnotationFor(eventType, PrebuiltEventData.class)
                .isPresent()) {
            return List.of();
        }
        return listenerEventData.stream().filter(BundledEventData::isBundled)
                .toList();
    }

    /**
     * Gets the identifier of the function that the bundle registers for the
     * given bundled event data or filter, which is what the server sends
     * instead of the expression. A hash of the expression, so that nothing in
     * the browser names the class that declared it. A value mapped to a
     * component or an element has the identifier of its expression without the
     * prefix.
     *
     * @param eventData
     *            event data that {@link #getBundledEventData(Class)} returns,
     *            or a filter that {@link #getBundledFilter(Class)} returns, not
     *            <code>null</code>
     * @return the identifier of the function, not <code>null</code>
     */
    public static String getFunctionId(String eventData) {
        // The function of a JsCall is hashed from what starts with a digit,
        // "(" or "executionContext,", so this never shares their hash input
        return StringUtil.getHash("eventData:" + getExpression(eventData),
                StandardCharsets.UTF_8);
    }

    /**
     * Gets the JavaScript expression of the given bundled event data, which is
     * what the function of the bundle returns: the event data without the
     * prefix of a value mapped to a component or an element.
     *
     * @param eventData
     *            event data that {@link #getBundledEventData(Class)} returns,
     *            not <code>null</code>
     * @return the expression, not <code>null</code>
     */
    public static String getExpression(String eventData) {
        return isMappedToElement(eventData)
                ? eventData.substring(
                        JsonConstants.MAP_STATE_NODE_EVENT_DATA.length())
                : eventData;
    }

    /**
     * Checks whether the client maps what the expression of the given event
     * data returns to the closest state node, which is how a value of a
     * component or an element parameter is sent.
     *
     * @param eventData
     *            event data that {@link #getBundledEventData(Class)} returns,
     *            not <code>null</code>
     * @return <code>true</code> if the value is mapped to a component or an
     *         element
     */
    public static boolean isMappedToElement(String eventData) {
        return eventData.startsWith(JsonConstants.MAP_STATE_NODE_EVENT_DATA);
    }

    private static boolean isBundled(String eventData) {
        // A bare prefix, for event.target, and a mapped property path are read
        // by the client without compiling them, so they are not bundled
        return ElementListenerMap.isCompiledByClient(eventData);
    }
}
