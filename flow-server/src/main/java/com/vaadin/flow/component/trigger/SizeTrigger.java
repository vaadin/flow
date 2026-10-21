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
package com.vaadin.flow.component.trigger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.shared.Registration;

/**
 * Fires when the host component's root element changes size, as reported by the
 * browser's {@code ResizeObserver} API. The bound actions run inside the
 * observer callback.
 * <p>
 * Example — run a server-side callback whenever a panel is resized:
 *
 * <pre>{@code
 * new SizeTrigger(panel).triggers(new CallbackAction<>(() -> relayout()));
 * }</pre>
 *
 * @since 25.4
 */
public final class SizeTrigger extends Trigger {

    /**
     * Creates a size trigger on the given host component's root element.
     *
     * @param host
     *            the component whose root element is observed, not {@code null}
     */
    public SizeTrigger(Component host) {
        super(host);
    }

    /**
     * Creates a size trigger on the given element.
     *
     * @param host
     *            the element that is observed, not {@code null}
     */
    public SizeTrigger(Element host) {
        super(host);
    }

    @Override
    protected Registration install(JsFunction action) {
        // ElementResize.observe() returns a disconnect function — exactly the
        // cleanup shape addJsInitializer expects, so we just propagate it.
        // The callback receives a {width, height} object that the action
        // ($0) treats as its `event` argument.
        return getHost().addJsInitializer(
                "return window.Vaadin.Flow.elementResize.observe(this, $0);",
                action);
    }
}
