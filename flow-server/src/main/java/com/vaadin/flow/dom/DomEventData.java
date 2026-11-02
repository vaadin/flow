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
 * A declared JavaScript expression whose value is sent to the server together
 * with a DOM event, for
 * {@link DomListenerRegistration#addEventData(DomEventData)}.
 * <p>
 * Event data is declared by a method of a {@link JsDefinition} interface that
 * returns this type, and created by calling that method on the implementation
 * that {@link JsSnippets#of(Class)} hands out. The expression is the body of a
 * function that is called with the DOM <code>event</code> and the
 * <code>element</code> the listener is registered on, and returns the value to
 * send. The arguments of the call are captured and referenced as
 * <code>$0</code>, <code>$1</code>, &hellip;. The value is read in the listener
 * with {@link DomEvent#getEventData(DomEventData)}, as the type that the method
 * declares:
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
 * The build collects the expression into the bundle, so nothing is compiled
 * from a string in the browser, and all event data declared by the same method
 * shares one function regardless of the captured values. The value is told
 * apart by what is captured, so the same method can be used several times on
 * one listener with different values.
 * <p>
 * The captured values are encoded when the event data is created, so a value
 * that can not be sent to the client fails right away. An element or component
 * captured this way must already be attached when the event data is created,
 * since one that is not is sent as <code>null</code>, even if it is attached by
 * the time the event data is added to a listener. Create the event data after
 * attaching, for instance in an attach listener, rather than in a field
 * initializer.
 *
 * @param <T>
 *            the type of the value
 * @see JsSnippets#of(Class)
 * @see DomEventFilter
 */
@JsCalledWith({ "event", "element" })
public final class DomEventData<T> implements JsSnippet {

    private final JsCall declaredCall;
    private final ArrayNode captures;
    private final JavaType valueType;

    private DomEventData(JsCall declaredCall, JavaType valueType) {
        this.declaredCall = declaredCall;
        this.captures = declaredCall.encodeSnippetCaptures();
        this.valueType = valueType;
    }

    /**
     * Creates event data for the given call of a {@link JsDefinition} method
     * that returns event data.
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
     *            the generic return type of the called method, which tells the
     *            type to read the value as, not <code>null</code>
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
        // DomEventData<String> is read as a String, while a raw or
        // undetermined type is read as whatever the JSON holds
        Type valueType = returnType instanceof ParameterizedType parameterized
                ? parameterized.getActualTypeArguments()[0]
                : Object.class;
        return new DomEventData<>(call, JacksonUtils.getMapper()
                .getTypeFactory().constructType(valueType));
    }

    /**
     * Gets the call of the {@link JsDefinition} method that declared this event
     * data, which names the function of the bundle it runs.
     * <p>
     * For internal use only. May be renamed or removed in a future release.
     *
     * @return the declaring call, not <code>null</code>
     */
    public JsCall getDeclaredCall() {
        return declaredCall;
    }

    /**
     * Gets the captured values as they were encoded when this event data was
     * created, which is what they are sent to the client as, and what the key
     * its value is read by is derived from. Not to be modified.
     * <p>
     * For internal use only. May be renamed or removed in a future release.
     *
     * @return the encoded captures, not <code>null</code>
     */
    public ArrayNode getEncodedCaptures() {
        return captures;
    }

    /**
     * Gets the type that the value is read as, as declared by the method that
     * declared this event data.
     *
     * @return the value type, not <code>null</code>
     */
    JavaType getValueType() {
        return valueType;
    }
}
