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

import com.vaadin.flow.component.Size;
import com.vaadin.flow.component.trigger.Action;
import com.vaadin.flow.component.trigger.SizeTrigger;
import com.vaadin.flow.component.trigger.Trigger;
import com.vaadin.flow.dom.JsFunction;

/**
 * The resize-event properties of a {@link SizeTrigger} exposed as static
 * {@link ActionInput} sources. Each field reads off the synthetic
 * {@code {width, height}} event that fires the trigger; pass any of them as the
 * value source of an {@link Action} wired to a {@link SizeTrigger}.
 * <p>
 * Each field is bound to {@link SizeTrigger}; using it in the handler of an
 * unrelated trigger throws {@link IllegalArgumentException} at
 * {@link Trigger#triggers(Action...)} time.
 * <p>
 * For internal use only. May be renamed or removed in any release.
 *
 * @since 25.4
 */
public final class SizeEventData {

    private SizeEventData() {
    }

    /**
     * {@code event.width} — the element's rounded content-box width in pixels.
     */
    public static final ActionInput<Integer> width = new HandlerInput<>("width",
            SizeTrigger.class);

    /**
     * {@code event.height} — the element's rounded content-box height in
     * pixels.
     */
    public static final ActionInput<Integer> height = new HandlerInput<>(
            "height", SizeTrigger.class);

    /**
     * The synthetic event object as a whole, shaped as {@code {width, height}}
     * — Jackson-deserialisable into the {@link Size} record when consumed by an
     * action that decodes its input on the server.
     */
    public static final ActionInput<Size> size = new ActionInput<>() {
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
