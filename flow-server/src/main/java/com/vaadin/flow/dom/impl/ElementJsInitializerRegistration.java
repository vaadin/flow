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
package com.vaadin.flow.dom.impl;

import java.io.Serializable;
import java.util.Objects;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsDefinitionProxy;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsSnippets;
import com.vaadin.flow.js.internal.JsWithExecutionContext;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.shared.Registration;

/**
 * Server-side bookkeeping for a JavaScript initializer registered through
 * {@link Element#addJsInitializer(String, Object...)}, or by framework code
 * with an initializer declared as a {@link JsSnippets snippet}.
 * <p>
 * Re-emits the init invocation each time the client acquires a fresh DOM for
 * this element, and emits a matching dispose when the registration is removed.
 * The initializer is sent as a {@link JsFunction} parameter of the declared
 * {@link InitializerJs} calls, which delegate the registry bookkeeping to the
 * {@code registerInitializer} / {@code disposeInitializer} callbacks of the
 * client-side execution context. An initializer declared as a snippet is
 * therefore not compiled from a string in the browser.
 * <p>
 * For internal use only.
 * 
 * @since 25.2
 */
public final class ElementJsInitializerRegistration implements Registration {

    private final StateNode node;
    private final JsFunction initializer;

    // Allocated lazily from the UI-wide counter on the first emit, then
    // reused for any re-emits and the dispose invocation.
    private int initializerId = -1;

    private boolean sent;
    private boolean removed;
    private boolean scheduled;
    private Registration attachListenerRegistration;

    /**
     * Creates a new registration for an expression given by the application and
     * schedules the first init invocation if the node is currently attached.
     * The expression is compiled from a string in the browser.
     *
     * @param node
     *            the owning state node, not {@code null}
     * @param expression
     *            the user-supplied expression, not {@code null}
     * @param parameters
     *            the user-supplied parameters, captured by the JsFunction
     */
    public ElementJsInitializerRegistration(StateNode node, String expression,
            Object[] parameters) {
        // JsFunction.of validates each capture, so unsupported parameter
        // types fail fast here rather than at execution time.
        this(node, JsFunction.of(
                Objects.requireNonNull(expression, "expression"), parameters));
    }

    /**
     * Creates a new registration for the given initializer and schedules the
     * first init invocation if the node is currently attached.
     * <p>
     * The initializer is applied with the element as <code>this</code> and no
     * arguments, and returns a cleanup function, <code>null</code> or
     * <code>undefined</code>, as described for
     * {@link Element#addJsInitializer(String, Object...)}. Framework code
     * passes one declared as a {@link JsSnippets snippet}, so that nothing is
     * compiled from a string in the browser.
     *
     * @param node
     *            the owning state node, not {@code null}
     * @param initializer
     *            the initializer to run, not {@code null}
     */
    public ElementJsInitializerRegistration(StateNode node,
            JsFunction initializer) {
        this.node = Objects.requireNonNull(node, "node");
        this.initializer = Objects.requireNonNull(initializer, "initializer");

        // Do not convert to lambda: the returned registration captures this
        // listener and is stored in a field here, and lambda deserialization
        // cannot resolve that cycle (ClassCastException on the captured value).
        // Same workaround as ElementAttributeMap#deferRegistration.
        attachListenerRegistration = node.addAttachListener(new Command() {
            @Override
            public void execute() {
                onAttach();
            }
        });
        if (node.isAttached()) {
            onAttach();
        }
    }

    private void onAttach() {
        if (removed || scheduled) {
            return;
        }
        scheduled = true;
        UI ui = ((StateTree) node.getOwner()).getUI();
        ui.getInternals().getStateTree().beforeClientResponse(node, context -> {
            scheduled = false;
            if (removed) {
                return;
            }
            if (sent && context.isClientSideInitialized()) {
                return;
            }
            emitInit(context.getUI());
            sent = true;
        });
    }

    private void emitInit(UI ui) {
        if (initializerId == -1) {
            initializerId = ui.getInternals().nextJsInitializerId();
        }
        createInitializerJs(ui).init(initializerId, initializer);
    }

    private void emitDispose(UI ui) {
        createInitializerJs(ui).dispose(initializerId);
    }

    /**
     * Queues the calls on the UI for this node directly rather than through
     * {@link Element#executeJs(Class)}, which waits for the node to be attached
     * and for the next response, as both are emitted while that response is
     * being built.
     */
    private InitializerJs createInitializerJs(UI ui) {
        Element element = Element.get(node);
        return JsDefinitionProxy.create(InitializerJs.class,
                call -> ui.getInternals().addJavaScriptInvocation(node,
                        new JavaScriptInvocation(call, call.getExpression(),
                                call.parametersFor(element))));
    }

    @Override
    public void remove() {
        if (removed) {
            return;
        }
        removed = true;
        if (attachListenerRegistration != null) {
            attachListenerRegistration.remove();
            attachListenerRegistration = null;
        }
        if (sent && node.isAttached()) {
            UI ui = ((StateTree) node.getOwner()).getUI();
            ui.getInternals().getStateTree().beforeClientResponse(node,
                    context -> emitDispose(context.getUI()));
        }
    }

    /**
     * Runs and disposes of an initializer on the client, as a JavaScript
     * definition. Both calls run on the element of the registration.
     * <p>
     * For internal use only. May be renamed or removed in a future release.
     */
    @JsDefinition
    public interface InitializerJs extends Serializable {

        /**
         * Applies the initializer to the element and registers the cleanup
         * function it returns, to be called when the client discards the DOM
         * node of the element or the initializer is disposed of.
         *
         * @param initializerId
         *            the id of the initializer, unique within the UI
         * @param initializer
         *            the initializer to apply
         */
        @JsExpression("""
                const node = $executionContext.getNode(this);
                const cleanup = $1.apply(this);
                if (typeof cleanup === 'function') {
                  $executionContext.registerInitializer(node, $0, cleanup);
                } else if (cleanup !== undefined && cleanup !== null) {
                  console.error('addJsInitializer expression must return a function, null or undefined; got ' + (typeof cleanup));
                }
                """)
        @JsWithExecutionContext
        void init(int initializerId, JsFunction initializer);

        /**
         * Calls the cleanup function that the initializer with the given id
         * registered, if any.
         *
         * @param initializerId
         *            the id of the initializer
         */
        @JsExpression("""
                try {
                  $executionContext.disposeInitializer($executionContext.getNode(this), $0);
                } catch (e) {}
                """)
        @JsWithExecutionContext
        void dispose(int initializerId);
    }
}
