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

import com.vaadin.flow.js.JsCall;
import com.vaadin.flow.js.JsCalledWith;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsSnippet;
import com.vaadin.flow.js.JsSnippets;

/**
 * A declared JavaScript expression that decides if a DOM event is sent to a
 * listener. Use it with
 * {@link DomListenerRegistration#setFilter(DomEventFilter)}.
 * <p>
 * To declare a filter, add a method that returns this type to a
 * {@link JsDefinition} interface. To create a filter, call that method on the
 * object that {@link JsSnippets#of(Class)} returns.
 * <p>
 * The expression is the body of a function. The function gets two parameters:
 * the DOM <code>event</code>, and the <code>element</code> that the listener is
 * added to. It returns <code>true</code> if the event should be sent. The
 * arguments of the method call are captured, and the expression uses them as
 * <code>$0</code>, <code>$1</code>, and so on:
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
 * The build adds the expression to the bundle. Because of this, the browser
 * does not compile any JavaScript from a string. All filters from the same
 * method use the same function, also when the captured values are different.
 * <p>
 * The captured values are checked when the filter is created. If a value cannot
 * be sent to the browser, creating the filter fails.
 * <p>
 * The captured values are encoded each time the settings of the listener are
 * sent to the browser. This happens when the listeners of the element change,
 * and when the whole UI is sent again, for example after a page refresh with
 * <code>@PreserveOnRefresh</code>. A captured element or component is sent with
 * the id that it has at that time.
 * <p>
 * So a captured element or component must be attached before the settings are
 * sent: in the same round trip in which the filter is set on a listener, or
 * earlier. If it is not attached at that time, the browser gets
 * <code>null</code> for it until the settings are sent again. The browser also
 * gets <code>null</code> for a captured element or component while it is
 * detached. A declared expression that uses such a captured value must handle
 * <code>null</code>.
 *
 * @see JsSnippets#of(Class)
 * @see DomEventData
 */
@JsCalledWith({ "event", "element" })
public final class DomEventFilter implements JsSnippet {

    private final JsCall declaredCall;
    private final String snippetKey;

    private DomEventFilter(JsCall declaredCall) {
        this.declaredCall = declaredCall;
        this.snippetKey = declaredCall.createSnippetKey();
    }

    /**
     * Creates a filter for the given call of a {@link JsDefinition} method. The
     * method must return a filter.
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
     * filter. The call tells which function of the bundle to run.
     * <p>
     * For internal use only. May be renamed or removed in a future release.
     *
     * @return the declaring call, not <code>null</code>
     */
    public JsCall getDeclaredCall() {
        return declaredCall;
    }

    /**
     * Gets the key that identifies this snippet: its function and its captured
     * values. It does not change after the snippet is created. See
     * {@link JsCall#createSnippetKey()}.
     * <p>
     * For internal use only. May be renamed or removed in a future release.
     *
     * @return the key of the snippet, not <code>null</code>
     */
    public String getSnippetKey() {
        return snippetKey;
    }
}
