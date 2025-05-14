/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import jakarta.servlet.ServletContext;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.web.context.WebApplicationContext;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.ExtendedClientDetails;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.internal.AfterNavigationHandler;
import com.vaadin.flow.server.UIInitEvent;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.VaadinSession;

import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VaadinRouteScopeTest extends AbstractUIScopedTest {

    @Tag(Tag.A)
    public static class NavigationTarget extends Component {

    }

    @Tag(Tag.A)
    public static class AnotherNavigationTarget extends Component {

    }

    @Override
    protected VaadinRouteScope getScope() {
        VaadinRouteScope scope = new VaadinRouteScope();
        scope.postProcessBeanFactory(
                Mockito.mock(ConfigurableListableBeanFactory.class));
        return scope;
    }

    @Test
    public void detachUI_uisHaveNoWindowName_beanInScopeIsDestroyed() {
        UI ui = mockUI();

        UI anotherUI = makeAnotherUI(ui);

        ui.getSession().addUI(ui);
        ui.getSession().addUI(anotherUI);

        mockServletContext(ui);

        VaadinRouteScope scope = initScope(ui);
        AtomicInteger count = new AtomicInteger();
        scope.registerDestructionCallback("foo", () -> count.getAndIncrement());

        putObjectIntoScope(scope);

        ui.getSession().removeUI(ui);

        Assert.assertEquals(1, count.get());
    }

    @Test
    public void destroySession_sessionAttributeIsCleanedAndDestructionCallbackIsCalled() {
        UI ui = mockUI();

        mockServletContext(ui);

        VaadinSession session = VaadinSession.getCurrent();
        VaadinService service = session.getService();

        doCallRealMethod().when(session)
                .addSessionDestroyListener(Mockito.any());
        doCallRealMethod().when(session).getLockInstance();
        doCallRealMethod().when(session).getPendingAccessQueue();
        doCallRealMethod().when(session).access(Mockito.any());

        VaadinRouteScope scope = initScope(ui);

        AtomicInteger count = new AtomicInteger();
        scope.registerDestructionCallback("foo", () -> count.getAndIncrement());

        ObjectFactory<?> factory = putObjectIntoScope(scope);

        String attribute = VaadinRouteScope.class.getName()
                + "$RouteStoreWrapper";

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

    @Test
    public void refresh_uiWithTheSameWindowName_beanInScopeIsDestroyedAfterRefresh() {
        UI ui = mockUI();

        UI anotherUI = makeAnotherUI(ui);

        ExtendedClientDetails details = Mockito
                .mock(ExtendedClientDetails.class);
        Mockito.when(details.getWindowName()).thenReturn("bar");
        ui.getInternals().setExtendedClientDetails(details);
        anotherUI.getInternals().setExtendedClientDetails(details);

        ui.getSession().addUI(ui);
        ui.getSession().addUI(anotherUI);

        mockServletContext(ui);

        VaadinRouteScope scope = initScope(ui);

        AtomicInteger count = new AtomicInteger();
        scope.registerDestructionCallback("foo", () -> count.getAndIncrement());

        new VaadinRouteScope.NavigationListenerRegistrar()
                .uiInit(new UIInitEvent(ui, ui.getSession().getService()));

        navigateTo(ui, new NavigationTarget());

        putObjectIntoScope(scope);

        // close the first UI
        ui.getSession().removeUI(ui);

        // the bean is not removed since there is a "preserved" UI
        Assert.assertEquals(0, count.get());

        UI.setCurrent(anotherUI);

        scope = initScope(anotherUI);

        // the bean is not removed since there is a "preserved" UI
        Assert.assertEquals(0, count.get());

        navigateTo(anotherUI, new AnotherNavigationTarget());

        // the bean is removed since navigation away from it's owner navigation
        // target
        Assert.assertEquals(1, count.get());
    }

    @Test
    public void detachUI_uiWithDifferentWindowName_beanInScopeIsDestroyedwhenUIIsDetached() {
        UI ui = mockUI();

        UI anotherUI = makeAnotherUI(ui);

        ExtendedClientDetails details = Mockito
                .mock(ExtendedClientDetails.class);
        Mockito.when(details.getWindowName()).thenReturn("bar");
        ui.getInternals().setExtendedClientDetails(details);

        ui.getSession().addUI(ui);
        ui.getSession().addUI(anotherUI);

        mockServletContext(ui);

        VaadinRouteScope scope = initScope(ui);

        AtomicInteger count = new AtomicInteger();
        scope.registerDestructionCallback("foo", () -> count.getAndIncrement());

        new VaadinRouteScope.NavigationListenerRegistrar()
                .uiInit(new UIInitEvent(ui, ui.getSession().getService()));

        navigateTo(ui, new NavigationTarget());

        putObjectIntoScope(scope);

        // close the first UI
        ui.getSession().removeUI(ui);

        // the bean is removed since there is no UI with the window name "bar"
        // present.
        Assert.assertEquals(1, count.get());
        count.set(0);

        UI.setCurrent(anotherUI);

        scope = initScope(anotherUI);

        navigateTo(anotherUI, new AnotherNavigationTarget());

        // the bean is not removed since it's already has been removed when the
        // first UI is detached.
        Assert.assertEquals(0, count.get());
    }

    private void navigateTo(UI ui, Component component) {
        AfterNavigationEvent event = Mockito.mock(AfterNavigationEvent.class);
        Mockito.when(event.getActiveChain())
                .thenReturn(Collections.singletonList(component));
        List<AfterNavigationHandler> navigationListeners = ui
                .getNavigationListeners(AfterNavigationHandler.class);
        navigationListeners
                .forEach(listener -> listener.afterNavigation(event));
    }

    private ObjectFactory<?> putObjectIntoScope(VaadinRouteScope scope) {
        Object object = new Object();
        @SuppressWarnings("rawtypes")
        ObjectFactory factory = Mockito.mock(ObjectFactory.class);
        when(factory.getObject()).thenReturn(object);
        scope.get("foo", factory);
        return factory;
    }

    private VaadinRouteScope initScope(UI ui) {
        VaadinRouteScope scope = getScope();
        scope.getBeanStore();

        new VaadinRouteScope.NavigationListenerRegistrar()
                .uiInit(new UIInitEvent(ui, ui.getSession().getService()));
        return scope;
    }

    private void mockServletContext(UI ui) {
        VaadinService service = ui.getSession().getService();
        VaadinServletContext context = ((VaadinServletContext) service
                .getContext());
        ServletContext servletContext = context.getContext();
        WebApplicationContext appContext = Mockito
                .mock(WebApplicationContext.class);
        Mockito.when(servletContext.getAttribute(
                WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
                .thenReturn(appContext);

    }

    private UI makeAnotherUI(UI ui) {
        UI anotherUI = new UI() {

            @Override
            public int getUIId() {
                return ui.getUIId() + 1;
            }

        };

        anotherUI.getInternals().setSession(ui.getSession());
        return anotherUI;
    }

}
