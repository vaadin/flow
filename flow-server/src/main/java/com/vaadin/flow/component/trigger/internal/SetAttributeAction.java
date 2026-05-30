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
package com.vaadin.flow.component.trigger.internal;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ElementAttributeMap;
import com.vaadin.flow.internal.nodefeature.ReturnChannelMap;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;

/**
 * Assigns a value to an HTML attribute on a target element when the bound
 * trigger fires.
 * <p>
 * Attributes differ from JS properties (see {@link SetPropertyAction}):
 * attributes live in the markup-facing namespace ({@code id}, {@code class},
 * {@code data-*}, ARIA attributes such as {@code aria-hidden}) and their values
 * are always strings in the DOM. A {@code null} value at fire time removes the
 * attribute via {@code removeAttribute}, matching the way the DOM models
 * "attribute is absent".
 * <p>
 * The value to assign can be either a literal (constant, serialised at build
 * time) or an {@link Action.Input} that produces the value on the client when
 * the trigger fires.
 * <p>
 * Common idioms:
 * <ul>
 * <li>Set an ARIA flag:
 * {@code new SetAttributeAction<>(panel, "aria-hidden", "true")}
 * <li>Remove an attribute:
 * {@code new SetAttributeAction<>(input, "disabled", (String) null)}
 * <li>Mirror a value from another component:
 * {@code new SetAttributeAction<>(target, "data-source", new PropertyInput<>(field, "value", String.class))}
 * </ul>
 *
 * <h2>Server-state mirror (opt-in)</h2>
 *
 * By default, this action runs client-only: the attribute is assigned (or
 * removed) in the browser and the server's view of the attribute is
 * <em>not</em> updated. As a result, {@link Element#getAttribute(String)} on
 * the target keeps returning whatever the server last set, which will drift
 * from what the user sees in the browser. Call {@link #mirrorToServer()} to opt
 * into mirroring the assigned value back into the target's
 * {@link ElementAttributeMap} so the server-side view tracks the browser. The
 * server-side update produced by the mirror does not generate another
 * client-bound change, so there is no echo round-trip.
 * <p>
 * <strong>Security implication of {@code mirrorToServer()}:</strong> when the
 * mirror is enabled, the value the server stores is whatever the client sent.
 * An end user with the browser devtools open can substitute any value for the
 * one this action would normally compute. <em>Server-side code must not rely on
 * the mirrored attribute value as a trusted source of truth</em> for
 * authorisation, validation, or any other security-sensitive decision; treat it
 * the same way you would treat any other client-originated input. If you need a
 * trusted server-side value, derive it on the server (e.g. via a
 * {@link CallbackAction} that recomputes from session state) rather than
 * trusting the mirror. This caveat is the reason the mirror is opt-in: the safe
 * default never trusts a client-set value.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @param <T>
 *            the runtime type of the value to assign — the DOM coerces it to a
 *            string when setting the attribute
 */
public class SetAttributeAction<T> extends Action {

    /**
     * Singleton input that yields a JS {@code null}. Used by the
     * {@link #SetAttributeAction(Component, String, Object) null-accepting
     * convenience constructor} so {@link LiteralInput} can stay non-null.
     */
    private static final Action.Input<Object> NULL_LITERAL = new Action.Input<>() {
        @Override
        protected JsFunction toJs(Trigger trigger) {
            return JsFunction.of("return null");
        }
    };

    @SuppressWarnings("unchecked")
    private static <T> Action.Input<T> nullLiteral() {
        return (Action.Input<T>) NULL_LITERAL;
    }

    private final Element target;
    private final String attributeName;
    private final Action.Input<? extends T> source;
    private final Map<StateNode, ReturnChannelRegistration> channelByNode = new IdentityHashMap<>();
    private boolean mirrorToServer;

    /**
     * Creates an action that assigns the given literal value to the given
     * attribute on {@code target} when the trigger fires. Passing {@code null}
     * removes the attribute (renders {@code target.removeAttribute(name);}).
     *
     * @param target
     *            the component whose root element to modify, not {@code null}
     * @param attributeName
     *            the attribute name, not {@code null}
     * @param value
     *            the value to assign — typically a {@code String}; any value is
     *            coerced to a string by the DOM. {@code null} removes the
     *            attribute.
     */
    public SetAttributeAction(Component target, String attributeName,
            @Nullable T value) {
        this(target, attributeName,
                value == null ? nullLiteral() : new LiteralInput<>(value));
    }

    /**
     * Creates an action that assigns the value produced by {@code source} to
     * the given attribute on {@code target} when the trigger fires. A
     * {@code null} produced by the source removes the attribute.
     *
     * @param target
     *            the component whose root element to modify, not {@code null}
     * @param attributeName
     *            the attribute name, not {@code null}
     * @param source
     *            input that produces the value to assign, not {@code null}
     */
    public SetAttributeAction(Component target, String attributeName,
            Action.Input<? extends T> source) {
        this.target = Objects.requireNonNull(target).getElement();
        this.attributeName = Objects.requireNonNull(attributeName);
        this.source = Objects.requireNonNull(source);
    }

    /**
     * Opts into the server-state mirror: when the trigger fires, the assigned
     * value (or the removal) is forwarded back to the server and stored on the
     * target's {@link ElementAttributeMap} so
     * {@link Element#getAttribute(String)} returns the same value the browser
     * sees.
     * <p>
     * See the class Javadoc for the security implication — the server stores
     * whatever the client sent, so the mirrored value is not trustworthy for
     * authorisation or validation decisions.
     * <p>
     * Must be called before this action is wired through
     * {@link Trigger#triggers(Action...)}; configuration changes after wiring
     * do not affect the already-installed client JS.
     *
     * @return this action, for chaining
     */
    public SetAttributeAction<T> mirrorToServer() {
        this.mirrorToServer = true;
        return this;
    }

    @Override
    protected JsFunction toJs(Trigger trigger) {
        if (mirrorToServer) {
            // Mirror: assign on the client and forward the assigned value back
            // through $3 so the server's ElementAttributeMap stays in sync.
            return JsFunction.of(
                    "const v = $2(event); if (v == null) $0.removeAttribute($1); else $0.setAttribute($1, v); $3(v);",
                    target, attributeName, source.toJs(trigger),
                    channelFor(trigger.getHost().getNode()))
                    .withArguments("event");
        }
        // Default: client-only. $0 = target element, $1 = attribute name
        // (string capture, Jackson-quoted on the client), $2 = source
        // JsFunction (invoked with event so handler-scoped inputs work).
        return JsFunction.of(
                "const v = $2(event); if (v == null) $0.removeAttribute($1); else $0.setAttribute($1, v);",
                target, attributeName, source.toJs(trigger))
                .withArguments("event");
    }

    private ReturnChannelRegistration channelFor(StateNode hostNode) {
        return channelByNode.computeIfAbsent(hostNode,
                node -> node.getFeature(ReturnChannelMap.class)
                        .registerChannel(this::mirrorToServer));
    }

    private void mirrorToServer(ArrayNode args) {
        JsonNode raw = args.get(0);
        // Attributes are always strings in the DOM; coerce non-null values to
        // their string form so an Input producing a number/boolean mirrors the
        // setAttribute coercion the browser already performed.
        String value = (raw == null || raw.isNull()) ? null : raw.asString();
        // emitChange=false: the attribute was already set on the client by the
        // action's JS; this update only realigns the server's view of the
        // attribute and must not echo back as another change.
        target.getNode().getFeature(ElementAttributeMap.class)
                .set(attributeName, value, false);
    }
}
