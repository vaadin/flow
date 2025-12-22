/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.hotswap;

import java.util.Optional;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.tests.util.MockUI;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class HotswapEventTest {

    private final VaadinService service = new MockVaadinServletService(false);

    @Test
    public void merge_reloadThenRefresh_keepsReload() {
        HotswapClassEvent event1 = new HotswapClassEvent(service, Set.of(),
                true);
        event1.triggerUpdate(UIUpdateStrategy.RELOAD);

        HotswapClassEvent event2 = new HotswapClassEvent(service, Set.of(),
                true);
        event2.triggerUpdate(UIUpdateStrategy.REFRESH);

        event1.merge(event2);
        Assert.assertTrue(event1.requiresPageReload());
    }

    @Test
    public void merge_refreshThenReload_keepsReload() {
        HotswapClassEvent event1 = new HotswapClassEvent(service, Set.of(),
                true);
        event1.triggerUpdate(UIUpdateStrategy.REFRESH);

        HotswapClassEvent event2 = new HotswapClassEvent(service, Set.of(),
                true);
        event2.triggerUpdate(UIUpdateStrategy.RELOAD);

        event1.merge(event2);
        Assert.assertTrue(event1.requiresPageReload());
    }

    @Test
    public void merge_unsetThenUIRefresh_keepsRefresh() {
        UI ui = new MockUI();
        HotswapClassEvent event1 = new HotswapClassEvent(service, Set.of(),
                true);

        HotswapClassEvent event2 = new HotswapClassEvent(service, Set.of(),
                true);
        event2.triggerUpdate(ui, UIUpdateStrategy.REFRESH);

        event1.merge(event2);
        Optional<UIUpdateStrategy> strategy = event1.getUIUpdateStrategy(ui);
        Assert.assertTrue(strategy.isPresent());
        Assert.assertEquals(UIUpdateStrategy.REFRESH, strategy.get());
    }

    @Test
    public void merge_uiRefreshThenUnset_keepsRefresh() {
        UI ui = new MockUI();
        HotswapClassEvent event1 = new HotswapClassEvent(service, Set.of(),
                true);
        event1.triggerUpdate(ui, UIUpdateStrategy.REFRESH);

        HotswapClassEvent event2 = new HotswapClassEvent(service, Set.of(),
                true);

        event1.merge(event2);
        Optional<UIUpdateStrategy> strategy = event1.getUIUpdateStrategy(ui);
        Assert.assertTrue(strategy.isPresent());
        Assert.assertEquals(UIUpdateStrategy.REFRESH, strategy.get());
    }

    @Test
    public void merge_uiReloadThenUIRefresh_keepsReload() {
        UI ui = new MockUI();
        HotswapClassEvent event1 = new HotswapClassEvent(service, Set.of(),
                true);
        event1.triggerUpdate(ui, UIUpdateStrategy.RELOAD);

        HotswapClassEvent event2 = new HotswapClassEvent(service, Set.of(),
                true);
        event2.triggerUpdate(ui, UIUpdateStrategy.REFRESH);

        event1.merge(event2);
        Optional<UIUpdateStrategy> strategy = event1.getUIUpdateStrategy(ui);
        Assert.assertTrue(strategy.isPresent());
        Assert.assertEquals(UIUpdateStrategy.RELOAD, strategy.get());
    }

    @Test
    public void merge_uiRefreshThenUiReload_keepsReload() {
        UI ui = new MockUI();
        HotswapClassEvent event1 = new HotswapClassEvent(service, Set.of(),
                true);
        event1.triggerUpdate(ui, UIUpdateStrategy.REFRESH);

        HotswapClassEvent event2 = new HotswapClassEvent(service, Set.of(),
                true);
        event2.triggerUpdate(ui, UIUpdateStrategy.RELOAD);

        event1.merge(event2);
        Optional<UIUpdateStrategy> strategy = event1.getUIUpdateStrategy(ui);
        Assert.assertTrue(strategy.isPresent());
        Assert.assertEquals(UIUpdateStrategy.RELOAD, strategy.get());
    }

    @Test
    public void merge_updateCommands_keepAll() {
        BrowserLiveReload liveReload = mock(BrowserLiveReload.class);

        HotswapClassEvent event1 = new HotswapClassEvent(service, Set.of(),
                true);
        event1.updateClientResource("R1", "CONTENT1");
        event1.sendHmrEvent("E1",
                JacksonUtils.createObjectNode().put("key", "PAYLOAD1"));
        event1.updateClientResource("R2", "CONTENT2");
        event1.sendHmrEvent("E2",
                JacksonUtils.createObjectNode().put("key", "PAYLOAD2"));

        HotswapClassEvent event2 = new HotswapClassEvent(service, Set.of(),
                true);
        event1.updateClientResource("R1", "CONTENT1_MOD");
        event1.sendHmrEvent("E1",
                JacksonUtils.createObjectNode().put("key", "PAYLOAD1_MOD"));
        event1.updateClientResource("R3", "CONTENT3");
        event1.sendHmrEvent("E3",
                JacksonUtils.createObjectNode().put("key", "PAYLOAD3"));

        event1.merge(event2);

        event1.applyClientCommands(liveReload);

        verify(liveReload).update("R1", "CONTENT1");
        verify(liveReload).sendHmrEvent(eq("E1"), argThat(
                node -> node.get("key").stringValue().equals("PAYLOAD1")));
        verify(liveReload).update("R2", "CONTENT2");
        verify(liveReload).sendHmrEvent(eq("E2"), argThat(
                node -> node.get("key").stringValue().equals("PAYLOAD2")));
        verify(liveReload).update("R1", "CONTENT1_MOD");
        verify(liveReload).sendHmrEvent(eq("E1"), argThat(
                node -> node.get("key").stringValue().equals("PAYLOAD1_MOD")));
        verify(liveReload).update("R3", "CONTENT3");
        verify(liveReload).sendHmrEvent(eq("E3"), argThat(
                node -> node.get("key").stringValue().equals("PAYLOAD3")));

    }

}
