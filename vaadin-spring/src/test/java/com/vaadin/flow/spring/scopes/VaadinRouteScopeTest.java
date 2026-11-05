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

import jakarta.servlet.ServletContext;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.web.context.WebApplicationContext;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.ExtendedClientDetails;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.internal.AfterNavigationHandler;
import com.vaadin.flow.router.internal.BeforeEnterHandler;
import com.vaadin.flow.server.UIInitEvent;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.VaadinSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VaadinRouteScopeTest extends AbstractUIScopedTest {

    @Tag(Tag.A)
    public static class NavigationTarget extends Component {

    }

    @Tag(Tag.A)
    public static class AnotherNavigationTarget extends Component {

    }

    @Tag(Tag.A)
    @PreserveOnRefresh
    public static class PreservedNavigationTarget extends Component {

    }

    @Tag(Tag.A)
    @PreserveOnRefresh
    public static class PreservedLayout extends Component
            implements RouterLayout {

    }

    @Override
    protected VaadinRouteScope getScope() {
        VaadinRouteScope scope = new VaadinRouteScope();
        scope.postProcessBeanFactory(
                mock(ConfigurableListableBeanFactory.class));
        return scope;
    }

    @Test
    void detachUI_uisHaveNoWindowName_beanInScopeIsDestroyed() {
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

        assertEquals(1, count.get());
    }

    @Test
    void destroySession_sessionAttributeIsCleanedAndDestructionCallbackIsCalled() {
        UI ui = mockUI();

        mockServletContext(ui);

        VaadinSession session = VaadinSession.getCurrent();
        VaadinService service = session.getService();

        doCallRealMethod().when(session).addSessionDestroyListener(any());
        doCallRealMethod().when(session).getLockInstance();
        doCallRealMethod().when(session).getPendingAccessQueue();
        doCallRealMethod().when(session).access(any());

        VaadinRouteScope scope = initScope(ui);

        AtomicInteger count = new AtomicInteger();
        scope.registerDestructionCallback("foo", () -> count.getAndIncrement());

        ObjectFactory<?> factory = putObjectIntoScope(scope);

        String attribute = VaadinRouteScope.class.getName()
                + "$RouteStoreWrapper";

        // self control - the attribute name is used by the implementation
        assertNotNull(session.getAttribute(attribute));

        service.fireSessionDestroy(session);
        service.runPendingAccessTasks(session);

        assertEquals(1, count.get());
        assertNull(session.getAttribute(attribute));

        // Destruction callbacks are not called anymore (they are removed)
        scope.getBeanStore().destroy();
        assertEquals(1, count.get());

        // object has been removed from the storage, so object factory is called
        // once again to create the bean
        scope.get("foo", factory);
        verify(factory, times(2)).getObject();
    }

    @Test
    void refresh_preserveOnRefreshTarget_beanIsReusedAndDestroyedOnNavigationAway() {
        UI ui = mockUI();

        UI anotherUI = makeAnotherUI(ui);

        setWindowName("bar", ui, anotherUI);

        ui.getSession().addUI(ui);
        ui.getSession().addUI(anotherUI);

        mockServletContext(ui);

        VaadinRouteScope scope = initScope(ui);

        AtomicInteger count = new AtomicInteger();
        scope.registerDestructionCallback("foo", () -> count.getAndIncrement());

        navigateTo(ui, new PreservedNavigationTarget());

        ObjectFactory<?> factory = putObjectIntoScope(scope);

        // Refresh: Flow hands the preserved chain over to the new UI and
        // closes the first UI.
        ui.close();

        UI.setCurrent(anotherUI);

        scope = initScope(anotherUI);
        navigateTo(anotherUI, new PreservedNavigationTarget());

        // the refreshed UI gets the same bean
        scope.get("foo", factory);
        verify(factory, times(1)).getObject();

        // the bean is not removed with the first UI since the refreshed UI
        // hosts it
        UI.setCurrent(ui);
        ui.getSession().removeUI(ui);
        UI.setCurrent(anotherUI);
        assertEquals(0, count.get());

        navigateTo(anotherUI, new AnotherNavigationTarget());

        // the bean is removed since navigation away from it's owner navigation
        // target
        assertEquals(1, count.get());
    }

    @Test
    void refresh_previousUIStillOpen_beanIsNotShared() {
        UI ui = mockUI();

        UI anotherUI = makeAnotherUI(ui);

        setWindowName("bar", ui, anotherUI);

        VaadinSession session = ui.getSession();
        session.addUI(ui);
        session.addUI(anotherUI);

        mockServletContext(ui);

        VaadinRouteScope scope = initScope(ui);

        AtomicInteger count = new AtomicInteger();
        scope.registerDestructionCallback("foo", count::getAndIncrement);

        navigateTo(ui, new NavigationTarget());

        ObjectFactory<?> factory = putObjectIntoScope(scope);

        // A UI created for the same browser window while the previous one is
        // still open, e.g. after the browser tab crashed.
        UI.setCurrent(anotherUI);
        scope = initScope(anotherUI);
        navigateTo(anotherUI, new NavigationTarget());

        scope.get("foo", factory);
        verify(factory, times(2)).getObject();

        // The bean of the previous UI is destroyed with that UI
        UI.setCurrent(ui);
        session.removeUI(ui);
        assertEquals(1, count.get());
    }

    @Test
    void refresh_windowNameKnownLate_preservedBeanIsTakenOverOnRepeatedNavigation() {
        UI ui = mockUI();

        UI anotherUI = makeAnotherUI(ui);

        VaadinSession session = ui.getSession();
        session.addUI(ui);
        session.addUI(anotherUI);

        mockServletContext(ui);

        VaadinRouteScope scope = initScope(ui);

        AtomicInteger count = new AtomicInteger();
        scope.registerDestructionCallback("foo", count::getAndIncrement);

        navigateTo(ui, new PreservedNavigationTarget());
        ObjectFactory<?> factory = putObjectIntoScope(scope);

        setWindowName("bar", ui);
        scope.get("foo", factory);

        // The refreshed UI navigates before its window name is known, while
        // the first UI is already closing: no chain can be matched yet, so it
        // gets its own beans
        ui.close();
        UI.setCurrent(anotherUI);
        scope = initScope(anotherUI);
        AtomicInteger anotherCount = new AtomicInteger();
        scope.registerDestructionCallback("foo", anotherCount::getAndIncrement);
        navigateTo(anotherUI, new PreservedNavigationTarget());
        scope.get("foo", factory);
        verify(factory, times(2)).getObject();
        assertEquals(0, anotherCount.get());

        // Flow repeats the navigation once the window name is known and hands
        // the preserved chain over, which takes the preserved beans over
        setWindowName("bar", anotherUI);
        navigateTo(anotherUI, new PreservedNavigationTarget());
        assertEquals(1, anotherCount.get());

        scope.get("foo", factory);
        verify(factory, times(2)).getObject();
        assertEquals(0, count.get());
    }

    @Test
    void preservedChain_previousUIClosedOnHandOver_newUITakesOverPreservedBeans() {
        UI ui = mockUI();

        UI anotherUI = makeAnotherUI(ui);

        setWindowName("bar", ui, anotherUI);

        VaadinSession session = ui.getSession();
        session.addUI(ui);
        session.addUI(anotherUI);

        mockServletContext(ui);

        VaadinRouteScope scope = initScope(ui);

        AtomicInteger count = new AtomicInteger();
        scope.registerDestructionCallback("foo", count::getAndIncrement);

        navigateTo(ui, new PreservedNavigationTarget());
        ObjectFactory<?> factory = putObjectIntoScope(scope);

        // The previous UI is closing, but the new UI shows a chain that is not
        // preserved, so it gets its own beans
        ui.close();
        UI.setCurrent(anotherUI);
        VaadinRouteScope anotherScope = initScope(anotherUI);
        navigateTo(anotherUI, new NavigationTarget());

        AtomicInteger anotherCount = new AtomicInteger();
        anotherScope.registerDestructionCallback("foo",
                anotherCount::getAndIncrement);
        anotherScope.get("foo", factory);
        verify(factory, times(2)).getObject();

        // Entering the preserved chain of the closing UI takes over the
        // preserved beans, and the beans of the previous chain of the new UI
        // go out of scope
        navigateTo(anotherUI, new PreservedNavigationTarget());
        anotherScope.get("foo", factory);
        verify(factory, times(2)).getObject();
        assertEquals(1, anotherCount.get());
        assertEquals(0, count.get());
    }

    @Test
    void preservedChain_previousUIClosedOnPlainRoute_beanIsNotTakenOver() {
        UI ui = mockUI();

        UI anotherUI = makeAnotherUI(ui);

        setWindowName("bar", ui, anotherUI);

        VaadinSession session = ui.getSession();
        session.addUI(ui);
        session.addUI(anotherUI);

        mockServletContext(ui);

        VaadinRouteScope scope = initScope(ui);

        AtomicInteger count = new AtomicInteger();
        scope.registerDestructionCallback("foo", count::getAndIncrement);

        navigateTo(ui, new NavigationTarget());
        ObjectFactory<?> factory = putObjectIntoScope(scope);

        // A page reload closes the UI of a route that is not preserved, so
        // the new UI cannot have been handed its chain
        ui.close();
        UI.setCurrent(anotherUI);
        scope = initScope(anotherUI);
        navigateTo(anotherUI, new PreservedNavigationTarget());
        scope.get("foo", factory);
        verify(factory, times(2)).getObject();

        // The bean of the previous UI stays with that UI until it is removed
        assertEquals(0, count.get());
        UI.setCurrent(ui);
        session.removeUI(ui);
        assertEquals(1, count.get());
    }

    @Test
    void preservedChain_previousUIStillOpenAndChainNotHandedOver_newUIGetsOwnBeans() {
        UI ui = mockUI();

        UI anotherUI = makeAnotherUI(ui);

        setWindowName("bar", ui, anotherUI);

        VaadinSession session = ui.getSession();
        session.addUI(ui);
        session.addUI(anotherUI);

        mockServletContext(ui);

        VaadinRouteScope scope = initScope(ui);

        AtomicInteger count = new AtomicInteger();
        scope.registerDestructionCallback("foo", count::getAndIncrement);

        navigateTo(ui, new PreservedNavigationTarget());
        ObjectFactory<?> factory = putObjectIntoScope(scope);

        // The tab crashed and is reloaded to a location of the same route that
        // has no cached chain, e.g. with other route parameters, so Flow builds
        // a new chain and leaves the previous UI open with its own components.
        UI.setCurrent(anotherUI);
        VaadinRouteScope anotherScope = initScope(anotherUI);
        navigateTo(anotherUI, new PreservedNavigationTarget());

        AtomicInteger anotherCount = new AtomicInteger();
        anotherScope.registerDestructionCallback("foo",
                anotherCount::getAndIncrement);
        anotherScope.get("foo", factory);
        verify(factory, times(2)).getObject();

        // A refresh hands the chain of the new UI over to a third UI while the
        // previous UI is still open: the third UI gets the beans of the
        // chain it shows
        UI thirdUI = makeAnotherUI(anotherUI);
        setWindowName("bar", thirdUI);
        session.addUI(thirdUI);
        anotherUI.close();
        UI.setCurrent(thirdUI);
        VaadinRouteScope thirdScope = initScope(thirdUI);
        navigateTo(thirdUI, new PreservedNavigationTarget());
        thirdScope.get("foo", factory);
        verify(factory, times(2)).getObject();

        UI.setCurrent(anotherUI);
        session.removeUI(anotherUI);
        assertEquals(0, anotherCount.get());

        // The beans of the previous UI are destroyed with that UI
        UI.setCurrent(ui);
        session.removeUI(ui);
        assertEquals(1, count.get());
        assertEquals(0, anotherCount.get());
    }

    @Test
    void navigate_chainStopsBeingPreserved_beanIsKeptAndDestroyedOnUIDetach() {
        UI ui = mockUI();

        // A stale UI of the same browser window, e.g. the one before a refresh
        UI staleUI = makeAnotherUI(ui);

        setWindowName("bar", ui, staleUI);

        VaadinSession session = ui.getSession();
        session.addUI(staleUI);
        session.addUI(ui);

        mockServletContext(ui);

        VaadinRouteScope scope = initScope(ui);

        AtomicInteger count = new AtomicInteger();
        scope.registerDestructionCallback("foo", count::getAndIncrement);

        NavigationTarget target = new NavigationTarget();
        navigateTo(ui, target);
        ObjectFactory<?> factory = putObjectIntoScope(scope);

        // The owner stays in the chain while it starts and then stops being
        // preserved on refresh
        navigateTo(ui, target, PreservedLayout.class);
        scope.get("foo", factory);
        navigateTo(ui, target);
        scope.get("foo", factory);

        verify(factory, times(1)).getObject();
        assertEquals(0, count.get());

        session.removeUI(ui);
        assertEquals(1, count.get());
    }

    @Test
    void detachUI_uiWithDifferentWindowName_beanInScopeIsDestroyedwhenUIIsDetached() {
        UI ui = mockUI();

        UI anotherUI = makeAnotherUI(ui);

        ExtendedClientDetails details = mock(ExtendedClientDetails.class);
        when(details.getWindowName()).thenReturn("bar");
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
        assertEquals(1, count.get());
        count.set(0);

        UI.setCurrent(anotherUI);

        scope = initScope(anotherUI);

        navigateTo(anotherUI, new AnotherNavigationTarget());

        // the bean is not removed since it's already has been removed when the
        // first UI is detached.
        assertEquals(0, count.get());
    }

    @Test
    void detachUI_refreshedUIDetachedLast_beanIsDestroyedWhenRefreshedUIIsDetached() {
        UI ui = mockUI();

        UI anotherUI = makeAnotherUI(ui);

        setWindowName("bar", ui, anotherUI);

        VaadinSession session = ui.getSession();
        session.addUI(ui);
        session.addUI(anotherUI);

        mockServletContext(ui);

        VaadinRouteScope scope = initScope(ui);

        AtomicInteger count = new AtomicInteger();
        scope.registerDestructionCallback("foo", () -> count.getAndIncrement());

        navigateTo(ui, new PreservedNavigationTarget());

        putObjectIntoScope(scope);

        // Refresh: Flow hands the preserved chain over to the new UI and
        // closes the first UI.
        ui.close();
        UI.setCurrent(anotherUI);
        initScope(anotherUI);
        navigateTo(anotherUI, new PreservedNavigationTarget());

        // The stale UI is closed later on, e.g. by the inactive UI cleanup.
        UI.setCurrent(ui);
        session.removeUI(ui);

        // the bean is not removed since the refreshed UI still hosts it
        assertEquals(0, count.get());

        // Detaching the refreshed UI must destroy the bean.
        UI.setCurrent(anotherUI);
        session.removeUI(anotherUI);

        assertEquals(1, count.get());
    }

    @SafeVarargs
    private void navigateTo(UI ui, Component component,
            Class<? extends RouterLayout>... layouts) {
        BeforeEnterEvent beforeEnterEvent = mock(BeforeEnterEvent.class);
        doReturn(component.getClass()).when(beforeEnterEvent)
                .getNavigationTarget();
        when(beforeEnterEvent.getLayouts()).thenReturn(List.of(layouts));
        ui.getNavigationListeners(BeforeEnterHandler.class)
                .forEach(listener -> listener.beforeEnter(beforeEnterEvent));

        AfterNavigationEvent event = mock(AfterNavigationEvent.class);
        when(event.getActiveChain())
                .thenReturn(Collections.singletonList(component));
        List<AfterNavigationHandler> navigationListeners = ui
                .getNavigationListeners(AfterNavigationHandler.class);
        navigationListeners
                .forEach(listener -> listener.afterNavigation(event));
    }

    private void setWindowName(String windowName, UI... uis) {
        ExtendedClientDetails details = mock(ExtendedClientDetails.class);
        when(details.getWindowName()).thenReturn(windowName);
        for (UI ui : uis) {
            ui.getInternals().setExtendedClientDetails(details);
        }
    }

    private ObjectFactory<?> putObjectIntoScope(VaadinRouteScope scope) {
        Object object = new Object();
        @SuppressWarnings("rawtypes")
        ObjectFactory factory = mock(ObjectFactory.class);
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
        WebApplicationContext appContext = mock(WebApplicationContext.class);
        when(servletContext.getAttribute(
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
