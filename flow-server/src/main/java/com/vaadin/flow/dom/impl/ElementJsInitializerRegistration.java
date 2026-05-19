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

import java.util.Objects;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.shared.Registration;

/**
 * Server-side bookkeeping for a JavaScript initializer registered through
 * {@link Element#addJsInitializer(String, Object...)}.
 * <p>
 * Re-emits the init invocation each time the client acquires a fresh DOM for
 * this element, and emits a matching dispose when the registration is removed.
 * The user expression is sent as a {@link JsFunction} parameter; the wrapper
 * expression itself contains only framework code that delegates registry
 * bookkeeping to {@code this.registerInitializer} /
 * {@code this.disposeInitializer}, which the client provides as part of the
 * {@code executeJs} execution context.
 * <p>
 * For internal use only.
 */
public final class ElementJsInitializerRegistration implements Registration {

    private static final String INIT_EXPRESSION = """
            const element = $0;
            const initializer = $1;
            const initializerId = $2;
            const node = this.getNode(element);
            const cleanup = initializer.apply(element);
            if (typeof cleanup === 'function') {
              this.registerInitializer(node, initializerId, cleanup);
            } else if (typeof cleanup !== 'undefined') {
              console.error('addJsInitializer expression must return a function or nothing; got ' + (typeof cleanup));
            }
            """;

    private static final String DISPOSE_EXPRESSION = """
            const element = $0;
            const initializerId = $1;
            try {
              this.disposeInitializer(this.getNode(element), initializerId);
            } catch (e) {}
            """;

    private final StateNode node;
    private final JsFunction userFunction;

    // Allocated lazily from the UI-wide counter on the first emit, then
    // reused for any re-emits and the dispose invocation.
    private int initializerId = -1;

    private boolean sent;
    private boolean removed;
    private boolean scheduled;
    private Registration attachListenerRegistration;

    /**
     * Creates a new registration and schedules the first init invocation if the
     * node is currently attached.
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
        this.node = Objects.requireNonNull(node, "node");
        Objects.requireNonNull(expression, "expression");
        // JsFunction.of validates each capture, so unsupported parameter
        // types fail fast here rather than at execution time.
        this.userFunction = JsFunction.of(expression, parameters);

        attachListenerRegistration = node.addAttachListener(this::onAttach);
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
        Object[] params = new Object[] { Element.get(node), userFunction,
                initializerId };
        JavaScriptInvocation invocation = new JavaScriptInvocation(
                INIT_EXPRESSION, params);
        ui.getInternals().addJavaScriptInvocation(
                new PendingJavaScriptInvocation(node, invocation));
    }

    private void emitDispose(UI ui) {
        Object[] params = new Object[] { Element.get(node), initializerId };
        JavaScriptInvocation invocation = new JavaScriptInvocation(
                DISPOSE_EXPRESSION, params);
        ui.getInternals().addJavaScriptInvocation(
                new PendingJavaScriptInvocation(node, invocation));
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
}
