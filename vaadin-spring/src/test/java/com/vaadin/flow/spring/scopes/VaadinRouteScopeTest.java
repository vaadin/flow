/*
 * Copyright 2000-2021 Vaadin Ltd.
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

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.web.context.WebApplicationContext;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.UIInitEvent;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.SpringVaadinSession;

import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VaadinRouteScopeTest extends AbstractUIScopedTest {

    @Override
    protected VaadinRouteScope getScope() {
        return new VaadinRouteScope();
    }

    @Test
    public void detachUI_uisHaveNoWindowName_beanInScopeIsDestroyed() {
        UI ui = mockUI();

        UI anotherUI = new UI() {

            @Override
            public int getUIId() {
                return ui.getUIId() + 1;
            }

            @Override
            public VaadinSession getSession() {
                return ui.getSession();
            }
        };

        ui.getSession().addUI(ui);
        ui.getSession().addUI(anotherUI);

        VaadinService service = ui.getSession().getService();
        VaadinServletContext context = ((VaadinServletContext) service
                .getContext());
        ServletContext servletContext = context.getContext();
        WebApplicationContext appContext = Mockito
                .mock(WebApplicationContext.class);
        Mockito.when(servletContext.getAttribute(
                WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
                .thenReturn(appContext);

        VaadinRouteScope scope = getScope();
        scope.getBeanStore();

        scope.uiInit(new UIInitEvent(ui, ui.getSession().getService()));

        AtomicInteger count = new AtomicInteger();
        scope.registerDestructionCallback("foo", () -> count.getAndIncrement());

        Object object = new Object();
        ObjectFactory factory = Mockito.mock(ObjectFactory.class);
        when(factory.getObject()).thenReturn(object);
        scope.get("foo", factory);

        ui.getSession().removeUI(ui);

        Assert.assertEquals(1, count.get());
    }

    @Test
    public void destroySession_sessionAttributeIsCleanedAndDestructionCallbackIsCalled() {
        UI ui = mockUI();

        VaadinService service = ui.getSession().getService();
        VaadinServletContext context = ((VaadinServletContext) service
                .getContext());
        ServletContext servletContext = context.getContext();

        WebApplicationContext appContext = Mockito
                .mock(WebApplicationContext.class);
        Mockito.when(servletContext.getAttribute(
                WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
                .thenReturn(appContext);

        SpringVaadinSession springSession = (SpringVaadinSession) VaadinSession
                .getCurrent();

        doCallRealMethod().when(springSession)
                .addDestroyListener(Mockito.any());

        doCallRealMethod().when(springSession).fireSessionDestroy();

        VaadinRouteScope scope = getScope();
        scope.getBeanStore();

        scope.uiInit(new UIInitEvent(ui, ui.getSession().getService()));

        AtomicInteger count = new AtomicInteger();
        scope.registerDestructionCallback("foo", () -> count.getAndIncrement());

        Object object = new Object();

        ObjectFactory factory = Mockito.mock(ObjectFactory.class);
        when(factory.getObject()).thenReturn(object);
        scope.get("foo", factory);

        String attribute = VaadinRouteScope.class.getName()
                + "$RouteStoreWrapper";

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

}
