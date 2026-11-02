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

import java.util.Objects;

import tools.jackson.databind.node.ArrayNode;

import com.vaadin.flow.js.JsCall;
import com.vaadin.flow.js.JsCalledWith;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsSnippet;
import com.vaadin.flow.js.JsSnippets;

/**
 * A declared JavaScript expression that decides whether a DOM event is sent to
 * a listener, for {@link DomListenerRegistration#setFilter(DomEventFilter)}.
 * <p>
 * A filter is declared by a method of a {@link JsDefinition} interface that
 * returns this type, and created by calling that method on the implementation
 * that {@link JsSnippets#of(Class)} hands out. The expression is the body of a
 * function that is called with the DOM <code>event</code> and the
 * <code>element</code> the listener is registered on, and returns whether the
 * event should be sent. The arguments of the call are captured and referenced
 * as <code>$0</code>, <code>$1</code>, &hellip;:
 *
 * <pre>
 * &#64;JsDefinition
 * public interface KeyFilters extends Serializable {
 *
 *     &#64;JsExpression("return event.key === $0 &amp;&amp; !event.repeat;")
 *     DomEventFilter isKey(String key);
 * }
 *
 * static final KeyFilters FILTERS = JsSnippets.of(KeyFilters.class);
 *
 * field.getElement().addEventListener("keydown", event -&gt; submit())
 *         .setFilter(FILTERS.isKey("Enter"));
 * </pre>
 * <p>
 * The build collects the expression into the bundle, so nothing is compiled
 * from a string in the browser, and all filters declared by the same method
 * share one function regardless of the captured values.
 * <p>
 * The captured values are encoded when the filter is created, so a value that
 * can not be sent to the client fails right away. An element or component
 * captured this way must already be attached when the filter is created, since
 * one that is not is sent as <code>null</code>, even if it is attached by the
 * time the filter is set on a listener. Create the filter after attaching, for
 * instance in an attach listener, rather than in a field initializer.
 *
 * @see JsSnippets#of(Class)
 * @see DomEventData
 */
@JsCalledWith({ "event", "element" })
public final class DomEventFilter implements JsSnippet {

    private final JsCall declaredCall;
    private final ArrayNode captures;

    private DomEventFilter(JsCall declaredCall) {
        this.declaredCall = declaredCall;
        this.captures = declaredCall.encodeSnippetCaptures();
    }

    /**
     * Creates a filter for the given call of a {@link JsDefinition} method that
     * returns a filter.
     * <p>
     * For internal use only. May be renamed or removed in a future release.
     * Create a filter by calling the relevant method on the implementation
     * returned by {@link JsSnippets#of(Class)}.
     *
     * @param call
     *            the call of a method that returns a filter, not
     *            <code>null</code>
     * @return a new filter, not <code>null</code>
     * @throws IllegalArgumentException
     *             if the called method does not declare a {@link JsSnippet}
     * @throws RuntimeException
     *             if any argument of the call cannot be encoded to be sent to
     *             the client
     */
    public static DomEventFilter ofDeclaredCall(JsCall call) {
        return new DomEventFilter(Objects.requireNonNull(call, "call"));
    }

    /**
     * Gets the call of the {@link JsDefinition} method that declared this
     * filter, which names the function of the bundle it runs.
     * <p>
     * For internal use only. May be renamed or removed in a future release.
     *
     * @return the declaring call, not <code>null</code>
     */
    public JsCall getDeclaredCall() {
        return declaredCall;
    }

    /**
     * Gets the captured values as they were encoded when this filter was
     * created, which is what they are sent to the client as. Not to be
     * modified.
     * <p>
     * For internal use only. May be renamed or removed in a future release.
     *
     * @return the encoded captures, not <code>null</code>
     */
    public ArrayNode getEncodedCaptures() {
        return captures;
    }
}
