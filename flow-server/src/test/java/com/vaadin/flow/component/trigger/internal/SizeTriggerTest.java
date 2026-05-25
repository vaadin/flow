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

import java.util.List;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.tests.util.MockUI;

import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.actionOf;
import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.installFns;
import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.singleInstallFn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SizeTriggerTest {

    @Test
    void install_delegatesToElementResizeHelperWithActionAsHandler() {
        UI ui = new MockUI();
        TagComponent panel = new TagComponent("div");
        TagComponent field = new TagComponent("input");
        ui.getElement().appendChild(panel.getElement(), field.getElement());

        SizeTrigger resize = new SizeTrigger(panel);
        resize.triggers(
                new SetPropertyAction<>(field, "value", resize.width()));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // Install JS hands the action ($0) to the per-UI ResizeObserver
        // helper and returns its disconnect function, so the host's
        // addJsInitializer cleanup pipeline detaches the observer on
        // remove. No user content leaks into the body — the action is a
        // capture.
        JsFunction install = singleInstallFn(ui);
        assertEquals(
                "return window.Vaadin.Flow.elementResize.observe(this, $0);",
                install.getBody());
    }

    @Test
    void widthAndHeight_renderAsHandlerEventProperties() {
        UI ui = new MockUI();
        TagComponent panel = new TagComponent("div");
        TagComponent xField = new TagComponent("input");
        TagComponent yField = new TagComponent("input");
        ui.getElement().appendChild(panel.getElement(), xField.getElement(),
                yField.getElement());

        SizeTrigger resize = new SizeTrigger(panel);
        resize.triggers(
                new SetPropertyAction<>(xField, "value", resize.width()),
                new SetPropertyAction<>(yField, "value", resize.height()));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // Each action's source input ($2) is a HandlerInput JsFunction
        // taking `event` and capturing the property name; the body itself
        // is the constant `return event[$0]`.
        List<JsFunction> installs = installFns(ui);
        JsFunction widthSource = (JsFunction) actionOf(installs.get(0))
                .getCaptures().get(2);
        assertEquals(List.of("event"), widthSource.getArgumentNames());
        assertEquals("return event[$0]", widthSource.getBody());
        assertEquals("width", widthSource.getCaptures().get(0));

        JsFunction heightSource = (JsFunction) actionOf(installs.get(1))
                .getCaptures().get(2);
        assertEquals("return event[$0]", heightSource.getBody());
        assertEquals("height", heightSource.getCaptures().get(0));
    }

    @Test
    void size_rendersAsBareEventReference() {
        UI ui = new MockUI();
        TagComponent panel = new TagComponent("div");
        TagComponent field = new TagComponent("input");
        ui.getElement().appendChild(panel.getElement(), field.getElement());

        SizeTrigger resize = new SizeTrigger(panel);
        // size() produces the synthetic event object as a whole — its
        // source-input function returns `event` directly, with no property
        // capture.
        resize.triggers(
                new SetPropertyAction<>(field, "data-size", resize.size()));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        JsFunction sizeSource = (JsFunction) actionOf(singleInstallFn(ui))
                .getCaptures().get(2);
        assertEquals(List.of("event"), sizeSource.getArgumentNames());
        assertEquals("return event", sizeSource.getBody());
        assertEquals(List.of(), sizeSource.getCaptures());
    }

    @Test
    void inputFromOtherTrigger_isRejected() {
        TagComponent panel1 = new TagComponent("div");
        TagComponent panel2 = new TagComponent("div");
        TagComponent field = new TagComponent("input");

        SizeTrigger resize1 = new SizeTrigger(panel1);
        SizeTrigger resize2 = new SizeTrigger(panel2);

        assertThrows(IllegalArgumentException.class, () -> resize2.triggers(
                new SetPropertyAction<>(field, "value", resize1.width())));
    }

    @Test
    void sizeFromOtherTrigger_isRejected() {
        TagComponent panel1 = new TagComponent("div");
        TagComponent panel2 = new TagComponent("div");
        TagComponent field = new TagComponent("input");

        SizeTrigger resize1 = new SizeTrigger(panel1);
        SizeTrigger resize2 = new SizeTrigger(panel2);

        assertThrows(IllegalArgumentException.class, () -> resize2.triggers(
                new SetPropertyAction<>(field, "data-size", resize1.size())));
    }
}
