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
import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.tests.util.MockUI;

import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.actionOf;
import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.singleInstallFn;
import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.singleReturnChannel;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WriteToClipboardActionTest {

    private static final String HELPER_BODY = "return window.Vaadin.Flow.clipboard.writePayload($0(event), $1(event), $2(event))";

    @Test
    void fireAndForget_textOnly_callsHelperWithHtmlAndImageSlotsReturningNull() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent field = new TagComponent("input");
        ui.getElement().appendChild(button.getElement(), field.getElement());

        new DomEventTrigger(button, "click")
                .triggers(new WriteToClipboardAction(
                        new PropertyInput<>(field, "value", String.class),
                        null));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        JsFunction action = actionOf(singleInstallFn(ui));
        assertEquals(HELPER_BODY, action.getBody());

        // $0 is the text input — a PropertyInput that reads from the field.
        JsFunction text = (JsFunction) action.getCaptures().get(0);
        assertEquals("return $0[$1]", text.getBody());

        // $1 is the html slot — the no-op "return null" stand-in.
        JsFunction html = (JsFunction) action.getCaptures().get(1);
        assertEquals("return null", html.getBody());
        assertEquals(List.of(), html.getCaptures());

        // $2 is the image slot — also the no-op stand-in.
        JsFunction image = (JsFunction) action.getCaptures().get(2);
        assertEquals("return null", image.getBody());
        assertEquals(List.of(), image.getCaptures());
    }

    @Test
    void fireAndForget_textAndHtml_capturesBothInputFunctions() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        new DomEventTrigger(button, "click").triggers(
                new WriteToClipboardAction(new LiteralInput<>("plain"),
                        new LiteralInput<>("<b>html</b>")));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        JsFunction action = actionOf(singleInstallFn(ui));
        assertEquals(HELPER_BODY, action.getBody());

        JsFunction text = (JsFunction) action.getCaptures().get(0);
        assertEquals("plain", text.getCaptures().get(0));

        JsFunction html = (JsFunction) action.getCaptures().get(1);
        assertEquals("<b>html</b>", html.getCaptures().get(0));

        // image slot is the no-op stand-in.
        JsFunction image = (JsFunction) action.getCaptures().get(2);
        assertEquals("return null", image.getBody());
    }

    @Test
    void fireAndForget_htmlOnly_textAndImageSlotsReturnNull() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());

        new DomEventTrigger(button, "click")
                .triggers(new WriteToClipboardAction(null,
                        new LiteralInput<>("<b>hi</b>")));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        JsFunction action = actionOf(singleInstallFn(ui));
        assertEquals(HELPER_BODY, action.getBody());

        // $0 is the text slot — the no-op stand-in.
        JsFunction text = (JsFunction) action.getCaptures().get(0);
        assertEquals("return null", text.getBody());

        // $1 is the html literal input.
        JsFunction html = (JsFunction) action.getCaptures().get(1);
        assertEquals("<b>hi</b>", html.getCaptures().get(0));

        // $2 is the image slot — the no-op stand-in.
        JsFunction image = (JsFunction) action.getCaptures().get(2);
        assertEquals("return null", image.getBody());
    }

    @Test
    void fireAndForget_imageOnly_textAndHtmlSlotsReturnNull() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent img = new TagComponent("img");
        ui.getElement().appendChild(button.getElement(), img.getElement());

        new DomEventTrigger(button, "click")
                .triggers(new WriteToClipboardAction(new ImageBlobInput(img)));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        JsFunction action = actionOf(singleInstallFn(ui));
        assertEquals(HELPER_BODY, action.getBody());

        // $0 and $1 are the no-op stand-ins.
        assertEquals("return null",
                ((JsFunction) action.getCaptures().get(0)).getBody());
        assertEquals("return null",
                ((JsFunction) action.getCaptures().get(1)).getBody());

        // $2 is the image input — yields the source <img> element verbatim,
        // captured at $0 of its JsFunction.
        JsFunction image = (JsFunction) action.getCaptures().get(2);
        assertEquals("return $0", image.getBody());
        assertSame(img.getElement(), image.getCaptures().get(0));
    }

    @Test
    void fireAndForget_allThreeSlots_eachInputCapturedInOrder() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent img = new TagComponent("img");
        ui.getElement().appendChild(button.getElement(), img.getElement());

        new DomEventTrigger(button, "click").triggers(
                new WriteToClipboardAction(new LiteralInput<>("plain"),
                        new LiteralInput<>("<b>html</b>"),
                        new ImageBlobInput(img)));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        JsFunction action = actionOf(singleInstallFn(ui));
        assertEquals(HELPER_BODY, action.getBody());

        assertEquals("plain", ((JsFunction) action.getCaptures().get(0))
                .getCaptures().get(0));
        assertEquals("<b>html</b>", ((JsFunction) action.getCaptures().get(1))
                .getCaptures().get(0));
        assertSame(img.getElement(), ((JsFunction) action.getCaptures().get(2))
                .getCaptures().get(0));
    }

    @Test
    void withCallbacks_actionFnWrapsInnerWithObserverAndChannel() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent field = new TagComponent("input");
        ui.getElement().appendChild(button.getElement(), field.getElement());

        new DomEventTrigger(button, "click")
                .triggers(new WriteToClipboardAction(
                        new PropertyInput<>(field, "value", String.class), null,
                        copied -> {
                        }, err -> {
                        }));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // With outcome handling, the action wraps the inner promise function
        // with OBSERVE_PROMISE + the return channel. The inner function still
        // calls writePayload — the action class itself does no string
        // assembly beyond the static body constant.
        JsFunction action = actionOf(singleInstallFn(ui));
        assertEquals("$0($1(event), $2)", action.getBody());

        JsFunction inner = (JsFunction) action.getCaptures().get(1);
        assertEquals(HELPER_BODY, inner.getBody());
    }

    @Test
    void withCallbacks_imageOnly_wrapsInnerWithObserverAndChannel() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent img = new TagComponent("img");
        ui.getElement().appendChild(button.getElement(), img.getElement());

        new DomEventTrigger(button, "click").triggers(
                new WriteToClipboardAction(new ImageBlobInput(img), copied -> {
                }, err -> {
                }));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // The dedicated image observed constructor produces the same outer
        // shape as the text/html one — only the inner $2 (image) slot differs.
        JsFunction action = actionOf(singleInstallFn(ui));
        assertEquals("$0($1(event), $2)", action.getBody());

        JsFunction inner = (JsFunction) action.getCaptures().get(1);
        assertEquals(HELPER_BODY, inner.getBody());
        JsFunction image = (JsFunction) inner.getCaptures().get(2);
        assertSame(img.getElement(), image.getCaptures().get(0));
    }

    @Test
    void onCopied_receivesTheStringFromTheResolvedPromise() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent field = new TagComponent("input");
        ui.getElement().appendChild(button.getElement(), field.getElement());

        List<@Nullable String> copied = new ArrayList<>();
        new DomEventTrigger(button, "click")
                .triggers(new WriteToClipboardAction(
                        new PropertyInput<>(field, "value", String.class), null,
                        copied::add, err -> {
                        }));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        ObjectNode outcome = JacksonUtils.createObjectNode();
        outcome.put("ok", true);
        outcome.put("value", "hello clipboard");
        ArrayNode args = JacksonUtils.createArrayNode();
        args.add(outcome);
        singleReturnChannel(ui).invoke(args);

        assertEquals(List.of("hello clipboard"), copied);
    }

    @Test
    void onCopied_receivesNullWhenJsResolvedWithoutValue() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent field = new TagComponent("input");
        ui.getElement().appendChild(button.getElement(), field.getElement());

        List<@Nullable String> copied = new ArrayList<>();
        new DomEventTrigger(button, "click")
                .triggers(new WriteToClipboardAction(
                        new PropertyInput<>(field, "value", String.class), null,
                        copied::add, err -> {
                        }));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        ObjectNode outcome = JacksonUtils.createObjectNode();
        outcome.put("ok", true);
        // No "value" field — JS resolved with undefined. The typed Consumer
        // gets null.
        ArrayNode args = JacksonUtils.createArrayNode();
        args.add(outcome);
        singleReturnChannel(ui).invoke(args);

        assertEquals(Arrays.asList((String) null), copied);
    }

    @Test
    void constructor_textHtml_bothNullRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WriteToClipboardAction(null, null));
    }

    @Test
    void constructor_multiFormat_allInputsNullRejected() {
        Action.@Nullable Input<String> nullText = null;
        Action.@Nullable Input<String> nullHtml = null;
        Action.@Nullable Input<?> nullImage = null;
        assertThrows(IllegalArgumentException.class,
                () -> new WriteToClipboardAction(nullText, nullHtml,
                        nullImage));
    }

}
