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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.shared.Registration;

/**
 * Something that fires on the client when some condition is met — a DOM event,
 * a signal change, an observer firing, a timer elapsing, an idle timeout, a
 * {@code BroadcastChannel} message, a media-query match, … — and, when it does,
 * runs one or more {@link AbstractAction actions}.
 * <p>
 * Each call to {@link #triggers(AbstractAction...)} produces one
 * {@link Element#addJsInitializer addJsInitializer} registration on the host
 * element; {@link #remove()} removes all such registrations. Subclasses provide
 * the JS that installs and tears down the listener by overriding
 * {@link #installJs()}; the action handler is exposed to that JS as a
 * {@link JsFunction} captured at {@code $0}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public abstract class AbstractTrigger implements Serializable {

    private final Element host;
    private final List<Registration> registrations = new ArrayList<>();

    /**
     * Creates a new trigger bound to the given host component's root element.
     *
     * @param host
     *            the component whose root element the trigger fires on, not
     *            {@code null}
     */
    protected AbstractTrigger(Component host) {
        this.host = Objects.requireNonNull(host).getElement();
    }

    /**
     * The host element this trigger fires on.
     *
     * @return the host element, never {@code null}
     */
    public final Element getHost() {
        return host;
    }

    /**
     * Wires the given actions to this trigger. They run in the order given the
     * next time this trigger fires. Each call adds another wiring; the existing
     * ones are kept.
     *
     * @param actions
     *            the actions to run, not {@code null} or empty
     * @return this trigger, for chaining
     */
    public final AbstractTrigger triggers(AbstractAction... actions) {
        Objects.requireNonNull(actions);
        if (actions.length == 0) {
            throw new IllegalArgumentException(
                    "At least one action is required");
        }
        JsBuilder builder = new JsBuilder(this);
        StringBuilder handlerBody = new StringBuilder();
        for (AbstractAction action : actions) {
            Objects.requireNonNull(action, "Action must not be null");
            action.appendStatement(builder, handlerBody);
            handlerBody.append(";");
        }
        JsFunction handler = JsFunction
                .of(handlerBody.toString(), builder.captures())
                .withArguments("event");
        registrations.add(host.addJsInitializer(installJs(), handler));
        return this;
    }

    /**
     * Builds the JS expression that installs this trigger's listener and
     * returns a cleanup function that removes it.
     * <p>
     * The expression runs with {@code this} bound to the host element. The
     * handler {@link JsFunction} is available as {@code $0}; subclasses pass it
     * to whatever client API the trigger wraps (e.g.
     * {@code this.addEventListener(name, $0)}) and reference the same
     * {@code $0} in the cleanup callback to detach it.
     *
     * @return the JS install expression, not {@code null}
     */
    protected abstract String installJs();

    /**
     * Removes this trigger and all wirings created from it. The corresponding
     * client-side listeners are detached as part of the next synchronisation.
     */
    public final void remove() {
        registrations.forEach(Registration::remove);
        registrations.clear();
    }
}
