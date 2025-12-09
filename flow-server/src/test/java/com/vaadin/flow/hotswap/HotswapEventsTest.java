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

import java.net.URI;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedSession;

import static org.mockito.Mockito.when;

public class HotswapEventsTest {

    private VaadinService vaadinService;
    private Set<Class<?>> classes;
    private ObjectMapper objectMapper;

    @Before
    public void setup() {
        vaadinService = new MockVaadinServletService(false);
        classes = new HashSet<>(Set.of(String.class, Integer.class));
        objectMapper = new ObjectMapper();
    }

    // ========== Constructor Tests ==========

    @Test
    public void constructor_HotswapClassEvent_validParameters_createsInstance() {
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);
        Assert.assertNotNull("Event should be created", event);
        Assert.assertSame(vaadinService, event.getVaadinService());
        Assert.assertEquals(classes, event.getChangedClasses());
        Assert.assertTrue(event.isRedefined());
    }

    @Test
    public void constructor_nullVaadinService_throws() {
        Exception exception = Assert.assertThrows(NullPointerException.class,
                () -> new HotswapClassEvent(null, classes, true));
        Assert.assertTrue("Event should not be created with null service",
                exception.getMessage().contains("VaadinService"));
        exception = Assert.assertThrows(NullPointerException.class,
                () -> new HotswapClassSessionEvent(null,
                        new MockVaadinSession(), classes, true));
        Assert.assertTrue("Event should not be created with null service",
                exception.getMessage().contains("VaadinService"));
        exception = Assert.assertThrows(NullPointerException.class,
                () -> new HotswapResourceEvent(null, new HashSet<>()));
        Assert.assertTrue("Event should not be created with null service",
                exception.getMessage().contains("VaadinService"));
    }

    @Test
    public void constructor_HotswapClassEvent_nullClasses_createsInstance() {
        Exception exception = Assert.assertThrows(NullPointerException.class,
                () -> new HotswapClassEvent(vaadinService, null, true));
        Assert.assertTrue(
                "Event should not be created with null changed classes",
                exception.getMessage().contains("Changed classes"));
    }

    @Test
    public void constructor_HotswapClassEvent_emptyClasses_createsInstance() {
        HotswapClassEvent event = new HotswapClassEvent(vaadinService,
                new HashSet<>(), false);

        Assert.assertNotNull("Event should be created with empty classes",
                event);
        Assert.assertSame(vaadinService, event.getVaadinService());
        Assert.assertTrue(event.getChangedClasses().isEmpty());
        Assert.assertFalse(event.isRedefined());
    }

    @Test
    public void constructor_HotswapClassSessionEvent_nullVaadinSession_throws() {
        Exception exception = Assert.assertThrows(NullPointerException.class,
                () -> new HotswapClassSessionEvent(vaadinService, null, classes,
                        true));
        Assert.assertTrue("Event should not be created with null session",
                exception.getMessage().contains("VaadinSession"));
    }

    @Test
    public void constructor_HotswapClassSessionEvent_createsInstance() {
        VaadinSession session = new MockVaadinSession();
        HotswapClassSessionEvent event = new HotswapClassSessionEvent(
                vaadinService, session, classes, true);
        Assert.assertNotNull("Event should be created with empty classes",
                event);
        Assert.assertSame(session, event.getVaadinSession());
    }

    @Test
    public void constructor_HotswapResourceEvent_nullResources_throws() {
        Exception exception = Assert.assertThrows(NullPointerException.class,
                () -> new HotswapResourceEvent(vaadinService, null));
        Assert.assertTrue("Event should not be created with null resources",
                exception.getMessage().contains("Changed resources"));
    }

    @Test
    public void constructor_HotswapResourceEvent_createsInstance() {
        Set<URI> resources = new HashSet<>();
        resources.add(URI.create("/path/to/resource.js"));
        resources.add(URI.create("/path/to/resource2.js"));
        HotswapResourceEvent event = new HotswapResourceEvent(vaadinService,
                resources);
        Assert.assertNotNull("Event should be created with empty classes",
                event);
        Assert.assertEquals(resources, event.getChangedResources());
    }

    // ========== Global triggerUpdate Tests ==========

    @Test
    public void triggerUpdate_global_setRefresh_succeeds() {
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);

        event.triggerUpdate(UIUpdateStrategy.REFRESH);
        Assert.assertFalse(event.requiresPageReload());
        Assert.assertFalse(event.anyUIRequiresPageReload());
    }

    @Test
    public void triggerUpdate_global_setReload_succeeds() {
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);
        Assert.assertFalse("UI update not yet requested",
                event.requiresPageReload());

        event.triggerUpdate(UIUpdateStrategy.RELOAD);
        Assert.assertTrue(event.requiresPageReload());
        Assert.assertTrue(event.anyUIRequiresPageReload());
    }

    @Test
    public void triggerUpdate_global_refreshThenReload_acceptsReload() {
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);

        event.triggerUpdate(UIUpdateStrategy.REFRESH);
        event.triggerUpdate(UIUpdateStrategy.RELOAD);

        Assert.assertTrue(event.requiresPageReload());
        Assert.assertTrue(event.anyUIRequiresPageReload());
    }

    @Test
    public void triggerUpdate_global_reloadThenRefresh_keepsReload() {
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);

        event.triggerUpdate(UIUpdateStrategy.RELOAD);
        event.triggerUpdate(UIUpdateStrategy.REFRESH);

        Assert.assertTrue(event.requiresPageReload());
        Assert.assertTrue(event.anyUIRequiresPageReload());
    }

    @Test
    public void triggerUpdate_global_reloadThenReload_keepsReload() {
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);

        event.triggerUpdate(UIUpdateStrategy.RELOAD);
        event.triggerUpdate(UIUpdateStrategy.RELOAD);

        Assert.assertTrue(event.requiresPageReload());
        Assert.assertTrue(event.anyUIRequiresPageReload());
    }

    @Test
    public void triggerUpdate_global_nullStrategy_throws() {
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);
        Assert.assertThrows(NullPointerException.class,
                () -> event.triggerUpdate(null));
    }

    // ========== Per-UI triggerUpdate Tests ==========

    @Test
    public void triggerUpdate_perUI_setSingleUI_succeeds() {
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);
        UI ui = createMockUI();

        Assert.assertTrue("UI update should not have yet been triggered",
                event.getUIUpdateStrategy(ui).isEmpty());

        event.triggerUpdate(ui, UIUpdateStrategy.REFRESH);

        Assert.assertFalse(event.requiresPageReload());
        Optional<UIUpdateStrategy> strategy = event.getUIUpdateStrategy(ui);
        Assert.assertTrue(strategy.isPresent());
        Assert.assertEquals(UIUpdateStrategy.REFRESH, strategy.get());

        Assert.assertFalse(event.anyUIRequiresPageReload());
    }

    @Test
    public void triggerUpdate_perUI_setMultipleUIs_succeeds() {
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);
        UI ui1 = createMockUI();
        UI ui2 = createMockUI();

        event.triggerUpdate(ui1, UIUpdateStrategy.REFRESH);
        event.triggerUpdate(ui2, UIUpdateStrategy.RELOAD);

        Assert.assertFalse(event.requiresPageReload());
        Optional<UIUpdateStrategy> strategy1 = event.getUIUpdateStrategy(ui1);
        Assert.assertTrue(strategy1.isPresent());
        Assert.assertEquals(UIUpdateStrategy.REFRESH, strategy1.get());
        Optional<UIUpdateStrategy> strategy2 = event.getUIUpdateStrategy(ui2);
        Assert.assertTrue(strategy2.isPresent());
        Assert.assertEquals(UIUpdateStrategy.RELOAD, strategy2.get());

        Assert.assertTrue(event.anyUIRequiresPageReload());
    }

    @Test
    public void triggerUpdate_perUI_sameUIRefreshThenReload_acceptsReload() {
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);
        UI ui = createMockUI();

        event.triggerUpdate(ui, UIUpdateStrategy.REFRESH);
        event.triggerUpdate(ui, UIUpdateStrategy.RELOAD);

        Assert.assertFalse(event.requiresPageReload());
        Optional<UIUpdateStrategy> strategy = event.getUIUpdateStrategy(ui);
        Assert.assertTrue(strategy.isPresent());
        Assert.assertEquals(UIUpdateStrategy.RELOAD, strategy.get());

        Assert.assertTrue(event.anyUIRequiresPageReload());

    }

    @Test
    public void triggerUpdate_perUI_sameUIReloadThenRefresh_keepsReload() {
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);
        UI ui = createMockUI();

        event.triggerUpdate(ui, UIUpdateStrategy.RELOAD);
        event.triggerUpdate(ui, UIUpdateStrategy.REFRESH);

        Assert.assertFalse(event.requiresPageReload());
        Optional<UIUpdateStrategy> strategy = event.getUIUpdateStrategy(ui);
        Assert.assertTrue(strategy.isPresent());
        Assert.assertEquals(UIUpdateStrategy.RELOAD, strategy.get());

        Assert.assertTrue(event.anyUIRequiresPageReload());
    }

    @Test
    public void triggerUpdate_perUI_sameUIReloadThenReload_keepsReload() {
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);
        UI ui = createMockUI();

        event.triggerUpdate(ui, UIUpdateStrategy.RELOAD);
        event.triggerUpdate(ui, UIUpdateStrategy.RELOAD);

        Assert.assertFalse(event.requiresPageReload());
        Optional<UIUpdateStrategy> strategy = event.getUIUpdateStrategy(ui);
        Assert.assertTrue(strategy.isPresent());
        Assert.assertEquals(UIUpdateStrategy.RELOAD, strategy.get());

        Assert.assertTrue(event.anyUIRequiresPageReload());
    }

    @Test
    public void triggerUpdate_perUI_nullUI_throws() {
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);

        NullPointerException exception = Assert.assertThrows(
                NullPointerException.class,
                () -> event.triggerUpdate(null, UIUpdateStrategy.REFRESH));
        Assert.assertTrue("Should not be able to trigger update for null UI",
                exception.getMessage().contains("UI"));
    }

    @Test
    public void triggerUpdate_perUI_nullStrategy_throws() {
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);
        UI ui = createMockUI();

        NullPointerException exception = Assert.assertThrows(
                NullPointerException.class,
                () -> event.triggerUpdate(ui, null));
        Assert.assertTrue("Should not be able to trigger update for null UI",
                exception.getMessage().contains("UI update strategy"));
    }

    @Test
    public void getUIUpdateStrategy_uiValueNotSet_keepsGlobalValue() {
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);
        UI ui = createMockUI();

        event.triggerUpdate(UIUpdateStrategy.REFRESH);

        Optional<UIUpdateStrategy> strategy = event.getUIUpdateStrategy(ui);
        Assert.assertTrue(strategy.isPresent());
        Assert.assertEquals(UIUpdateStrategy.REFRESH, strategy.get());

        event.triggerUpdate(UIUpdateStrategy.RELOAD);
        strategy = event.getUIUpdateStrategy(ui);
        Assert.assertTrue(strategy.isPresent());
        Assert.assertEquals(UIUpdateStrategy.RELOAD, strategy.get());

        Assert.assertTrue(event.anyUIRequiresPageReload());
    }

    // ========== updateClientResource Tests ==========

    @Test
    public void updateClientResource_validPathAndContent_succeeds() {
        BrowserLiveReload reload = Mockito.mock(BrowserLiveReload.class);
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);

        String path = "/path/to/resource.js";
        String content = "console.log('test');";
        event.updateClientResource(path, content);

        event.applyClientCommands(reload);
        Mockito.verify(reload).update(path, content);
    }

    @Test
    public void updateClientResource_validPathNullContent_succeeds() {
        BrowserLiveReload reload = Mockito.mock(BrowserLiveReload.class);
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);

        String path = "/path/to/resource.js";
        event.updateClientResource(path, null);

        event.applyClientCommands(reload);
        Mockito.verify(reload).update(path, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateClientResource_nullPath_throwsException() {
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);

        event.updateClientResource(null, "content");
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateClientResource_emptyPath_throwsException() {
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);

        event.updateClientResource("", "content");
    }

    @Test
    public void updateClientResource_multipleResources_succeeds() {
        BrowserLiveReload reload = Mockito.mock(BrowserLiveReload.class);
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);

        String path1 = "/path1.js";
        String content1 = "content1";
        event.updateClientResource(path1, content1);

        String path2 = "/path2.css";
        String content2 = "content2";
        event.updateClientResource(path2, content2);

        String path3 = "/path3.html";
        event.updateClientResource(path3, null);

        event.applyClientCommands(reload);
        Mockito.verify(reload).update(path1, content1);
        Mockito.verify(reload).update(path2, content2);
        Mockito.verify(reload).update(path3, null);
    }

    @Test
    public void updateClientResource_samePath_allowsDuplicates() {
        BrowserLiveReload reload = Mockito.mock(BrowserLiveReload.class);
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);

        String path = "/same/path.js";
        String content1 = "content1";
        event.updateClientResource(path, content1);
        String content2 = "content2";
        event.updateClientResource(path, content2);

        event.applyClientCommands(reload);
        Mockito.verify(reload).update(path, content1);
        Mockito.verify(reload).update(path, content2);

    }

    // ========== sendHMRMessage Tests ==========

    @Test
    public void sendHmrEvent_validEventAndData_succeeds() {
        BrowserLiveReload reload = Mockito.mock(BrowserLiveReload.class);

        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);
        ObjectNode data = objectMapper.createObjectNode();
        data.put("key", "value");

        String eventName = "test-event";
        event.sendHmrEvent(eventName, data);

        event.applyClientCommands(reload);
        Mockito.verify(reload).sendHmrEvent(eventName, data);

    }

    @Test
    public void sendHmrEvent_validEventNullData_succeeds() {
        BrowserLiveReload reload = Mockito.mock(BrowserLiveReload.class);
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);

        String eventName = "test-event";
        event.sendHmrEvent(eventName, null);

        event.applyClientCommands(reload);
        Mockito.verify(reload).sendHmrEvent(eventName, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void sendHmrEvent_nullEvent_throwsException() {
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);
        ObjectNode data = objectMapper.createObjectNode();

        event.sendHmrEvent(null, data);
    }

    @Test(expected = IllegalArgumentException.class)
    public void sendHmrEvent_emptyEvent_throwsException() {
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);
        ObjectNode data = objectMapper.createObjectNode();

        event.sendHmrEvent("", data);
    }

    @Test
    public void sendHmrEvent_multipleMessages_succeeds() {
        BrowserLiveReload reload = Mockito.mock(BrowserLiveReload.class);
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);
        String eventName1 = "event1";
        ObjectNode data1 = objectMapper.createObjectNode();
        data1.put("key1", "value1");
        String eventName2 = "event2";
        ObjectNode data2 = objectMapper.createObjectNode();
        data2.put("key2", "value2");
        String eventName3 = "event3";

        event.sendHmrEvent(eventName1, data1);
        event.sendHmrEvent(eventName2, data2);
        event.sendHmrEvent(eventName3, null);

        event.applyClientCommands(reload);
        Mockito.verify(reload).sendHmrEvent(eventName1, data1);
        Mockito.verify(reload).sendHmrEvent(eventName2, data2);
        Mockito.verify(reload).sendHmrEvent(eventName3, null);
    }

    @Test
    public void sendHmrEvent_sameEvent_allowsDuplicates() {
        BrowserLiveReload reload = Mockito.mock(BrowserLiveReload.class);
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);
        ObjectNode data1 = objectMapper.createObjectNode();
        data1.put("key1", "value1");
        ObjectNode data2 = objectMapper.createObjectNode();
        data2.put("key2", "value2");

        String eventName = "same-event";
        event.sendHmrEvent(eventName, data1);
        event.sendHmrEvent(eventName, data2);

        event.applyClientCommands(reload);
        Mockito.verify(reload).sendHmrEvent(eventName, data1);
        Mockito.verify(reload).sendHmrEvent(eventName, data2);

    }

    // ========== Helper Methods ==========

    private UI createMockUI() {
        VaadinSession session = createMockVaadinSession();
        session.lock();
        try {
            UI ui = new UI();
            ui.getInternals().setSession(session);
            return ui;
        } finally {
            session.unlock();
        }
    }

    private VaadinSession createMockVaadinSession() {
        WrappedSession wrappedSession = Mockito.mock(WrappedSession.class);
        when(wrappedSession.getId()).thenReturn(UUID.randomUUID().toString());

        return new MockVaadinSession(vaadinService) {
            @Override
            public WrappedSession getSession() {
                return wrappedSession;
            }
        };
    }
}
