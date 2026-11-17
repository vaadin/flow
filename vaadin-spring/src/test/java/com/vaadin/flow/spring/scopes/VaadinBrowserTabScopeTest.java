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
package com.vaadin.flow.spring.scopes;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.ExtendedClientDetails;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VaadinBrowserTabScopeTest extends AbstractUIScopedTest {

    private int nextUiId = 2;

    @Test
    void get_currentUiIsSet_objectIsStored() {
        mockUI();
        get_currentScopeIsSet_objectIsStored(getScope());
    }

    @Test
    void remove_currentUiIsSet_objectIsStored() {
        mockUI();
        remove_currentScopeIsSet_objectIsStored(getScope());
    }

    @Test
    void registerDestructionCallback_currentUiIsSet_objectIsStored() {
        mockUI();
        registerDestructionCallback_currentScopeIsSet_objectIsStored(
                getScope());
    }

    @Test
    void get_noCurrentUI_throwException() {
        Scope scope = getScope();
        mockSession();

        assertThrows(IllegalStateException.class,
                () -> scope.get("foo", mock(ObjectFactory.class)));
    }

    @Test
    void get_uisInSameBrowserTab_shareBean() {
        UI ui = mockUI();
        setWindowName(ui, "tab-a");
        Scope scope = getScope();
        Object bean = scope.get("foo", Object::new);

        addUI(ui.getSession(), "tab-a");
        assertSame(bean, scope.get("foo", Object::new));

        addUI(ui.getSession(), "tab-b");
        assertNotSame(bean, scope.get("foo", Object::new));
    }

    @Test
    void destroySession_destructionCallbackIsCalled() {
        UI ui = mockUI();
        setWindowName(ui, "tab-a");
        VaadinSession session = ui.getSession();
        VaadinService service = session.getService();
        doCallRealMethod().when(session).addSessionDestroyListener(any());
        doCallRealMethod().when(session).getLockInstance();
        doCallRealMethod().when(session).getPendingAccessQueue();
        doCallRealMethod().when(session).access(any());

        Scope scope = getScope();
        AtomicInteger count = new AtomicInteger();
        scope.get("foo", Object::new);
        scope.registerDestructionCallback("foo", count::incrementAndGet);

        service.fireSessionDestroy(session);
        service.runPendingAccessTasks(session);

        assertEquals(1, count.get());
    }

    @Override
    protected VaadinBrowserTabScope getScope() {
        return new VaadinBrowserTabScope();
    }

    private void addUI(VaadinSession session, String windowName) {
        UI ui = new UI();
        ui.getInternals().setSession(session);
        ui.doInit(null, nextUiId++, "foo");
        setWindowName(ui, windowName);
        UI.setCurrent(ui);
    }

    private static void setWindowName(UI ui, String windowName) {
        ExtendedClientDetails details = mock(ExtendedClientDetails.class);
        when(details.getWindowName()).thenReturn(windowName);
        ui.getInternals().setExtendedClientDetails(details);
    }
}
