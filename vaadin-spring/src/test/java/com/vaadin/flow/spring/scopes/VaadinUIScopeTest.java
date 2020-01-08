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
package com.vaadin.flow.spring.scopes;

import javax.servlet.ServletContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.DefaultDeploymentConfiguration;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.SpringVaadinSession;

import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@NotThreadSafe
public class VaadinUIScopeTest extends AbstractScopeTest {

    private UI ui;

    @Before
    public void setUp() {
        VaadinSession.setCurrent(null);
        UI.setCurrent(null);
        ui = null;
    }

    @After
    public void clearUI() {
        ui = null;
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
        Assert.assertNotNull(springSession.getAttribute(attribute));

        springSession.fireSessionDestroy();

        Assert.assertEquals(1, count.get());
        Assert.assertNull(springSession.getAttribute(attribute));

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

        Assert.assertEquals(1, count.get());

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
        return new VaadinUIScope();
    }

    private UI mockUI() {
        VaadinSession session = mockSession();

        Router router = mock(Router.class);
        VaadinService service = session.getService();
        when(service.getRouter()).thenReturn(router);

        Properties initParameters = new Properties();
        when(service.getDeploymentConfiguration())
                .thenReturn(new DefaultDeploymentConfiguration(getClass(),
                        initParameters));

        when(service.getMainDivId(Mockito.any(), Mockito.any()))
                .thenReturn(" - ");

        final Map<String, Object> attributeMap = new HashMap<>();

        ServletContext servletContext = Mockito.mock(ServletContext.class);
        Mockito.when(servletContext.getAttribute(Mockito.anyString())).then(invocationOnMock -> attributeMap.get(invocationOnMock.getArguments()[0].toString()));
        Mockito.doAnswer(invocationOnMock -> attributeMap.put(
                invocationOnMock.getArguments()[0].toString(),
                invocationOnMock.getArguments()[1]
        )).when(servletContext).setAttribute(Mockito.anyString(), Mockito.any());

        VaadinServletContext context = new VaadinServletContext(servletContext);
        Mockito.when(service.getContext()).thenReturn(context);

        UI ui = new UI();
        ui.getInternals().setSession(session);
        ui.doInit(null, 1);

        UI.setCurrent(ui);

        // prevent UI from being GCed.
        this.ui = ui;
        return ui;
    }
}
