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
package com.vaadin.base.devserver.hotswap;

import java.net.URI;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class HotswapEventsTest {

    private VaadinService vaadinService;
    private Set<Class<?>> classes;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        vaadinService = new MockVaadinServletService(false);
        classes = new HashSet<>(Set.of(String.class, Integer.class));
        objectMapper = new ObjectMapper();
    }

    // ========== Constructor Tests ==========

    @Test
    void constructor_HotswapClassEvent_validParameters_createsInstance() {
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);
        assertNotNull(event, "Event should be created");
        assertSame(vaadinService, event.getVaadinService());
        assertEquals(classes, event.getChangedClasses());
        assertTrue(event.isRedefined());
    }

    @Test
    void constructor_nullVaadinService_throws() {
        Exception exception = assertThrows(NullPointerException.class,
                () -> new HotswapClassEvent(null, classes, true));
        assertTrue(exception.getMessage().contains("VaadinService"),
                "Event should not be created with null service");
        exception = assertThrows(NullPointerException.class,
                () -> new HotswapClassSessionEvent(null,
                        new MockVaadinSession(), classes, true));
        assertTrue(exception.getMessage().contains("VaadinService"),
                "Event should not be created with null service");
        exception = assertThrows(NullPointerException.class,
                () -> new HotswapResourceEvent(null, new HashSet<>()));
        assertTrue(exception.getMessage().contains("VaadinService"),
                "Event should not be created with null service");
    }

    @Test
    void constructor_HotswapClassEvent_nullClasses_createsInstance() {
        Exception exception = assertThrows(NullPointerException.class,
                () -> new HotswapClassEvent(vaadinService, null, true));
        assertTrue(exception.getMessage().contains("Changed classes"),
                "Event should not be created with null changed classes");
    }

    @Test
    void constructor_HotswapClassEvent_emptyClasses_createsInstance() {
        HotswapClassEvent event = new HotswapClassEvent(vaadinService,
                new HashSet<>(), false);

        assertNotNull(event, "Event should be created with empty classes");
        assertSame(vaadinService, event.getVaadinService());
        assertTrue(event.getChangedClasses().isEmpty());
        assertFalse(event.isRedefined());
    }

    @Test
    void constructor_HotswapClassSessionEvent_nullVaadinSession_throws() {
        Exception exception = assertThrows(NullPointerException.class,
                () -> new HotswapClassSessionEvent(vaadinService, null, classes,
                        true));
        assertTrue(exception.getMessage().contains("VaadinSession"),
                "Event should not be created with null session");
    }

    @Test
    void constructor_HotswapClassSessionEvent_createsInstance() {
        VaadinSession session = new MockVaadinSession();
        HotswapClassSessionEvent event = new HotswapClassSessionEvent(
                vaadinService, session, classes, true);
        assertNotNull(event, "Event should be created with empty classes");
        assertSame(session, event.getVaadinSession());
    }

    @Test
    void constructor_HotswapResourceEvent_nullResources_throws() {
        Exception exception = assertThrows(NullPointerException.class,
                () -> new HotswapResourceEvent(vaadinService, null));
        assertTrue(exception.getMessage().contains("Changed resources"),
                "Event should not be created with null resources");
    }

    @Test
    void constructor_HotswapResourceEvent_createsInstance() {
        Set<URI> resources = new HashSet<>();
        resources.add(URI.create("/path/to/resource.js"));
        resources.add(URI.create("/path/to/resource2.js"));
        HotswapResourceEvent event = new HotswapResourceEvent(vaadinService,
                resources);
        assertNotNull(event, "Event should be created with empty classes");
        assertEquals(resources, event.getChangedResources());
    }

    // ========== Global triggerUpdate Tests ==========

    @Test
    void triggerUpdate_global_setRefresh_succeeds() {
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);

        event.triggerUpdate(UIUpdateStrategy.REFRESH);
        assertFalse(event.requiresPageReload());
        assertFalse(event.anyUIRequiresPageReload());
    }

    @Test
    void triggerUpdate_global_setReload_succeeds() {
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);
        assertFalse(event.requiresPageReload(), "UI update not yet requested");

        event.triggerUpdate(UIUpdateStrategy.RELOAD);
        assertTrue(event.requiresPageReload());
        assertTrue(event.anyUIRequiresPageReload());
    }

    @Test
    void triggerUpdate_global_refreshThenReload_acceptsReload() {
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);

        event.triggerUpdate(UIUpdateStrategy.REFRESH);
        event.triggerUpdate(UIUpdateStrategy.RELOAD);

        assertTrue(event.requiresPageReload());
        assertTrue(event.anyUIRequiresPageReload());
    }

    @Test
    void triggerUpdate_global_reloadThenRefresh_keepsReload() {
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);

        event.triggerUpdate(UIUpdateStrategy.RELOAD);
        event.triggerUpdate(UIUpdateStrategy.REFRESH);

        assertTrue(event.requiresPageReload());
        assertTrue(event.anyUIRequiresPageReload());
    }

    @Test
    void triggerUpdate_global_reloadThenReload_keepsReload() {
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);

        event.triggerUpdate(UIUpdateStrategy.RELOAD);
        event.triggerUpdate(UIUpdateStrategy.RELOAD);

        assertTrue(event.requiresPageReload());
        assertTrue(event.anyUIRequiresPageReload());
    }

    @Test
    void triggerUpdate_global_nullStrategy_throws() {
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);
        assertThrows(NullPointerException.class,
                () -> event.triggerUpdate(null));
    }

    // ========== Per-UI triggerUpdate Tests ==========

    @Test
    void triggerUpdate_perUI_setSingleUI_succeeds() {
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);
        UI ui = createMockUI();

        assertTrue(event.getUIUpdateStrategy(ui).isEmpty(),
                "UI update should not have yet been triggered");

        event.triggerUpdate(ui, UIUpdateStrategy.REFRESH);

        assertFalse(event.requiresPageReload());
        Optional<UIUpdateStrategy> strategy = event.getUIUpdateStrategy(ui);
        assertTrue(strategy.isPresent());
        assertEquals(UIUpdateStrategy.REFRESH, strategy.get());

        assertFalse(event.anyUIRequiresPageReload());
    }

    @Test
    void triggerUpdate_perUI_setMultipleUIs_succeeds() {
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);
        UI ui1 = createMockUI();
        UI ui2 = createMockUI();

        event.triggerUpdate(ui1, UIUpdateStrategy.REFRESH);
        event.triggerUpdate(ui2, UIUpdateStrategy.RELOAD);

        assertFalse(event.requiresPageReload());
        Optional<UIUpdateStrategy> strategy1 = event.getUIUpdateStrategy(ui1);
        assertTrue(strategy1.isPresent());
        assertEquals(UIUpdateStrategy.REFRESH, strategy1.get());
        Optional<UIUpdateStrategy> strategy2 = event.getUIUpdateStrategy(ui2);
        assertTrue(strategy2.isPresent());
        assertEquals(UIUpdateStrategy.RELOAD, strategy2.get());

        assertTrue(event.anyUIRequiresPageReload());
    }

    @Test
    void triggerUpdate_perUI_sameUIRefreshThenReload_acceptsReload() {
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);
        UI ui = createMockUI();

        event.triggerUpdate(ui, UIUpdateStrategy.REFRESH);
        event.triggerUpdate(ui, UIUpdateStrategy.RELOAD);

        assertFalse(event.requiresPageReload());
        Optional<UIUpdateStrategy> strategy = event.getUIUpdateStrategy(ui);
        assertTrue(strategy.isPresent());
        assertEquals(UIUpdateStrategy.RELOAD, strategy.get());

        assertTrue(event.anyUIRequiresPageReload());

    }

    @Test
    void triggerUpdate_perUI_sameUIReloadThenRefresh_keepsReload() {
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);
        UI ui = createMockUI();

        event.triggerUpdate(ui, UIUpdateStrategy.RELOAD);
        event.triggerUpdate(ui, UIUpdateStrategy.REFRESH);

        assertFalse(event.requiresPageReload());
        Optional<UIUpdateStrategy> strategy = event.getUIUpdateStrategy(ui);
        assertTrue(strategy.isPresent());
        assertEquals(UIUpdateStrategy.RELOAD, strategy.get());

        assertTrue(event.anyUIRequiresPageReload());
    }

    @Test
    void triggerUpdate_perUI_sameUIReloadThenReload_keepsReload() {
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);
        UI ui = createMockUI();

        event.triggerUpdate(ui, UIUpdateStrategy.RELOAD);
        event.triggerUpdate(ui, UIUpdateStrategy.RELOAD);

        assertFalse(event.requiresPageReload());
        Optional<UIUpdateStrategy> strategy = event.getUIUpdateStrategy(ui);
        assertTrue(strategy.isPresent());
        assertEquals(UIUpdateStrategy.RELOAD, strategy.get());

        assertTrue(event.anyUIRequiresPageReload());
    }

    @Test
    void triggerUpdate_perUI_nullUI_throws() {
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> event.triggerUpdate(null, UIUpdateStrategy.REFRESH));
        assertTrue(exception.getMessage().contains("UI"),
                "Should not be able to trigger update for null UI");
    }

    @Test
    void triggerUpdate_perUI_nullStrategy_throws() {
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);
        UI ui = createMockUI();

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> event.triggerUpdate(ui, null));
        assertTrue(exception.getMessage().contains("UI update strategy"),
                "Should not be able to trigger update for null UI");
    }

    @Test
    void getUIUpdateStrategy_uiValueNotSet_keepsGlobalValue() {
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);
        UI ui = createMockUI();

        event.triggerUpdate(UIUpdateStrategy.REFRESH);

        Optional<UIUpdateStrategy> strategy = event.getUIUpdateStrategy(ui);
        assertTrue(strategy.isPresent());
        assertEquals(UIUpdateStrategy.REFRESH, strategy.get());

        event.triggerUpdate(UIUpdateStrategy.RELOAD);
        strategy = event.getUIUpdateStrategy(ui);
        assertTrue(strategy.isPresent());
        assertEquals(UIUpdateStrategy.RELOAD, strategy.get());

        assertTrue(event.anyUIRequiresPageReload());
    }

    // ========== updateClientResource Tests ==========

    @Test
    void updateClientResource_validPathAndContent_succeeds() {
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
    void updateClientResource_validPathNullContent_succeeds() {
        BrowserLiveReload reload = Mockito.mock(BrowserLiveReload.class);
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);

        String path = "/path/to/resource.js";
        event.updateClientResource(path, null);

        event.applyClientCommands(reload);
        Mockito.verify(reload).update(path, null);
    }

    @Test
    void updateClientResource_nullPath_throwsException() {
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);
        assertThrows(IllegalArgumentException.class,
                () -> event.updateClientResource(null, "content"));
    }

    @Test
    void updateClientResource_emptyPath_throwsException() {
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);
        assertThrows(IllegalArgumentException.class,
                () -> event.updateClientResource("", "content"));
    }

    @Test
    void updateClientResource_multipleResources_succeeds() {
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
    void updateClientResource_samePath_allowsDuplicates() {
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
    void sendHmrEvent_validEventAndData_succeeds() {
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
    void sendHmrEvent_validEventNullData_succeeds() {
        BrowserLiveReload reload = Mockito.mock(BrowserLiveReload.class);
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);

        String eventName = "test-event";
        event.sendHmrEvent(eventName, null);

        event.applyClientCommands(reload);
        Mockito.verify(reload).sendHmrEvent(eventName, null);
    }

    @Test
    void sendHmrEvent_nullEvent_throwsException() {
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);
        ObjectNode data = objectMapper.createObjectNode();
        assertThrows(IllegalArgumentException.class,
                () -> event.sendHmrEvent(null, data));
    }

    @Test
    void sendHmrEvent_emptyEvent_throwsException() {
        HotswapClassEvent event = new HotswapClassEvent(vaadinService, classes,
                true);
        ObjectNode data = objectMapper.createObjectNode();
        assertThrows(IllegalArgumentException.class,
                () -> event.sendHmrEvent("", data));
    }

    @Test
    void sendHmrEvent_multipleMessages_succeeds() {
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
    void sendHmrEvent_sameEvent_allowsDuplicates() {
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
