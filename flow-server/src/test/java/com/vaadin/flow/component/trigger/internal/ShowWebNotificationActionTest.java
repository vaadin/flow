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
import com.vaadin.flow.component.webnotification.WebNotificationOptions;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ReturnChannelMap;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;
import com.vaadin.tests.util.MockUI;

import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.actionOf;
import static com.vaadin.flow.component.trigger.internal.TriggerTestUtil.installFns;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ShowWebNotificationActionTest {

    @Test
    void sharedOnSameHost_removingOneTriggerKeepsReportingToTheOther() {
        UI ui = new MockUI();
        TagComponent button = new TagComponent("button");
        ui.getElement().appendChild(button.getElement());
        List<String> clicks = new ArrayList<>();

        ShowWebNotificationAction show = new ShowWebNotificationAction(
                new LiteralInput<>("Hi"),
                WebNotificationOptions.builder().build(),
                () -> clicks.add("click"), error -> {
                });
        DomEventTrigger click = new DomEventTrigger(button, "click");
        click.triggers(show);
        DomEventTrigger keydown = new DomEventTrigger(button, "keydown");
        keydown.triggers(show);
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        List<ReturnChannelRegistration> channels = installFns(ui).stream()
                .map(fn -> (ReturnChannelRegistration) actionOf(fn)
                        .getCaptures().get(2))
                .toList();
        assertEquals(2, channels.size());
        assertSame(channels.get(0), channels.get(1),
                "both triggers report through one channel per host");
        ReturnChannelRegistration channel = channels.get(0);

        click.remove();

        assertNotNull(
                button.getElement().getNode().getFeature(ReturnChannelMap.class)
                        .get(channel.getChannelId()),
                "the remaining trigger still reports through the channel");
        ArrayNode args = JacksonUtils.createArrayNode();
        args.add("click");
        channel.invoke(args);
        assertEquals(List.of("click"), clicks);
    }
}
