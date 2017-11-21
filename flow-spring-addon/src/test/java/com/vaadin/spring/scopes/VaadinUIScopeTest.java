/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.spring.scopes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;

import com.vaadin.flow.router.ImmutableRouterConfiguration;
import com.vaadin.router.RouterInterface;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;
import com.vaadin.spring.SpringVaadinSession;
import com.vaadin.ui.ComponentUtil;
import com.vaadin.ui.UI;

public class VaadinUIScopeTest extends AbstractScopeTest {

    @Before
    public void tearDown() {
        VaadinSession.setCurrent(null);
        UI.setCurrent(null);
    }

    @Test
    public void get_currentUiIsSet_objectIsStored() {
        VaadinUIScope scope = new VaadinUIScope();

        mockUI();
        get_currentScopeIsSet_objectIsStored(scope);
    }

    @Test
    public void remove_currentUiIsSet_objectIsStored() {
        VaadinUIScope scope = new VaadinUIScope();

        mockUI();
        remove_currentScopeIsSet_objectIsStored(scope);
    }

    @Test
    public void registerDestructionCallback_currentSessionIsSet_objectIsStored() {
        VaadinUIScope scope = new VaadinUIScope();

        mockUI();
        registerDestructionCallback_currentScopeIsSet_objectIsStored(scope);
    }

    @Test(expected = IllegalStateException.class)
    public void get_noCurrentUI_throwException() {
        Scope scope = getScope();
        mockSession();

        scope.get("foo", Mockito.mock(ObjectFactory.class));
    }

    @Test(expected = IllegalStateException.class)
    public void registerDestructionCallback_noCurrentUI_throwException() {
        Scope scope = getScope();
        mockSession();

        scope.registerDestructionCallback("foo", Mockito.mock(Runnable.class));
    }

    @Test(expected = IllegalStateException.class)
    public void remove_noCurrentUI_throwException() {
        Scope scope = getScope();

        mockSession();

        scope.remove("foo");
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void destroySession_sessionAttributeIsCleanedAndDestructionCallbackIsCalled() {
        mockUI();

        SpringVaadinSession springSession = (SpringVaadinSession) VaadinSession
                .getCurrent();

        doCallRealMethod().when(springSession)
                .addDestroyListener(Mockito.any());

        doCallRealMethod().when(springSession).fireSessionDestroy();

        VaadinUIScope scope = new VaadinUIScope();

        AtomicInteger count = new AtomicInteger();
        scope.registerDestructionCallback("foo", () -> count.getAndIncrement());

        Object object = new Object();

        ObjectFactory factory = Mockito.mock(ObjectFactory.class);
        when(factory.getObject()).thenReturn(object);
        scope.get("foo", factory);

        String attribute = VaadinUIScope.class.getName() + "$UIStoreWrapper";

        // self control - the attribute name is used by the implementation
        assertNotNull(springSession.getAttribute(attribute));

        springSession.fireSessionDestroy();

        assertEquals(1, count.get());
        assertNull(springSession.getAttribute(attribute));

        // Destruction callbacks are not called anymore (they are removed)
        scope.getBeanStore().destroy();
        assertEquals(1, count.get());

        // object has been removed from the storage, so object factory is called
        // once again to create the bean
        scope.get("foo", factory);
        verify(factory, times(2)).getObject();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void datachUI_sessionAttributeIsCleanedAndDestructionCallbackIsCalled() {
        UI ui = mockUI();

        VaadinUIScope scope = new VaadinUIScope();

        AtomicInteger count = new AtomicInteger();
        scope.registerDestructionCallback("foo", () -> count.getAndIncrement());

        Object object = new Object();

        ObjectFactory factory = Mockito.mock(ObjectFactory.class);
        when(factory.getObject()).thenReturn(object);
        scope.get("foo", factory);

        ComponentUtil.onComponentDetach(ui);

        assertEquals(1, count.get());

        // Destruction callbacks are not called anymore (they are removed)
        scope.getBeanStore().destroy();
        assertEquals(1, count.get());

        // object has been removed from the storage, so object factory is called
        // once again to create the bean
        scope.get("foo", factory);
        verify(factory, times(2)).getObject();
    }

    @Override
    protected Scope getScope() {
        return new VaadinUIScope();
    }

    private UI mockUI() {
        VaadinSession session = mockSession();

        RouterInterface routerIface = mock(RouterInterface.class);
        VaadinService service = session.getService();
        when(service.getRouter()).thenReturn(routerIface);

        ImmutableRouterConfiguration config = mock(
                ImmutableRouterConfiguration.class);
        when(routerIface.getConfiguration()).thenReturn(config);
        when(config.isConfigured()).thenReturn(false);

        UI ui = new UI();
        ui.getInternals().setSession(session);
        ui.doInit(null, 1);

        UI.setCurrent(ui);
        return ui;
    }
}
