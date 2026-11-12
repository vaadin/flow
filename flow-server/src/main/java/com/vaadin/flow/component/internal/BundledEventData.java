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
 * A value that maps to a component or an element, and the values of a
 * {@link PrebuiltEventData} event, which are keys rather than JavaScript, are
 * not put into the bundle. Neither is a value that the client reads without
 * compiling it, such as a property path like <code>event.clientX</code>.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 25.4
 */
public final class BundledEventData {

    private BundledEventData() {
        // Static methods only
    }

    /**
     * Gets the event data of the given event type that the build puts into the
     * bundle as functions, in constructor parameter order.
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
        return selectBundled(eventType,
                ComponentEventBusUtil.getListenerEventData(eventType));
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
     * given event data value, which is what the server sends instead of the
     * value. A hash of the value, so that nothing in the browser names the
     * class that declared it.
     *
     * @param expression
     *            an {@link EventData} value, not <code>null</code>
     * @return the identifier of the function, not <code>null</code>
     */
    public static String getFunctionId(String expression) {
        // The function of a JsCall is hashed from what starts with a digit,
        // "(" or "executionContext,", so this never shares their hash input
        return StringUtil.getHash("eventData:" + expression,
                StandardCharsets.UTF_8);
    }

    private static boolean isBundled(String eventData) {
        // A value mapped to a state node is sent with a prefix that tells the
        // client to map what the expression returns, which a function of the
        // bundle does not do
        return !eventData.startsWith(JsonConstants.MAP_STATE_NODE_EVENT_DATA)
                && ElementListenerMap.isCompiledByClient(eventData);
    }
}
