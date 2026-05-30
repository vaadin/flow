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

import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ArrayNode;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.change.MapPutChange;
import com.vaadin.flow.internal.change.MapRemoveChange;
import com.vaadin.flow.internal.change.NodeChange;
import com.vaadin.flow.internal.nodefeature.ElementAttributeMap;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;
import com.vaadin.tests.util.MockUI;

import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.actionOf;
import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.singleInstallFn;
import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.singleReturnChannel;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SetAttributeActionTest {

    @Test
    void defaultIsClientOnly_noMirrorChannel() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent panel = new TagComponent("div");
        ui.getElement().appendChild(button.getElement(), panel.getElement());

        new DomEventTrigger(button, "click").triggers(
                new SetAttributeAction<>(panel, "aria-hidden", "true"));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        JsFunction action = actionOf(singleInstallFn(ui));
        // Security-first default: no mirror channel, no extra capture, body
        // is the client-side conditional set/remove.
        assertEquals(
                "const v = $2(event); if (v == null) $0.removeAttribute($1); else $0.setAttribute($1, v);",
                action.getBody());
        assertEquals(3, action.getCaptures().size());
        assertSame(panel.getElement(), action.getCaptures().get(0));
        assertEquals("aria-hidden", action.getCaptures().get(1));
        assertTrue(
                action.getCaptures().stream()
                        .noneMatch(c -> c instanceof ReturnChannelRegistration),
                "default SetAttributeAction must not register a return channel");
    }

    @Test
    void defaultClientOnly_leavesServerAttributeUntouched() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent panel = new TagComponent("div");
        ui.getElement().appendChild(button.getElement(), panel.getElement());

        panel.getElement().setAttribute("aria-hidden", "false");
        new DomEventTrigger(button, "click").triggers(
                new SetAttributeAction<>(panel, "aria-hidden", "true"));

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // No channel registered: nothing the client can send would touch the
        // server's view of the attribute.
        assertEquals("false", panel.getElement().getAttribute("aria-hidden"));
    }

    @Test
    void mirrorToServer_addsChannelAndExtendsBody() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent panel = new TagComponent("div");
        ui.getElement().appendChild(button.getElement(), panel.getElement());

        new DomEventTrigger(button, "click")
                .triggers(new SetAttributeAction<>(panel, "aria-hidden", "true")
                        .mirrorToServer());

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        JsFunction action = actionOf(singleInstallFn(ui));
        // $0 = target, $1 = name, $2 = source JsFunction, $3 = mirror channel.
        assertEquals(
                "const v = $2(event); if (v == null) $0.removeAttribute($1); else $0.setAttribute($1, v); $3(v);",
                action.getBody());
        assertEquals(4, action.getCaptures().size());
        assertTrue(
                action.getCaptures()
                        .get(3) instanceof ReturnChannelRegistration,
                "Expected $3 to be the server-mirror return channel");
    }

    @Test
    void mirrorToServer_channelInvocation_updatesTargetServerAttribute() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent panel = new TagComponent("div");
        ui.getElement().appendChild(button.getElement(), panel.getElement());

        new DomEventTrigger(button, "click").triggers(
                new SetAttributeAction<>(panel, "aria-hidden", "initial")
                        .mirrorToServer());

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        // Simulate the trigger firing on the client and the post-assignment
        // value arriving through the channel.
        ArrayNode args = JacksonUtils.createArrayNode();
        args.add("from-client");
        singleReturnChannel(ui).invoke(args);

        assertEquals("from-client",
                panel.getElement().getAttribute("aria-hidden"));
    }

    @Test
    void mirrorToServer_channelInvocation_doesNotEchoBackToClient() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent panel = new TagComponent("div");
        ui.getElement().appendChild(button.getElement(), panel.getElement());

        new DomEventTrigger(button, "click")
                .triggers(new SetAttributeAction<>(panel, "aria-hidden", "true")
                        .mirrorToServer());

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        ReturnChannelRegistration channel = singleReturnChannel(ui);
        // Drain pending JS and changes so the next collect starts clean.
        ui.getInternals().dumpPendingJavaScriptInvocations();
        ui.getInternals().getStateTree().collectChanges(c -> {
        });

        ArrayNode args = JacksonUtils.createArrayNode();
        args.add("client-typed");
        channel.invoke(args);

        // The mirror updates the server's ElementAttributeMap silently —
        // collectChanges must not produce a MapPut for "aria-hidden" because
        // that would re-send the value the client just set, causing an echo.
        List<NodeChange> changes = new ArrayList<>();
        ui.getInternals().getStateTree().collectChanges(changes::add);
        long attrChanges = changes.stream()
                .filter(c -> c instanceof MapPutChange)
                .map(c -> (MapPutChange) c)
                .filter(c -> c.getFeature().equals(ElementAttributeMap.class))
                .filter(c -> "aria-hidden".equals(c.getKey())).count();
        assertEquals(0, attrChanges,
                "Server-mirror update must not produce a client-bound change");
    }

    @Test
    void mirrorToServer_channelInvocation_nullValue_removesServerAttribute() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent panel = new TagComponent("div");
        ui.getElement().appendChild(button.getElement(), panel.getElement());

        panel.getElement().setAttribute("aria-hidden", "true");
        new DomEventTrigger(button, "click").triggers(
                new SetAttributeAction<>(panel, "aria-hidden", (String) null)
                        .mirrorToServer());

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        ArrayNode args = JacksonUtils.createArrayNode();
        args.add(JacksonUtils.nullNode());
        singleReturnChannel(ui).invoke(args);

        assertFalse(panel.getElement().hasAttribute("aria-hidden"),
                "null mirror should remove the attribute server-side");
        assertNull(panel.getElement().getAttribute("aria-hidden"));
    }

    @Test
    void mirrorToServer_channelInvocation_nullValue_doesNotEchoRemoveToClient() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        TagComponent panel = new TagComponent("div");
        ui.getElement().appendChild(button.getElement(), panel.getElement());

        panel.getElement().setAttribute("aria-hidden", "true");
        new DomEventTrigger(button, "click").triggers(
                new SetAttributeAction<>(panel, "aria-hidden", (String) null)
                        .mirrorToServer());

        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        ReturnChannelRegistration channel = singleReturnChannel(ui);
        ui.getInternals().dumpPendingJavaScriptInvocations();
        ui.getInternals().getStateTree().collectChanges(c -> {
        });

        ArrayNode args = JacksonUtils.createArrayNode();
        args.add(JacksonUtils.nullNode());
        channel.invoke(args);

        List<NodeChange> changes = new ArrayList<>();
        ui.getInternals().getStateTree().collectChanges(changes::add);
        long attrRemoves = changes.stream()
                .filter(c -> c instanceof MapRemoveChange)
                .map(c -> (MapRemoveChange) c)
                .filter(c -> c.getFeature().equals(ElementAttributeMap.class))
                .filter(c -> "aria-hidden".equals(c.getKey())).count();
        assertEquals(0, attrRemoves,
                "Server-mirror remove must not produce a client-bound change");
    }
}
