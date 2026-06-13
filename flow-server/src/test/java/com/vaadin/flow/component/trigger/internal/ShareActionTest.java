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

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.tests.util.MockUI;

import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.actionOf;
import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.singleInstallFn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ShareActionTest {

    @Test
    void fireAndForget_allSlots_emitsNavigatorShareWithEachInputFunction() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        new DomEventTrigger(button, "click").triggers(new ShareAction(
                new LiteralInput<>("Hi"), new LiteralInput<>("World"),
                new LiteralInput<>("https://vaadin.com")));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // Each slot value is produced on the client by invoking its input
        // JsFunction with the event; the literal values are captured, not
        // stringified into the body.
        JsFunction action = actionOf(singleInstallFn(ui));
        assertEquals(
                "return navigator.share({title:$0(event),text:$1(event),url:$2(event)})",
                action.getBody());
        assertEquals("Hi", ((JsFunction) action.getCaptures().get(0))
                .getCaptures().get(0));
        assertEquals("World", ((JsFunction) action.getCaptures().get(1))
                .getCaptures().get(0));
        assertEquals("https://vaadin.com",
                ((JsFunction) action.getCaptures().get(2)).getCaptures()
                        .get(0));
    }

    @Test
    void fireAndForget_onlyUrl_emitsObjectWithSingleField() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        new DomEventTrigger(button, "click").triggers(new ShareAction(null,
                null, new LiteralInput<>("https://vaadin.com")));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        JsFunction action = actionOf(singleInstallFn(ui));
        assertEquals("return navigator.share({url:$0(event)})",
                action.getBody());
        assertEquals("https://vaadin.com",
                ((JsFunction) action.getCaptures().get(0)).getCaptures()
                        .get(0));
    }

    @Test
    void withCallbacks_wrapsInnerNavigatorSharePromiseWithObserver() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        new DomEventTrigger(button, "click").triggers(
                new ShareAction(new LiteralInput<>("Hi"), null, null, () -> {
                }, err -> {
                }));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // With outcome handling the action wraps the inner promise function
        // with OBSERVE_PROMISE + the return channel; the inner $1 still calls
        // navigator.share.
        JsFunction action = actionOf(singleInstallFn(ui));
        assertEquals("$0($1(event), $2)", action.getBody());

        JsFunction inner = (JsFunction) action.getCaptures().get(1);
        assertEquals("return navigator.share({title:$0(event)})",
                inner.getBody());
    }

    @Test
    void allInputsNull_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> new ShareAction(null, null, null));
    }
}
