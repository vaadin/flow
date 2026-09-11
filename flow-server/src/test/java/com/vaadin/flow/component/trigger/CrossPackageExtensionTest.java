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

import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.trigger.internal.Action;
import com.vaadin.flow.component.trigger.internal.ClickTrigger;
import com.vaadin.flow.component.trigger.internal.DomEventTrigger;
import com.vaadin.flow.component.trigger.internal.HandlerInput;
import com.vaadin.flow.component.trigger.internal.MouseEventTrigger;
import com.vaadin.flow.component.trigger.internal.Trigger;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies that the trigger framework can be extended from outside its own
 * package: a custom {@link Action} can consume any {@link Action.Input} it
 * receives as a parameter (via the public {@link Action.Input#toJs(Trigger)}),
 * and a custom trigger family can reuse {@link HandlerInput} for its
 * handler-scoped event properties instead of subclassing {@code Action.Input}
 * by hand.
 */
class CrossPackageExtensionTest {

    private static class Div extends Component {
        Div() {
            super(new Element("div"));
        }
    }

    /**
     * A custom action that takes an arbitrary input and embeds it in its own JS
     * — the shape a component author outside the internal package writes.
     */
    private static class LogAction extends Action {

        private final Action.Input<?> message;

        LogAction(Action.Input<?> message) {
            this.message = message;
        }

        @Override
        protected JsFunction toJs(Trigger trigger) {
            return JsFunction
                    .of("console.log($0(event))", message.toJs(trigger))
                    .withArguments("event");
        }

        JsFunction render(Trigger trigger) {
            return toJs(trigger);
        }
    }

    /**
     * A custom trigger family exposing its handler event state through
     * {@link HandlerInput}, the same way the built-in families do.
     */
    private static class InputEventTrigger extends DomEventTrigger {

        static final Action.Input<String> data = new HandlerInput<>("data",
                InputEventTrigger.class);

        InputEventTrigger(Component host) {
            super(host, "input");
        }
    }

    @Test
    void customAction_consumesInputItDidNotCreate() {
        ClickTrigger click = new ClickTrigger(new Div());
        LogAction action = new LogAction(MouseEventTrigger.EventData.screenX);

        JsFunction rendered = action.render(click);

        assertEquals("console.log($0(event))", rendered.getBody());
        JsFunction source = (JsFunction) rendered.getCaptures().get(0);
        assertEquals("return event[$0]", source.getBody());
        assertEquals("screenX", source.getCaptures().get(0));
    }

    @Test
    void handlerInput_reusableByCustomTriggerFamily() {
        JsFunction rendered = InputEventTrigger.data
                .toJs(new InputEventTrigger(new Div()));

        assertEquals(List.of("event"), rendered.getArgumentNames());
        assertEquals("return event[$0]", rendered.getBody());
        assertEquals("data", rendered.getCaptures().get(0));
    }

    @Test
    void handlerInput_keepsOwnerScopeCheck() {
        // Class-based scoping must survive the widened visibility: an input
        // declared for InputEventTrigger is still rejected by an unrelated
        // trigger's handler.
        ClickTrigger click = new ClickTrigger(new Div());
        assertThrows(IllegalArgumentException.class,
                () -> InputEventTrigger.data.toJs(click));
    }
}
