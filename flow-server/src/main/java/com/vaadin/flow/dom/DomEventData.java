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
import tools.jackson.databind.node.ArrayNode;

import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.js.JsCall;
import com.vaadin.flow.js.JsCalledWith;
import com.vaadin.flow.js.JsDefinition;
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
 * method declares:
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
 * The build adds the expression to the bundle. Because of this, the browser
 * does not compile any JavaScript from a string. All event data from the same
 * method uses the same function, also when the captured values are different.
 * You can use the same method several times on one listener with different
 * values. Each value is read separately.
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
 * If all captured values are plain values, such as strings, numbers, booleans
 * or enums, you can read the value with any event data created with the same
 * method and equal values. Otherwise, for example when an element is captured,
 * read the value with the same instance that you added to the listener.
 *
 * @param <T>
 *            the type of the value
 * @see JsSnippets#of(Class)
 * @see DomEventFilter
 */
@JsCalledWith({ "event", "element" })
public final class DomEventData<T> implements JsSnippet {

    private final JsCall declaredCall;
    private final String captureIdentity;
    private final JavaType valueType;

    private DomEventData(JsCall declaredCall, JavaType valueType) {
        this.declaredCall = declaredCall;
        this.captureIdentity = declaredCall.createSnippetCaptureIdentity();
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
     * Encodes the captured values to send them to the browser. They are encoded
     * again each time they are sent, so that an element is sent with the id
     * that it has at that time.
     * <p>
     * For internal use only. May be renamed or removed in a future release.
     *
     * @return the encoded captures, not <code>null</code>
     */
    public ArrayNode encodeCaptures() {
        return declaredCall.encodeSnippetCaptures();
    }

    /**
     * Gets the number of captured values.
     * <p>
     * For internal use only. May be renamed or removed in a future release.
     *
     * @return the number of captures
     */
    public int getCaptureCount() {
        return declaredCall.flattenArguments().size();
    }

    /**
     * Gets a string that identifies the captured values of this event data. It
     * does not change after the event data is created. See
     * {@link JsCall#createSnippetCaptureIdentity()}.
     * <p>
     * For internal use only. May be renamed or removed in a future release.
     *
     * @return the identity of the captures, not <code>null</code>
     */
    public String getCaptureIdentity() {
        return captureIdentity;
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
}
