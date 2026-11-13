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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

import tools.jackson.databind.JavaType;

import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.js.JsCall;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsFunctionParameterNames;
import com.vaadin.flow.js.JsSnippet;
import com.vaadin.flow.js.JsSnippets;

/**
 * A declared JavaScript expression. When a DOM event occurs, the browser runs
 * the expression and sends its value to the server together with the event. Use
 * it with {@link DomListenerRegistration#addEventData(DomEventData)}.
 * <p>
 * To declare event data, add a method that returns this type to a
 * {@link JsDefinition} interface. To create event data, call that method on the
 * object that {@link JsSnippets#of(Class)} returns.
 * <p>
 * The expression is the body of a function. The function gets two parameters:
 * the DOM <code>event</code>, and the <code>element</code> that the listener is
 * added to. It returns the value to send. The arguments of the method call are
 * captured, and the expression uses them as <code>$0</code>, <code>$1</code>,
 * and so on.
 * <p>
 * In the listener, read the value with
 * {@link DomEvent#getEventData(DomEventData)}. The value has the type that the
 * method declares. For <code>DomEventData&lt;Element&gt;</code>, the expression
 * returns a DOM node, and the value is the closest {@link Element} of the
 * server that is an ancestor of the node or the node itself, see
 * {@link DomEvent#getEventDataElement(DomEventData)}:
 *
 * <pre>
 * &#64;JsDefinition
 * public interface ClipboardJs extends Serializable {
 *
 *     &#64;JsExpression("return event.clipboardData?.getData($0) || null;")
 *     DomEventData&lt;String&gt; clipboardData(String mimeType);
 * }
 *
 * static final ClipboardJs CLIPBOARD = JsSnippets.of(ClipboardJs.class);
 *
 * DomEventData&lt;String&gt; pasted = CLIPBOARD.clipboardData("text/plain");
 * field.getElement()
 *         .addEventListener("paste",
 *                 event -&gt; handle(event.getEventData(pasted)))
 *         .addEventData(pasted);
 * </pre>
 * <p>
 * The build adds the expression to the bundle, so the browser does not compile
 * any JavaScript from a string. Event data from the same method shares one
 * function in the browser, also when the arguments are different. Each one
 * still sends its own value: a listener can have both
 * <code>clipboardData("text/plain")</code> and
 * <code>clipboardData("text/html")</code>, and
 * {@link DomEvent#getEventData(DomEventData)} returns the value of the one that
 * you pass to it.
 * <p>
 * When all arguments are plain values (strings, numbers, booleans, characters,
 * enums or <code>null</code>), the settings of the listener are stored in a
 * cache that the server and the browser keep for the lifetime of the UI. Each
 * different value adds one entry to this cache, and entries are never removed.
 * So avoid a captured value that is different for each element, such as a row
 * id on every row of a long list. Read such a value from the element instead,
 * for example from an attribute.
 * <p>
 * The captured values are checked when the event data is created. If a value
 * cannot be sent to the browser, creating the event data fails.
 * <p>
 * The captured values are encoded each time the settings of the listener are
 * sent to the browser. This happens when the listeners of the element change,
 * and when the whole UI is sent again, for example after a page refresh with
 * <code>@PreserveOnRefresh</code>. A captured element or component is sent with
 * the id that it has at that time.
 * <p>
 * So a captured element or component must be attached before the settings are
 * sent: in the same round trip in which the event data is added to a listener,
 * or earlier. If it is not attached at that time, the browser gets
 * <code>null</code> for it until the settings are sent again. The browser also
 * gets <code>null</code> for a captured element or component while it is
 * detached. A declared expression that uses such a captured value must handle
 * <code>null</code>.
 * <p>
 * {@link DomEvent#getEventData(DomEventData)} finds the value by the method and
 * the arguments of the event data that you pass to it. If all arguments are
 * plain values (strings, numbers, booleans, characters, enums or
 * <code>null</code>), you can pass any event data created with the same method
 * and equal arguments. With any other argument, such as an element, a record or
 * a list, pass the same instance that you added to the listener. Another
 * instance gets <code>null</code>, so keep the instance in a variable.
 *
 * @param <T>
 *            the type of the value
 * @see JsSnippets#of(Class)
 * @see DomEventFilter
 */
@JsFunctionParameterNames({ "event", "element" })
public final class DomEventData<T> implements JsSnippet {

    private final JsCall declaredCall;
    private final String snippetKey;
    private final JavaType valueType;

    private DomEventData(JsCall declaredCall, JavaType valueType) {
        this.declaredCall = declaredCall;
        this.snippetKey = declaredCall.createSnippetKey();
        this.valueType = valueType;
    }

    /**
     * Creates event data for the given call of a {@link JsDefinition} method.
     * The method must return event data.
     * <p>
     * For internal use only. May be renamed or removed in a future release.
     * Create event data by calling the relevant method on the implementation
     * returned by {@link JsSnippets#of(Class)}.
     *
     * @param <T>
     *            the type of the value
     * @param call
     *            the call of a method that returns event data, not
     *            <code>null</code>
     * @param returnType
     *            the generic return type of the called method. The value is
     *            read as this type. Not <code>null</code>
     * @return new event data, not <code>null</code>
     * @throws IllegalArgumentException
     *             if the called method does not declare a {@link JsSnippet}
     * @throws RuntimeException
     *             if any argument of the call cannot be encoded to be sent to
     *             the client
     */
    public static <T> DomEventData<T> ofDeclaredCall(JsCall call,
            Type returnType) {
        Objects.requireNonNull(call, "call");
        // DomEventData<String> is read as a String. A raw or unknown type is
        // read as the JSON value as such
        Type valueType = returnType instanceof ParameterizedType parameterized
                ? parameterized.getActualTypeArguments()[0]
                : Object.class;
        return new DomEventData<>(call, JacksonUtils.getMapper()
                .getTypeFactory().constructType(valueType));
    }

    /**
     * Gets the call of the {@link JsDefinition} method that declared this event
     * data. The call tells which function of the bundle to run.
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

    /**
     * Gets the type to read the value as. The method that declared this event
     * data defines the type.
     *
     * @return the value type, not <code>null</code>
     */
    JavaType getValueType() {
        return valueType;
    }

    /**
     * Checks whether the value of this event data is an {@link Element}. The
     * browser then sends the closest element that the server knows instead of
     * the DOM node that the expression returns.
     * <p>
     * For internal use only. May be renamed or removed in a future release.
     *
     * @return <code>true</code> if the value type is {@link Element},
     *         <code>false</code> otherwise
     */
    public boolean isElementValue() {
        return valueType.getRawClass() == Element.class;
    }
}
