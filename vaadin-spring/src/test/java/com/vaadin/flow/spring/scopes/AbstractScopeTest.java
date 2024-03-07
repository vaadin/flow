/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.scopes;

import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.DefaultDeploymentConfiguration;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.VaadinSessionState;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.spring.SpringVaadinSession;

import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public abstract class AbstractScopeTest {

    private VaadinSession session;

    public static class TestSession extends SpringVaadinSession {

        public TestSession() {
            super(Mockito.mock(VaadinService.class));
        }

    }

    @After
    public void clearSession() {
        session = null;
    }

    @Test(expected = IllegalStateException.class)
    public void get_noCurrentSession_throwException() {
        Scope scope = getScope();

        scope.get("foo", Mockito.mock(ObjectFactory.class));
    }

    @Test(expected = IllegalStateException.class)
    public void registerDestructionCallback_noCurrentSession_throwException() {
        Scope scope = getScope();

        scope.registerDestructionCallback("foo", Mockito.mock(Runnable.class));
    }

    @Test(expected = IllegalStateException.class)
    public void remove_noCurrentSession_throwException() {
        Scope scope = getScope();

        scope.remove("foo");
    }

    @SuppressWarnings("rawtypes")
    public void get_currentScopeIsSet_objectIsStored(Scope scope) {
        ObjectFactory factory = Mockito.mock(ObjectFactory.class);

        Object object = new Object();

        when(factory.getObject()).thenReturn(object);
        Object fooObject = scope.get("foo", factory);

        Assert.assertSame(object, fooObject);
        verify(factory).getObject();

        fooObject = scope.get("foo", factory);

        // This time it has not been called. Otherwise this would have failed.
        verify(factory).getObject();
        Assert.assertSame(object, fooObject);
    }

    @SuppressWarnings("rawtypes")
    protected void remove_currentScopeIsSet_objectIsStored(Scope scope) {
        ObjectFactory factory = Mockito.mock(ObjectFactory.class);

        Object object = new Object();

        when(factory.getObject()).thenReturn(object);
        scope.get("foo", factory);

        verify(factory).getObject();

        scope.remove("foo");

        scope.get("foo", factory);

        // Object is not available, so factory is called one more time
        verify(factory, times(2)).getObject();

    }

    protected void registerDestructionCallback_currentScopeIsSet_objectIsStored(
            AbstractScope scope) {
        AtomicInteger count = new AtomicInteger();
        scope.registerDestructionCallback("foo", () -> count.getAndIncrement());

        scope.getBeanStore().destroy();

        Assert.assertEquals(1, count.get());
    }

    @SuppressWarnings("unchecked")
    protected VaadinSession mockSession() {
        SpringVaadinSession session = Mockito.mock(TestSession.class,
                Mockito.withSettings().useConstructor());
        doCallRealMethod().when(session).setAttribute(Mockito.any(Class.class),
                Mockito.any());
        doCallRealMethod().when(session).getAttribute(Mockito.any(Class.class));
        doCallRealMethod().when(session)
                .getAttribute(Mockito.any(String.class));

        doCallRealMethod().when(session).addUI(Mockito.any());
        doCallRealMethod().when(session).removeUI(Mockito.any());

        doCallRealMethod().when(session).getService();
        doCallRealMethod().when(session).getUIs();

        when(session.getState()).thenReturn(VaadinSessionState.OPEN);

        final Properties initParameters = new Properties();
        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(appConfig.getPropertyNames())
                .thenReturn(Collections.emptyEnumeration());
        VaadinContext context = Mockito.mock(VaadinContext.class);
        Lookup lookup = Mockito.mock(Lookup.class);
        Mockito.when(context.getAttribute(Lookup.class)).thenReturn(lookup);
        Mockito.when(appConfig.getContext()).thenReturn(context);
        DefaultDeploymentConfiguration config = new DefaultDeploymentConfiguration(
                appConfig, getClass(), initParameters);
        when(session.getConfiguration()).thenReturn(config);

        VaadinSession.setCurrent(session);
        when(session.hasLock()).thenReturn(true);

        // keep a reference to the session so that it cannot be GCed.
        this.session = session;

        return session;
    }

    protected abstract Scope getScope();
}
