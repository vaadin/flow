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

import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;

import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.VaadinSessionState;
import com.vaadin.spring.SpringVaadinSession;

public abstract class AbstractScopeTest {

    public static class TestSession extends SpringVaadinSession {

        public TestSession() {
            super(Mockito.mock(VaadinService.class));
        }

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

        doCallRealMethod().when(session).getService();

        when(session.getState()).thenReturn(VaadinSessionState.OPEN);

        VaadinSession.setCurrent(session);
        when(session.hasLock()).thenReturn(true);

        return session;
    }

    protected abstract Scope getScope();
}
