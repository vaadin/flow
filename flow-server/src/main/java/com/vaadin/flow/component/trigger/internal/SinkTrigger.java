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

import java.util.Objects;

import com.vaadin.flow.component.trigger.ClientActionSink;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.shared.Registration;

/**
 * A trigger that installs no client-side listener of its own: it hands the
 * rendered action function to a {@link ClientActionSink} and lets the sink
 * decide when it runs.
 * <p>
 * Used where the thing that fires is rendered on the client and has no
 * server-side component to listen on — a {@code LitRenderer} template binding
 * {@code @click=${copy}}, for example. The sink passes the function into the
 * renderer, the template calls it from its own event binding, and the action
 * therefore runs inside the browser's event handler with the user gesture still
 * valid, exactly as it does for a {@link DomEventTrigger}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class SinkTrigger extends Trigger {

    private final ClientActionSink sink;

    /**
     * Creates a trigger that hands its rendered actions to {@code sink}.
     *
     * @param host
     *            the element whose lifecycle the trigger belongs to (the
     *            renderer's container), not {@code null}
     * @param sink
     *            receives the rendered action function, not {@code null}
     */
    public SinkTrigger(Element host, ClientActionSink sink) {
        super(host);
        this.sink = Objects.requireNonNull(sink, "sink must not be null");
    }

    @Override
    public boolean suppliesContext() {
        // The renderer knows which item each rendered element belongs to and
        // passes it to the action as the context argument.
        return true;
    }

    @Override
    protected Registration install(JsFunction action) {
        return Objects.requireNonNull(sink.install(action),
                "ClientActionSink.install must return a Registration");
    }
}
