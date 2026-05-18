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
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.shared.Registration;

/**
 * Base class for {@link Trigger} implementations. Each call to
 * {@link #triggers(Action...)} produces one {@link Element#addJsInitializer
 * addJsInitializer} registration on the host element; {@link #remove()} removes
 * all such registrations.
 * <p>
 * Subclasses provide their event-install JS by overriding
 * {@link #installJs(JsBuilder, String)}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public abstract class AbstractTrigger implements Trigger {

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

    @Override
    public final Trigger triggers(Action... actions) {
        Objects.requireNonNull(actions);
        if (actions.length == 0) {
            throw new IllegalArgumentException(
                    "At least one action is required");
        }
        JsBuilder builder = new JsBuilder(host);
        StringBuilder handlerBody = new StringBuilder();
        for (Action action : actions) {
            Objects.requireNonNull(action, "Action must not be null");
            ((AbstractAction) action).appendStatement(builder, handlerBody);
            handlerBody.append(";");
        }
        String js = installJs(builder, handlerBody.toString());
        registrations.add(host.addJsInitializer(js, builder.params()));
        return this;
    }

    /**
     * Builds the JS expression that installs this trigger's listener and
     * returns a cleanup function that removes it. The expression runs with
     * {@code this} bound to the host element.
     * <p>
     * Subclasses must use {@code builder} (via {@link JsBuilder#reference}) to
     * reference any non-host element rather than embedding element identifiers
     * in the JS string.
     *
     * @param builder
     *            collects element parameter references, not {@code null}
     * @param handlerBody
     *            JS statements (each terminated with a semicolon) that run when
     *            the trigger fires, not {@code null}
     * @return the JS install expression, not {@code null}
     */
    protected abstract String installJs(JsBuilder builder, String handlerBody);

    @Override
    public final void remove() {
        registrations.forEach(Registration::remove);
        registrations.clear();
    }
}
