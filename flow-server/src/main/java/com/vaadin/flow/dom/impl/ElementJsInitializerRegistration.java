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
import com.vaadin.flow.internal.nodefeature.JsInitializerCounter;
import com.vaadin.flow.shared.Registration;

/**
 * Server-side bookkeeping for a JavaScript initializer registered through
 * {@link Element#addJsInitializer(String, Object...)}.
 * <p>
 * The user expression is encoded as a {@link JsFunction} and travels to the
 * client as a regular parameter, so the wrapper expression is a fixed framework
 * string with no user JavaScript concatenated into it.
 * <p>
 * For internal use only.
 */
public final class ElementJsInitializerRegistration implements Registration {

    /**
     * Wrapper expression. Parameters at runtime are: {@code $0} the user
     * initializer reified as a function (from a {@link JsFunction} with the
     * user-supplied parameters as captures); {@code $1} this element;
     * {@code $2} the initializer id.
     * <p>
     * The wrapper calls {@code $0} with this element as the receiver, so the
     * user body sees the element as {@code this} and references its own
     * parameters as {@code $0}, {@code $1}, &hellip; (the JsFunction's
     * captures). A {@code return} from the user body yields the cleanup
     * function. No inner IIFE is needed: the JsFunction itself provides the
     * function scope.
     */
    private static final String INIT_WRAPPER = """
            const __ctx = this;
            const __ns = window.Vaadin = window.Vaadin || {};
            const __flow = __ns.Flow = __ns.Flow || {};
            const __registry = __flow.initializers = __flow.initializers || (function() {
              const byNode = new WeakMap();
              function ensure(node, onFirst) {
                let entry = byNode.get(node);
                if (!entry) {
                  entry = new Map();
                  byNode.set(node, entry);
                  onFirst(entry);
                }
                return entry;
              }
              return {
                register: function(node, id, cleanup, onFirst) {
                  const entry = ensure(node, onFirst);
                  const existing = entry.get(id);
                  if (existing) { try { existing(); } catch (e) { console.error(e); } }
                  entry.set(id, cleanup);
                },
                dispose: function(node, id) {
                  const entry = byNode.get(node);
                  if (!entry) return;
                  const fn = entry.get(id);
                  entry.delete(id);
                  if (fn) { try { fn(); } catch (e) { console.error(e); } }
                },
                drain: function(node) {
                  const entry = byNode.get(node);
                  if (!entry) return;
                  byNode.delete(node);
                  entry.forEach(function(fn) {
                    try { fn(); } catch (e) { console.error(e); }
                  });
                  entry.clear();
                }
              };
            })();
            const __node = __ctx.getNode($1);
            const __cleanup = $0.apply($1);
            if (typeof __cleanup === 'function') {
              __registry.register(__node, $2, __cleanup, function() {
                __ctx.runOnNodeUnregister(__node, function() {
                  __registry.drain(__node);
                });
              });
            }
            """;

    private static final String DISPOSE_EXPRESSION = """
            const __ns = window.Vaadin;
            if (!__ns || !__ns.Flow || !__ns.Flow.initializers) return;
            try {
              __ns.Flow.initializers.dispose(this.getNode($1), $0);
            } catch (e) {}
            """;

    private final StateNode node;
    private final JsFunction userFunction;
    private final int initializerId;

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
        this.initializerId = node.getFeature(JsInitializerCounter.class).next();

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
        node.runWhenAttached(ui -> ui.getInternals().getStateTree()
                .beforeClientResponse(node, context -> {
                    scheduled = false;
                    if (removed) {
                        return;
                    }
                    if (sent && context.isClientSideInitialized()) {
                        return;
                    }
                    emitInit(context.getUI());
                    sent = true;
                }));
    }

    private void emitInit(UI ui) {
        Object[] params = new Object[] { userFunction, Element.get(node),
                initializerId };
        JavaScriptInvocation invocation = new JavaScriptInvocation(INIT_WRAPPER,
                params);
        ui.getInternals().addJavaScriptInvocation(
                new PendingJavaScriptInvocation(node, invocation));
    }

    private void emitDispose(UI ui) {
        Object[] params = new Object[] { initializerId, Element.get(node) };
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
            node.runWhenAttached(
                    ui -> ui.getInternals().getStateTree().beforeClientResponse(
                            node, context -> emitDispose(context.getUI())));
        }
    }
}
