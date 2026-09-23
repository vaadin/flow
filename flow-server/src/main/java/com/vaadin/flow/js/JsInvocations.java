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
package com.vaadin.flow.js;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementUtil;

/**
 * The JavaScript that a UI has scheduled, read from its queue once and then
 * queried as often as needed.
 * <p>
 * Reading the queue empties it, so a caller that wants to know more than one
 * thing about what was scheduled — which calls one definition got, in which
 * order two of them were made, what a single call carries — has to keep what it
 * read. This is that snapshot, for a driver of the client side that acts on the
 * calls instead of running JavaScript, and for a test that asserts what the
 * server scheduled:
 *
 * <pre>
 * element.callJsFunction("open", "Saved");
 *
 * JsInvocations scheduled = JsInvocations.dumpFrom(ui);
 * assertEquals(List.of("open"), scheduled.getFunctionCalls().stream()
 *         .map(FunctionCall::functionName).toList());
 * </pre>
 *
 * The invocations keep the order they were scheduled in, whether they are calls
 * of a JavaScript definition or plain JavaScript, so a caller can also tell
 * which of two calls came first.
 */
public final class JsInvocations implements Serializable {

    /**
     * Something a UI has scheduled for the client to run: either a {@link Call}
     * of a JavaScript definition or a {@link Script} of plain JavaScript.
     */
    public sealed interface Invocation extends Serializable {

        /**
         * Gets the element that the invocation belongs to, which is what
         * decides when it is sent and what it is discarded with. JavaScript
         * scheduled on the page belongs to the element of the UI.
         *
         * @return the owner element, or <code>null</code> if the invocation is
         *         owned by a node that is not an element, such as a shadow root
         */
        @Nullable
        Element owner();
    }

    /**
     * A call of a method of a JavaScript definition, scheduled with
     * {@link Element#executeJs(Class)} or by an API of its own that is built on
     * it, such as {@link Element#callJsFunction(String, Object...)}.
     *
     * @param call
     *            the call that the invocation performs
     * @param owner
     *            the element the invocation belongs to, or <code>null</code>
     *            for a node that is not an element
     */
    public record Call(JsCall call,
            @Nullable Element owner) implements Invocation {
    }

    /**
     * Plain JavaScript, scheduled with
     * {@link Element#executeJs(String, Object...)} as an expression that the
     * browser compiles.
     *
     * @param expression
     *            the JavaScript expression
     * @param parameters
     *            the parameters the expression is given, which it references as
     *            <code>$0</code>, <code>$1</code> and so on
     * @param owner
     *            the element the invocation belongs to, or <code>null</code>
     *            for a node that is not an element
     */
    public record Script(String expression, List<Object> parameters,
            @Nullable Element owner) implements Invocation {
    }

    /**
     * A call of a named function on an element, scheduled with
     * {@link Element#callJsFunction(String, Object...)}.
     * <p>
     * The client is sent the name of the function and the arguments of the call
     * rather than JavaScript that names the function, so every such invocation
     * carries the same expression and the called function can only be told from
     * the call itself.
     *
     * @param functionName
     *            the name of the called function, which contains dots for a
     *            function on a property, such as
     *            <code>$connector.scrollToItem</code>
     * @param arguments
     *            the arguments of the call, without the name of the function
     * @param owner
     *            the element the function is called on, or <code>null</code>
     *            for a node that is not an element
     */
    public record FunctionCall(String functionName, List<Object> arguments,
            @Nullable Element owner) implements Serializable {
    }

    private final List<Invocation> invocations;

    private JsInvocations(List<PendingJavaScriptInvocation> pending) {
        List<Invocation> read = new ArrayList<>(pending.size());
        for (PendingJavaScriptInvocation pendingInvocation : pending) {
            JavaScriptInvocation invocation = pendingInvocation.getInvocation();
            Element owner = ElementUtil.from(pendingInvocation.getOwner())
                    .orElse(null);
            JsCall call = invocation.getJsCall();
            read.add(call != null ? new Call(call, owner)
                    : new Script(invocation.getExpression(),
                            invocation.getParameters(), owner));
        }
        invocations = Collections.unmodifiableList(read);
    }

    /**
     * Takes what the given UI has scheduled for the client, which empties its
     * queue the same way sending a response to the client does.
     *
     * @param ui
     *            the UI to read the scheduled JavaScript from, not
     *            <code>null</code>
     * @return the scheduled invocations, not <code>null</code>
     */
    public static JsInvocations dumpFrom(UI ui) {
        Objects.requireNonNull(ui, "UI cannot be null");
        return new JsInvocations(
                ui.getInternals().dumpPendingJavaScriptInvocations());
    }

    /**
     * Gets every invocation that was scheduled, in the order it was scheduled
     * in.
     *
     * @return the invocations, not <code>null</code>
     */
    public List<Invocation> getInvocations() {
        return invocations;
    }

    /**
     * Gets the calls of the given JavaScript definition, in the order they were
     * scheduled in, leaving out the calls of every other definition and the
     * plain JavaScript.
     *
     * @param definitionType
     *            the JavaScript definition to get the calls of, not
     *            <code>null</code>
     * @return the calls of the definition, not <code>null</code>
     */
    public List<Call> getCalls(Class<?> definitionType) {
        Objects.requireNonNull(definitionType,
                "Definition type cannot be null");
        return invocations.stream().filter(Call.class::isInstance)
                .map(Call.class::cast)
                .filter(call -> call.call().definitionType() == definitionType)
                .toList();
    }

    /**
     * Gets the function calls that were scheduled with
     * {@link Element#callJsFunction(String, Object...)}, in the order they were
     * scheduled in, as the name of the called function and the arguments of the
     * call.
     *
     * @return the function calls, not <code>null</code>
     */
    public List<FunctionCall> getFunctionCalls() {
        return getCalls(Element.CallFunctionJs.class).stream()
                .map(JsInvocations::readFunctionCall).toList();
    }

    private static FunctionCall readFunctionCall(Call scheduled) {
        // The declaration of the called JavaScript takes the name of the
        // function first and collects the arguments of the call after it
        List<Object> arguments = scheduled.call().flattenArguments();
        return new FunctionCall((String) arguments.get(0),
                arguments.subList(1, arguments.size()), scheduled.owner());
    }
}
