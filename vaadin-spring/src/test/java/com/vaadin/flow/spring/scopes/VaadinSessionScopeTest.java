/*
 * Copyright 2000-2023 Vaadin Ltd.
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

import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;

import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.SpringVaadinSession;

import net.jcip.annotations.NotThreadSafe;

@NotThreadSafe
public class VaadinSessionScopeTest extends AbstractScopeTest {

    @Before
    public void tearDown() {
        VaadinSession.setCurrent(null);
    }

    @Test
    public void get_currentSessionIsSet_objectIsStored() {
        VaadinSessionScope scope = new VaadinSessionScope();

        mockSession();

        get_currentScopeIsSet_objectIsStored(scope);
    }

    @Test
    public void remove_currentSessionIsSet_objectIsStored() {
        VaadinSessionScope scope = new VaadinSessionScope();
        mockSession();

        remove_currentScopeIsSet_objectIsStored(scope);
    }

    @Test
    public void registerDestructionCallback_currentSessionIsSet_objectIsStored() {
        VaadinSessionScope scope = new VaadinSessionScope();
        mockSession();

        registerDestructionCallback_currentScopeIsSet_objectIsStored(scope);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void destroySession_sessionAttributeIsCleanedAndDestructionCallbackIsCalled() {
        VaadinSession session = mockSession();
        SpringVaadinSession springSession = (SpringVaadinSession) session;

        doCallRealMethod().when(springSession)
                .addDestroyListener(Mockito.any());

        doCallRealMethod().when(springSession).fireSessionDestroy();

        VaadinSessionScope scope = new VaadinSessionScope();

        AtomicInteger count = new AtomicInteger();
        scope.registerDestructionCallback("foo", () -> count.getAndIncrement());

        Object object = new Object();

        ObjectFactory factory = Mockito.mock(ObjectFactory.class);
        when(factory.getObject()).thenReturn(object);
        scope.get("foo", factory);

        springSession.fireSessionDestroy();

        Assert.assertEquals(1, count.get());
        Assert.assertNull(session.getAttribute(BeanStore.class));

        // Destruction callbacks are not called anymore (they are removed)
        scope.getBeanStore().destroy();
        Assert.assertEquals(1, count.get());

        // object has been removed from the storage, so object factory is called
        // once again to create the bean
        scope.get("foo", factory);
        verify(factory, times(2)).getObject();
    }

    @Override
    protected Scope getScope() {
        return new VaadinSessionScope();
    }

}
