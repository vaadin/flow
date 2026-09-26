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
package com.vaadin.flow.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.ExtendedClientDetails;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BrowserTabTest {

    private MockVaadinSession session;

    private int nextUiId;

    @BeforeEach
    void setup() {
        session = new MockVaadinSession();
        session.lock();
    }

    @AfterEach
    void tearDown() {
        session.unlock();
        CurrentInstance.clearAll();
    }

    @Test
    void get_uisWithSameWindowName_shareAttributes() {
        UI ui = addUI("tab-a");
        BrowserTab.get(ui).setAttribute(String.class, "booking");

        UI reloadedUI = addUI("tab-a");
        UI otherTabUI = addUI("tab-b");

        assertSame(BrowserTab.get(ui), BrowserTab.get(reloadedUI));
        assertSame(BrowserTab.getCurrent(), BrowserTab.get(otherTabUI));
        assertEquals("tab-a", BrowserTab.get(reloadedUI).getId());
        assertEquals("booking",
                BrowserTab.get(reloadedUI).getAttribute(String.class));
        assertNull(BrowserTab.get(otherTabUI).getAttribute(String.class));
    }

    @Test
    void get_uisWithoutWindowName_doNotShareBrowserTab() {
        UI ui = addUI(null);
        UI otherUI = addUI(null);

        assertSame(BrowserTab.get(ui), BrowserTab.get(ui));
        assertNotSame(BrowserTab.get(ui), BrowserTab.get(otherUI));
    }

    @Test
    void destroyInactiveTabs_tabWithOpenUI_isKept() {
        UI ui = addUI("tab-a");
        BrowserTab tab = BrowserTab.get(ui);
        AtomicInteger destroyed = new AtomicInteger();
        tab.addDestroyListener(destroyed::incrementAndGet);

        // An open UI that never used the tab also keeps it, by window name
        removeUI(ui);
        UI reloadedUI = addUI("tab-a");
        BrowserTab.destroyInactiveTabs(session, 0);

        assertEquals(0, destroyed.get());
        assertSame(tab, BrowserTab.get(reloadedUI));
    }

    @Test
    void destroyInactiveTabs_tabWithoutOpenUI_isDestroyedAfterTimeout() {
        UI ui = addUI("tab-a");
        BrowserTab tab = BrowserTab.get(ui);
        tab.setAttribute("key", "value");
        AtomicInteger destroyed = new AtomicInteger();
        tab.addDestroyListener(() -> {
            assertEquals("value", tab.getAttribute("key"));
            destroyed.incrementAndGet();
        });

        // A closing UI, like the one being reloaded, does not keep the tab,
        // but its recent heartbeat does until the timeout
        ui.close();
        BrowserTab.destroyInactiveTabs(session, 60_000);
        removeUI(ui);
        BrowserTab.destroyInactiveTabs(session, 60_000);
        BrowserTab.destroyInactiveTabs(session, -1);
        assertEquals(0, destroyed.get());

        BrowserTab.destroyInactiveTabs(session, 0);
        assertEquals(1, destroyed.get());
        assertNull(tab.getAttribute("key"));
        assertThrows(IllegalStateException.class,
                () -> tab.setAttribute("key", "value"));
        assertNotSame(tab, BrowserTab.get(addUI("tab-a")));
    }

    @Test
    void destroyAllTabs_destroysEveryTab() {
        List<Throwable> errors = new ArrayList<>();
        session.setErrorHandler(event -> errors.add(event.getThrowable()));
        IllegalStateException failure = new IllegalStateException("failure");
        AtomicInteger destroyed = new AtomicInteger();
        BrowserTab tab = BrowserTab.get(addUI("tab-a"));
        tab.addDestroyListener(() -> {
            throw failure;
        });
        tab.addDestroyListener(destroyed::incrementAndGet);
        BrowserTab.get(addUI("tab-b"))
                .addDestroyListener(destroyed::incrementAndGet);

        BrowserTab.destroyAllTabs(session);

        assertEquals(2, destroyed.get());
        assertEquals(List.of(failure), errors);
    }

    private UI addUI(String windowName) {
        int uiId = nextUiId++;
        UI ui = new MockUI(session) {
            @Override
            public int getUIId() {
                return uiId;
            }
        };
        ExtendedClientDetails details = mock(ExtendedClientDetails.class);
        when(details.getWindowName()).thenReturn(windowName);
        ui.getInternals().setExtendedClientDetails(details);
        session.addUI(ui);
        return ui;
    }

    private void removeUI(UI ui) {
        UI.setCurrent(ui);
        session.removeUI(ui);
    }
}
