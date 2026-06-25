/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.scopes;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VaadinUIScopeTest extends AbstractUIScopedTest {

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

        VaadinSession session = VaadinSession.getCurrent();
        VaadinService service = session.getService();

        doCallRealMethod().when(session)
                .addSessionDestroyListener(Mockito.any());
        doCallRealMethod().when(session).getLockInstance();
        doCallRealMethod().when(session).getPendingAccessQueue();
        doCallRealMethod().when(session).access(Mockito.any());

        VaadinUIScope scope = new VaadinUIScope();

        AtomicInteger count = new AtomicInteger();
        scope.registerDestructionCallback("foo", () -> count.getAndIncrement());

        Object object = new Object();

        ObjectFactory factory = Mockito.mock(ObjectFactory.class);
        when(factory.getObject()).thenReturn(object);
        scope.get("foo", factory);

        String attribute = VaadinUIScope.class.getName() + "$UIStoreWrapper";

        // self control - the attribute name is used by the implementation
        Assert.assertNotNull(session.getAttribute(attribute));

        service.fireSessionDestroy(session);
        service.runPendingAccessTasks(session);

        Assert.assertEquals(1, count.get());
        Assert.assertNull(session.getAttribute(attribute));

        // Destruction callbacks are not called anymore (they are removed)
        scope.getBeanStore().destroy();
        Assert.assertEquals(1, count.get());

        // object has been removed from the storage, so object factory is called
        // once again to create the bean
        scope.get("foo", factory);
        verify(factory, times(2)).getObject();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void detachUI_uiClosing_sessionAttributeIsCleanedAndDestructionCallbackIsCalled() {
        UI ui = mockUI();
        ui.close();

        VaadinUIScope scope = new VaadinUIScope();

        AtomicInteger count = new AtomicInteger();
        scope.registerDestructionCallback("foo", () -> count.getAndIncrement());

        Object object = new Object();

        ObjectFactory factory = Mockito.mock(ObjectFactory.class);
        when(factory.getObject()).thenReturn(object);
        scope.get("foo", factory);

        ComponentUtil.onComponentDetach(ui);

        Assert.assertEquals(1, count.get());

        // Destruction callbacks are not called anymore (they are removed)
        scope.getBeanStore().destroy();
        Assert.assertEquals(1, count.get());

        // object has been removed from the storage, so object factory is called
        // once again to create the bean
        scope.get("foo", factory);
        verify(factory, times(2)).getObject();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void detachUi_uiNotClosing_sessionAttributeIsNotCleanedAndDestructionCallbackIsNotCalled() {
        UI ui = mockUI();

        VaadinUIScope scope = new VaadinUIScope();

        AtomicInteger count = new AtomicInteger();
        scope.registerDestructionCallback("foo", () -> count.getAndIncrement());

        Object object = new Object();

        ObjectFactory factory = Mockito.mock(ObjectFactory.class);
        when(factory.getObject()).thenReturn(object);
        scope.get("foo", factory);

        ComponentUtil.onComponentDetach(ui);

        Assert.assertEquals(0, count.get());

        // Destruction callbacks are not called anymore (they are removed)
        // scope.getBeanStore().destroy();
        Assert.assertEquals(0, count.get());

        // object has been not removed from the storage, so object factory is
        // not called to create the bean
        scope.get("foo", factory);
        verify(factory, times(1)).getObject();
    }

    @Override
    protected Scope getScope() {
        return new VaadinUIScope();
    }

}
