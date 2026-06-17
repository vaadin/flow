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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.shared.Registration;

/**
 * Fires when the host component's root element changes size, as reported by the
 * browser's {@code ResizeObserver} API. The bound actions run inside the
 * observer callback with a synthetic event whose {@code width} and
 * {@code height} hold the rounded pixel size of the element's content box.
 * <p>
 * The size values are exposed as static {@link Action.Input} sources on
 * {@link EventData}. The whole-event source {@link EventData#size} is shaped to
 * match the {@link Size} record so it can be deserialised directly on the
 * server when forwarded through an action that decodes its input on the server.
 * <p>
 * Example — mirror the element's pixel size into two display fields:
 *
 * <pre>{@code
 * SizeTrigger resize = new SizeTrigger(panel);
 * resize.triggers(
 *         new SetPropertyAction<>(widthSpan, "textContent",
 *                 SizeTrigger.EventData.width),
 *         new SetPropertyAction<>(heightSpan, "textContent",
 *                 SizeTrigger.EventData.height));
 * }</pre>
 *
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class SizeTrigger extends Trigger {

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
     * The resize-event properties exposed as static {@link Action.Input}
     * sources. Each field reads off the synthetic {@code {width, height}} event
     * that fires the trigger; pass any of them as the value source of an
     * {@link Action} wired to a {@link SizeTrigger}.
     * <p>
     * Each field is bound to {@link SizeTrigger}; using it in the handler of an
     * unrelated trigger throws {@link IllegalArgumentException} at
     * {@link Trigger#triggers(Action...)} time.
     */
    public abstract static class EventData implements Serializable {

        /**
         * The class exists purely as a namespace for the static
         * {@link Action.Input} fields.
         */
        protected EventData() {
        }

        /**
         * {@code event.width} — the element's rounded content-box width in
         * pixels.
         */
        public static final Action.Input<Integer> width = new HandlerInput<>(
                "width", SizeTrigger.class);

        /**
         * {@code event.height} — the element's rounded content-box height in
         * pixels.
         */
        public static final Action.Input<Integer> height = new HandlerInput<>(
                "height", SizeTrigger.class);

        /**
         * The synthetic event object as a whole, shaped as
         * {@code {width, height}} — Jackson-deserialisable into the
         * {@link Size} record when consumed by an action that decodes its input
         * on the server.
         */
        public static final Action.Input<Size> size = new Action.Input<>() {
            @Override
            public JsFunction toJs(Trigger trigger) {
                if (!(trigger instanceof SizeTrigger)) {
                    throw new IllegalArgumentException("Input is scoped to "
                            + SizeTrigger.class.getSimpleName()
                            + " and cannot be used in a "
                            + trigger.getClass().getSimpleName() + " handler");
                }
                return JsFunction.of("return event").withArguments("event");
            }
        };
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
