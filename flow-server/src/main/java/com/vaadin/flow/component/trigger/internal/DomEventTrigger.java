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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.shared.Registration;

/**
 * Fires when the host component receives a DOM event with the given name. The
 * bound actions run inside the browser's event handler, preserving the
 * user-gesture context (so downstream actions may invoke APIs gated on a
 * gesture, such as clipboard or fullscreen).
 * <p>
 * Examples:
 *
 * <pre>{@code
 * new DomEventTrigger(button, "click").triggers(action);
 * new DomEventTrigger(input, "input").triggers(action);
 * new DomEventTrigger(panel, "pointerdown").triggers(action);
 * }</pre>
 *
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class DomEventTrigger extends Trigger {

    private final String eventName;
    private boolean preventDefault;
    private boolean stopPropagation;

    /**
     * Creates a trigger that fires when the host receives a DOM event with the
     * given name.
     *
     * @param host
     *            the component whose root element listens for the event, not
     *            {@code null}
     * @param eventName
     *            the DOM event name (e.g. {@code "click"}, {@code "input"}),
     *            not {@code null}
     */
    public DomEventTrigger(Component host, String eventName) {
        super(host);
        this.eventName = Objects.requireNonNull(eventName);
    }

    /**
     * Configures the trigger to call {@code event.preventDefault()} when it
     * fires, suppressing the browser's default action (e.g. submitting a form
     * on Enter, scrolling on Space). Affects {@link #triggers(Action...)} calls
     * made after this method; existing wirings are not retroactively changed.
     *
     * @return this trigger, for chaining
     */
    public DomEventTrigger preventDefault() {
        this.preventDefault = true;
        return this;
    }

    /**
     * Configures the trigger to call {@code event.stopPropagation()} when it
     * fires, preventing the event from bubbling to ancestor listeners. Affects
     * {@link #triggers(Action...)} calls made after this method; existing
     * wirings are not retroactively changed.
     *
     * @return this trigger, for chaining
     */
    public DomEventTrigger stopPropagation() {
        this.stopPropagation = true;
        return this;
    }

    /**
     * Returns an {@link Action.Input} that yields {@code event[name]} at fire
     * time, valid in the handler of any trigger that is an instance of
     * {@code ownerClass}. Used by trigger families that expose their event
     * properties as {@code public static final} fields — see
     * {@link MouseEventTrigger.EventData}.
     *
     * @param name
     *            the event property name, not {@code null}
     * @param ownerClass
     *            the trigger class the expression is valid for, not
     *            {@code null}
     * @param <T>
     *            the runtime type of the value produced
     * @return an input that resolves to {@code event[name]} on fire
     */
    static <T> Action.Input<T> eventProperty(String name,
            Class<? extends DomEventTrigger> ownerClass) {
        return new HandlerInput<>(name, ownerClass);
    }

    @Override
    protected final Registration install(JsFunction action) {
        // Action at $0 (the convention the framework documents in
        // Trigger#install), event name at $1 — both captures of the install
        // JsFunction, no string concatenation around either. Subclasses
        // contribute to the wrapper body and extra captures via
        // appendHandlerBody; when the body is non-empty the action is wrapped
        // in a const h so the same reference is passed to
        // add/removeEventListener.
        StringBuilder body = new StringBuilder();
        List<Object> extraCaptures = new ArrayList<>();
        appendHandlerBody(body, extraCaptures);
        StringBuilder prelude = new StringBuilder();
        appendInstallPrelude(prelude);
        if (body.length() == 0 && prelude.length() == 0) {
            return getHost().addJsInitializer("""
                    this.addEventListener($1, $0);\
                    return () => this.removeEventListener($1, $0);""", action,
                    eventName);
        }
        Object[] params = new Object[2 + extraCaptures.size()];
        params[0] = action;
        params[1] = eventName;
        for (int i = 0; i < extraCaptures.size(); i++) {
            params[2 + i] = extraCaptures.get(i);
        }
        return getHost().addJsInitializer(
                prelude + "const h=e=>{" + body
                        + "$0(e);};this.addEventListener($1, h);"
                        + "return () => this.removeEventListener($1, h);",
                params);
    }

    /**
     * Hook for subclasses to contribute statements to the wrapper handler body
     * and extra captures referenced by those statements. The base
     * implementation appends {@code e.preventDefault();} and
     * {@code e.stopPropagation();} when the corresponding flags are set.
     * <p>
     * Subclasses overriding this method typically prepend their own guard
     * statements (and call {@code super.appendHandlerBody} afterwards) so the
     * guard runs before {@code preventDefault} — preventing the default action
     * on events the guard would have rejected would be wrong. Each entry added
     * to {@code extraCaptures} is appended after the event name capture, so the
     * first extra is referenced as {@code $2} in the body, the next as
     * {@code $3}, and so on.
     *
     * @param body
     *            the wrapper body so far; statements appended here run inside
     *            the {@code const h = e => { ... $0(e); }} wrapper, in order,
     *            with the event available as {@code e}
     * @param extraCaptures
     *            mutable list of captures appended after the event-name capture
     *            ({@code $1}); the first capture added is {@code $2}, etc.
     */
    protected void appendHandlerBody(StringBuilder body,
            List<Object> extraCaptures) {
        if (preventDefault) {
            body.append("e.preventDefault();");
        }
        if (stopPropagation) {
            body.append("e.stopPropagation();");
        }
    }

    /**
     * Hook for subclasses to inject JavaScript that runs once per install,
     * before the {@code const h = e => {...}} wrapper is created. Use this to
     * declare closure variables (e.g. {@code let i = 0;} for sequence tracking)
     * that the wrapper body then captures by lexical scope.
     * <p>
     * The prelude has access to the same captures as the rest of the install JS
     * — {@code $0} is the action, {@code $1} is the event name, and any extras
     * added via {@link #appendHandlerBody} appear at {@code $2}+. The base
     * implementation is empty.
     *
     * @param prelude
     *            the prelude buffer; statements appended here run before the
     *            wrapper is created and remain in scope inside it
     */
    protected void appendInstallPrelude(StringBuilder prelude) {
        // No prelude needed by default.
    }
}
