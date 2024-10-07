/*
 * Copyright 2000-2024 Vaadin Ltd.
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

package com.vaadin.flow.hotswap;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.BrowserLiveReloadAccessor;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.MockVaadinSession;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.ServiceDestroyEvent;
import com.vaadin.flow.server.ServiceDestroyListener;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.SessionDestroyEvent;
import com.vaadin.flow.server.SessionDestroyListener;
import com.vaadin.flow.server.SessionInitEvent;
import com.vaadin.flow.server.SessionInitListener;
import com.vaadin.flow.server.UIInitListener;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedSession;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.server.startup.ApplicationConfigurationFactory;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.tests.util.MockDeploymentConfiguration;
import com.vaadin.tests.util.MockUI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

public class HotswapperTest {

    Hotswapper hotswapper;
    Lookup lookup;
    private VaadinService service;
    private VaadinHotswapper flowHotswapper;
    private VaadinHotswapper hillaHotswapper;
    private BrowserLiveReload liveReload;

    @Before
    public void setup() {
        lookup = Mockito.mock(Lookup.class);
        service = new MockVaadinServletService();
        service.getContext().setAttribute(Lookup.class, lookup);

        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(appConfig.isProductionMode()).then(
                i -> service.getDeploymentConfiguration().isProductionMode());
        Mockito.when(lookup.lookup(ApplicationConfigurationFactory.class))
                .thenReturn(context -> appConfig);

        liveReload = Mockito.mock(BrowserLiveReload.class);
        Mockito.when(lookup.lookup(BrowserLiveReloadAccessor.class))
                .thenReturn(context -> liveReload);

        hotswapper = new Hotswapper(service);

        flowHotswapper = Mockito.mock(VaadinHotswapper.class);
        hillaHotswapper = Mockito.mock(VaadinHotswapper.class);
        Mockito.when(lookup.lookupAll(VaadinHotswapper.class))
                .thenReturn(List.of(flowHotswapper, hillaHotswapper));
    }

    @Test
    public void onHotswap_nullArguments_hotswappersNotInvoked() {
        hotswapper.onHotswap(null, true);
        Mockito.verifyNoInteractions(flowHotswapper, hillaHotswapper);
    }

    @Test
    public void onHotswap_emptyArguments_hotswappersNotInvoked() {
        hotswapper.onHotswap(new String[0], true);
        Mockito.verifyNoInteractions(flowHotswapper, hillaHotswapper);
    }

    @Test
    public void onHotswap_serviceDestroyed_hotswappersNotInvoked() {
        hotswapper.serviceDestroy(new ServiceDestroyEvent(service));
        hotswapper.onHotswap(new String[] { Integer.class.getName(),
                String.class.getName() }, true);
        Mockito.verifyNoInteractions(flowHotswapper, hillaHotswapper);
    }

    @Test
    public void onHotswap_noActiveSession_onlyGlobalHookCalled() {

        HashSet<Class<?>> classes = new HashSet<>(
                Set.of(Integer.class, String.class, java.io.File.class));
        hotswapper.onHotswap(toClassNameArray(classes), true);

        Mockito.verify(flowHotswapper).onClassLoadEvent(service, classes, true);
        Mockito.verify(flowHotswapper, never()).onClassLoadEvent(
                isA(VaadinSession.class), anySet(), anyBoolean());
        Mockito.verify(hillaHotswapper).onClassLoadEvent(service, classes,
                true);
        Mockito.verify(hillaHotswapper, never()).onClassLoadEvent(
                isA(VaadinSession.class), anySet(), anyBoolean());

        Mockito.reset(flowHotswapper, hillaHotswapper);

        classes = new HashSet<>(Set.of(BigDecimal.class, Long.class));
        hotswapper.onHotswap(toClassNameArray(classes), false);

        Mockito.verify(flowHotswapper).onClassLoadEvent(service, classes,
                false);
        Mockito.verify(flowHotswapper, never()).onClassLoadEvent(
                isA(VaadinSession.class), anySet(), anyBoolean());
        Mockito.verify(hillaHotswapper).onClassLoadEvent(service, classes,
                false);
        Mockito.verify(hillaHotswapper, never()).onClassLoadEvent(
                isA(VaadinSession.class), anySet(), anyBoolean());

    }

    @Test
    public void onHotswap_sessionHookCalledOnlyForActiveSessions()
            throws ServiceException {
        HashSet<Class<?>> classes = new HashSet<>(
                Set.of(Integer.class, String.class, java.io.File.class));
        VaadinSession sessionA = createMockVaadinSession();

        hotswapper.sessionInit(new SessionInitEvent(service, sessionA, null));

        hotswapper.onHotswap(toClassNameArray(classes), true);
        Mockito.verify(flowHotswapper).onClassLoadEvent(service, classes, true);
        Mockito.verify(flowHotswapper).onClassLoadEvent(sessionA, classes,
                true);
        Mockito.verify(hillaHotswapper).onClassLoadEvent(service, classes,
                true);
        Mockito.verify(hillaHotswapper).onClassLoadEvent(sessionA, classes,
                true);

        Mockito.reset(flowHotswapper, hillaHotswapper);
        VaadinSession sessionB = createMockVaadinSession();
        hotswapper.sessionInit(new SessionInitEvent(service, sessionB, null));
        hotswapper.onHotswap(toClassNameArray(classes), true);

        Mockito.verify(flowHotswapper).onClassLoadEvent(service, classes, true);
        Mockito.verify(flowHotswapper).onClassLoadEvent(sessionA, classes,
                true);
        Mockito.verify(flowHotswapper).onClassLoadEvent(sessionB, classes,
                true);
        Mockito.verify(hillaHotswapper).onClassLoadEvent(service, classes,
                true);
        Mockito.verify(hillaHotswapper).onClassLoadEvent(sessionA, classes,
                true);
        Mockito.verify(hillaHotswapper).onClassLoadEvent(sessionB, classes,
                true);

        Mockito.reset(flowHotswapper, hillaHotswapper);
        hotswapper.sessionDestroy(new SessionDestroyEvent(service, sessionA));
        hotswapper.onHotswap(toClassNameArray(classes), true);

        Mockito.verify(flowHotswapper).onClassLoadEvent(service, classes, true);
        Mockito.verify(flowHotswapper, never()).onClassLoadEvent(sessionA,
                classes, true);
        Mockito.verify(flowHotswapper).onClassLoadEvent(sessionB, classes,
                true);
        Mockito.verify(hillaHotswapper).onClassLoadEvent(service, classes,
                true);
        Mockito.verify(hillaHotswapper, never()).onClassLoadEvent(sessionA,
                classes, true);
        Mockito.verify(hillaHotswapper).onClassLoadEvent(sessionB, classes,
                true);

        Mockito.reset(flowHotswapper, hillaHotswapper);
        hotswapper.sessionDestroy(new SessionDestroyEvent(service, sessionB));
        hotswapper.onHotswap(toClassNameArray(classes), true);

        Mockito.verify(flowHotswapper).onClassLoadEvent(service, classes, true);
        Mockito.verify(flowHotswapper, never()).onClassLoadEvent(
                isA(VaadinSession.class), anySet(), anyBoolean());
        Mockito.verify(hillaHotswapper).onClassLoadEvent(service, classes,
                true);
        Mockito.verify(hillaHotswapper, never()).onClassLoadEvent(
                isA(VaadinSession.class), anySet(), anyBoolean());

    }

    @Test
    public void onHotswap_hotswapperFailure_doNotFail()
            throws ServiceException {
        VaadinSession sessionA = createMockVaadinSession();
        VaadinSession sessionB = createMockVaadinSession();
        hotswapper.sessionInit(new SessionInitEvent(service, sessionA, null));
        hotswapper.sessionInit(new SessionInitEvent(service, sessionB, null));

        doThrow(new RuntimeException("FLOW BOOM!!!")).when(flowHotswapper)
                .onClassLoadEvent(any(VaadinService.class), anySet(),
                        anyBoolean());
        doThrow(new RuntimeException("FLOW BOOM!!!")).when(hillaHotswapper)
                .onClassLoadEvent(same(sessionA), anySet(), anyBoolean());

        hotswapper.onHotswap(new String[] { String.class.getName() }, true);

        Mockito.verify(flowHotswapper).onClassLoadEvent(same(sessionA),
                anySet(), anyBoolean());
        Mockito.verify(flowHotswapper).onClassLoadEvent(same(sessionB),
                anySet(), anyBoolean());
        Mockito.verify(hillaHotswapper).onClassLoadEvent(same(service),
                anySet(), anyBoolean());
        Mockito.verify(hillaHotswapper).onClassLoadEvent(same(sessionB),
                anySet(), anyBoolean());
    }

    @Test
    public void onHotswap_forcedReload_liveReloadTriggered() {
        Mockito.when(flowHotswapper.onClassLoadEvent(any(VaadinService.class),
                anySet(), anyBoolean())).thenReturn(true);
        hotswapper.onHotswap(new String[] { String.class.getName() }, true);
        Mockito.verify(liveReload).reload();
    }

    @Test
    public void onHotswap_pushDisabled_routeClassChanged_UINotRefreshedButLiveReloadTriggered()
            throws ServiceException {
        VaadinSession session = createMockVaadinSession();
        hotswapper.sessionInit(new SessionInitEvent(service, session, null));
        RefreshTestingUI ui = initUIAndNavigateTo(session, MyRoute.class);

        hotswapper.onHotswap(new String[] { MyRoute.class.getName() }, true);

        ui.assertNotRefreshed();
        Mockito.verify(liveReload).refresh(anyBoolean());
    }

    @Test
    public void onHotswap_pushDisabled_routeLayoutClassChanged_UINotRefreshedButLiveReloadTriggered()
            throws ServiceException {
        VaadinSession session = createMockVaadinSession();
        hotswapper.sessionInit(new SessionInitEvent(service, session, null));
        RefreshTestingUI ui = initUIAndNavigateTo(session, MyRoute.class,
                MyLayout.class);

        hotswapper.onHotswap(new String[] { MyLayout.class.getName() }, true);

        ui.assertNotRefreshed();
        Mockito.verify(liveReload).refresh(anyBoolean());
    }

    @Test
    public void onHotswap_pushDisabled_autoLayoutClassChanged_UINotRefreshedButLiveReloadTriggered()
            throws ServiceException {
        VaadinSession session = createMockVaadinSession();
        hotswapper.sessionInit(new SessionInitEvent(service, session, null));
        RefreshTestingUI ui = initUIAndNavigateTo(session,
                MyAutoLayoutRoute.class);

        @Layout
        class AutoLayout extends Component implements RouterLayout {
        }
        ApplicationRouteRegistry.getInstance(service.getContext())
                .setLayout(AutoLayout.class);

        hotswapper.onHotswap(new String[] { AutoLayout.class.getName() }, true);

        ui.assertNotRefreshed();
        Mockito.verify(liveReload).refresh(anyBoolean());
    }

    @Test
    public void onHotswap_pushDisabled_parentAutoLayoutClassChanged_UINotRefreshedButLiveReloadTriggered()
            throws ServiceException {

        @Layout("level1")
        @ParentLayout(MyLayout.class)
        class AutoLayout extends Component implements RouterLayout {
        }

        VaadinSession session = createMockVaadinSession();
        hotswapper.sessionInit(new SessionInitEvent(service, session, null));
        RefreshTestingUI ui = initUIAndNavigateTo(session,
                MyAutoLayoutRoute.class, "level1/view");

        ApplicationRouteRegistry.getInstance(service.getContext())
                .setLayout(AutoLayout.class);

        hotswapper.onHotswap(new String[] { AutoLayout.class.getName() }, true);

        ui.assertNotRefreshed();
        Mockito.verify(liveReload).refresh(anyBoolean());
    }

    @Test
    public void onHotswap_pushDisabled_routeTargetChainChanged_UINotRefreshedButLiveReloadTriggered()
            throws ServiceException {
        VaadinSession session = createMockVaadinSession();
        hotswapper.sessionInit(new SessionInitEvent(service, session, null));

        class NewLayout extends Component implements RouterLayout {
        }
        RefreshTestingUI ui = initUIAndNavigateTo(session, MyRoute.class,
                MyLayout.class);
        RouteRegistry registry = ui.getInternals().getRouter().getRegistry();
        RouteConfiguration routeConfiguration = RouteConfiguration
                .forRegistry(registry);
        routeConfiguration.update(() -> {
            String path = ui.getActiveViewLocation().getPath();
            routeConfiguration.removeRoute(path);
            routeConfiguration.setRoute(path, MyRoute.class,
                    List.of(NewLayout.class));
        });

        hotswapper.onHotswap(new String[] { NewLayout.class.getName() }, true);

        ui.assertNotRefreshed();
        Mockito.verify(liveReload).refresh(anyBoolean());
    }

    @Test
    public void onHotswap_pushDisabled_routeChildClassChanged_UINotRefreshedButLiveReloadTriggered()
            throws ServiceException {
        VaadinSession session = createMockVaadinSession();
        hotswapper.sessionInit(new SessionInitEvent(service, session, null));
        RefreshTestingUI ui = initUIAndNavigateTo(session,
                MyRouteWithChild.class);

        hotswapper.onHotswap(new String[] { MyComponent.class.getName() },
                true);

        ui.assertNotRefreshed();
        Mockito.verify(liveReload).refresh(anyBoolean());
    }

    @Test
    public void onHotswap_pushDisabled_layoutChildClassChanged_UINotRefreshedButLiveReloadTriggered()
            throws ServiceException {
        VaadinSession session = createMockVaadinSession();
        hotswapper.sessionInit(new SessionInitEvent(service, session, null));
        RefreshTestingUI ui = initUIAndNavigateTo(session, MyRoute.class,
                MyLayoutWithChild.class);

        hotswapper.onHotswap(new String[] { MyComponent.class.getName() },
                true);

        ui.assertNotRefreshed();
        Mockito.verify(liveReload).refresh(anyBoolean());
    }

    @Test
    public void onHotswap_pushDisabled_routeAndLayoutClassesChanged_UINotRefreshedButLiveReloadTriggered()
            throws ServiceException {
        VaadinSession session = createMockVaadinSession();
        hotswapper.sessionInit(new SessionInitEvent(service, session, null));
        RefreshTestingUI ui = initUIAndNavigateTo(session, MyRoute.class,
                MyLayout.class);

        hotswapper.onHotswap(new String[] { MyRoute.class.getName(),
                MyLayout.class.getName() }, true);

        ui.assertNotRefreshed();
        Mockito.verify(liveReload).refresh(anyBoolean());
    }

    @Test
    public void onHotswap_pushDisabled_routeAndLayoutChildClassChanged_UINotRefreshedButLiveReloadTriggered()
            throws ServiceException {
        VaadinSession session = createMockVaadinSession();
        hotswapper.sessionInit(new SessionInitEvent(service, session, null));
        RefreshTestingUI ui = initUIAndNavigateTo(session,
                MyRouteWithChild.class, MyLayoutWithChild.class);

        hotswapper.onHotswap(new String[] { MyComponent.class.getName() },
                true);

        ui.assertNotRefreshed();
        Mockito.verify(liveReload).refresh(anyBoolean());
    }

    @Test
    public void onHotswap_pushDisabled_changedClassNotInUITree_skipLiveReloadAndUIRefresh()
            throws ServiceException {
        VaadinSession session = createMockVaadinSession();
        hotswapper.sessionInit(new SessionInitEvent(service, session, null));
        RefreshTestingUI ui = initUIAndNavigateTo(session, MyRoute.class,
                MyLayoutWithChild.class);

        hotswapper.onHotswap(new String[] { String.class.getName() }, true);

        ui.assertNotRefreshed();
        Mockito.verify(liveReload, never()).reload();
        Mockito.verify(liveReload, never()).refresh(anyBoolean());
    }

    @Test
    public void onHotswap_pushEnabled_routeClassChanged_routeRefreshed()
            throws ServiceException {
        VaadinSession session = createMockVaadinSession();
        hotswapper.sessionInit(new SessionInitEvent(service, session, null));

        RefreshTestingUI ui = initUIAndNavigateTo(session, MyRoute.class);
        ui.enablePush();

        hotswapper.onHotswap(new String[] { MyRoute.class.getName() }, true);

        ui.assertRouteRefreshed();
        Mockito.verify(liveReload, never()).reload();
        Mockito.verify(liveReload, never()).refresh(anyBoolean());
    }

    @Test
    public void onHotswap_pushEnabled_routeLayoutClassChanged_activeChainRefreshed()
            throws ServiceException {
        VaadinSession session = createMockVaadinSession();
        hotswapper.sessionInit(new SessionInitEvent(service, session, null));

        RefreshTestingUI ui = initUIAndNavigateTo(session, MyRoute.class,
                MyLayout.class);
        ui.enablePush();

        hotswapper.onHotswap(new String[] { MyLayout.class.getName() }, true);

        ui.assertChainRefreshed();
        Mockito.verify(liveReload, never()).reload();
        Mockito.verify(liveReload, never()).refresh(anyBoolean());
    }

    @Test
    public void onHotswap_pushEnabled_autoLayoutClassChanged_activeChainRefreshed()
            throws ServiceException {
        VaadinSession session = createMockVaadinSession();
        hotswapper.sessionInit(new SessionInitEvent(service, session, null));
        RefreshTestingUI ui = initUIAndNavigateTo(session,
                MyAutoLayoutRoute.class);
        ui.enablePush();

        @Layout
        class AutoLayout extends Component implements RouterLayout {
        }
        ApplicationRouteRegistry.getInstance(service.getContext())
                .setLayout(AutoLayout.class);

        hotswapper.onHotswap(new String[] { AutoLayout.class.getName() }, true);

        ui.assertChainRefreshed();
        Mockito.verify(liveReload, never()).reload();
        Mockito.verify(liveReload, never()).refresh(anyBoolean());
    }

    @Test
    public void onHotswap_pushEnabled_parentAutoLayoutClassChanged_activeChainRefreshed()
            throws ServiceException {

        @Layout("level1")
        @ParentLayout(MyLayout.class)
        class AutoLayout extends Component implements RouterLayout {
        }

        VaadinSession session = createMockVaadinSession();
        hotswapper.sessionInit(new SessionInitEvent(service, session, null));
        RefreshTestingUI ui = initUIAndNavigateTo(session,
                MyAutoLayoutRoute.class, "level1/view");
        ui.enablePush();

        ApplicationRouteRegistry.getInstance(service.getContext())
                .setLayout(AutoLayout.class);

        hotswapper.onHotswap(new String[] { AutoLayout.class.getName() }, true);

        ui.assertChainRefreshed();
        Mockito.verify(liveReload, never()).reload();
        Mockito.verify(liveReload, never()).refresh(anyBoolean());
    }

    @Test
    public void onHotswap_pushEnabled_routeTargetChainChanged_activeChainRefreshed()
            throws ServiceException {
        VaadinSession session = createMockVaadinSession();
        hotswapper.sessionInit(new SessionInitEvent(service, session, null));

        class NewLayout extends Component implements RouterLayout {
        }
        RefreshTestingUI ui = initUIAndNavigateTo(session, MyRoute.class,
                MyLayout.class);
        ui.enablePush();
        RouteRegistry registry = ui.getInternals().getRouter().getRegistry();
        RouteConfiguration routeConfiguration = RouteConfiguration
                .forRegistry(registry);
        routeConfiguration.update(() -> {
            String path = ui.getActiveViewLocation().getPath();
            routeConfiguration.removeRoute(path);
            routeConfiguration.setRoute(path, MyRoute.class,
                    List.of(NewLayout.class));
        });

        hotswapper.onHotswap(new String[] { NewLayout.class.getName() }, true);

        ui.assertChainRefreshed();
        Mockito.verify(liveReload, never()).reload();
        Mockito.verify(liveReload, never()).refresh(anyBoolean());
    }

    @Test
    public void onHotswap_pushEnabled_routeChildrenClassChanged_routeRefreshed()
            throws ServiceException {
        VaadinSession session = createMockVaadinSession();
        hotswapper.sessionInit(new SessionInitEvent(service, session, null));

        RefreshTestingUI ui = initUIAndNavigateTo(session,
                MyRouteWithChild.class);
        ui.enablePush();

        hotswapper.onHotswap(new String[] { MyComponent.class.getName() },
                true);

        ui.assertRouteRefreshed();
        Mockito.verify(liveReload, never()).reload();
        Mockito.verify(liveReload, never()).refresh(anyBoolean());
    }

    @Test
    public void onHotswap_pushEnabled_layoutChildrenClassChanged_activeChainRefreshed()
            throws ServiceException {
        VaadinSession session = createMockVaadinSession();
        hotswapper.sessionInit(new SessionInitEvent(service, session, null));

        RefreshTestingUI ui = initUIAndNavigateTo(session, MyRoute.class,
                MyLayoutWithChild.class);
        ui.enablePush();

        hotswapper.onHotswap(new String[] { MyComponent.class.getName() },
                true);

        ui.assertChainRefreshed();
        Mockito.verify(liveReload, never()).reload();
        Mockito.verify(liveReload, never()).refresh(anyBoolean());
    }

    @Test
    public void onHotswap_pushEnabled_routeAndLayoutClassesChanged_activeChainRefreshed()
            throws ServiceException {
        VaadinSession session = createMockVaadinSession();
        hotswapper.sessionInit(new SessionInitEvent(service, session, null));

        RefreshTestingUI ui = initUIAndNavigateTo(session, MyRoute.class,
                MyLayout.class);
        ui.enablePush();

        hotswapper.onHotswap(new String[] { MyRoute.class.getName(),
                MyLayout.class.getName() }, true);

        ui.assertChainRefreshed();
        Mockito.verify(liveReload, never()).reload();
        Mockito.verify(liveReload, never()).refresh(anyBoolean());
    }

    @Test
    public void onHotswap_pushEnabled_routeAndLayoutChildClassChanged_activeChainRefreshed()
            throws ServiceException {
        VaadinSession session = createMockVaadinSession();
        hotswapper.sessionInit(new SessionInitEvent(service, session, null));

        RefreshTestingUI ui = initUIAndNavigateTo(session,
                MyRouteWithChild.class, MyLayoutWithChild.class);
        ui.enablePush();

        hotswapper.onHotswap(new String[] { MyComponent.class.getName() },
                true);

        ui.assertChainRefreshed();
        Mockito.verify(liveReload, never()).reload();
        Mockito.verify(liveReload, never()).refresh(anyBoolean());
    }

    @Test
    public void onHotswap_pushEnabled_changedClassNotInUITree_skipRefresh()
            throws ServiceException {
        VaadinSession session = createMockVaadinSession();
        hotswapper.sessionInit(new SessionInitEvent(service, session, null));

        RefreshTestingUI ui = initUIAndNavigateTo(session,
                MyRouteWithChild.class, MyLayoutWithChild.class);
        ui.enablePush();

        hotswapper.onHotswap(new String[] { String.class.getName() }, true);

        ui.assertNotRefreshed();
        Mockito.verify(liveReload, never()).reload();
        Mockito.verify(liveReload, never()).refresh(anyBoolean());
    }

    @Test
    public void onHotswap_mixedPushState_classInUITreeChanged_liveReloadTriggered()
            throws ServiceException {
        VaadinSession session = createMockVaadinSession();
        hotswapper.sessionInit(new SessionInitEvent(service, session, null));
        RefreshTestingUI pushUI = initUIAndNavigateTo(session,
                MyRouteWithChild.class, MyLayoutWithChild.class);
        pushUI.enablePush();

        VaadinSession session2 = createMockVaadinSession();
        hotswapper.sessionInit(new SessionInitEvent(service, session2, null));
        RefreshTestingUI notPushUI = initUIAndNavigateTo(session2,
                MyRouteWithChild.class, MyLayoutWithChild.class);

        hotswapper.onHotswap(new String[] { MyComponent.class.getName() },
                true);

        pushUI.assertNotRefreshed();
        notPushUI.assertNotRefreshed();
        Mockito.verify(liveReload).refresh(anyBoolean());
    }

    @Test
    public void register_developmentMode_trackingListenerInstalled() {
        AtomicBoolean sessionInitInstalled = new AtomicBoolean();
        AtomicBoolean sessionDestroyInstalled = new AtomicBoolean();
        AtomicBoolean serviceDestroyInstalled = new AtomicBoolean();
        AtomicBoolean uiInitInstalled = new AtomicBoolean();
        MockDeploymentConfiguration configuration = new MockDeploymentConfiguration();
        configuration.setProductionMode(false);
        VaadinService vaadinService = new MockVaadinServletService(
                configuration) {
            @Override
            public Registration addSessionInitListener(
                    SessionInitListener listener) {
                sessionInitInstalled.set(true);
                return super.addSessionInitListener(listener);
            }

            @Override
            public Registration addSessionDestroyListener(
                    SessionDestroyListener listener) {
                sessionDestroyInstalled.set(true);
                return super.addSessionDestroyListener(listener);
            }

            @Override
            public Registration addServiceDestroyListener(
                    ServiceDestroyListener listener) {
                serviceDestroyInstalled.set(true);
                return super.addServiceDestroyListener(listener);
            }

            @Override
            public Registration addUIInitListener(UIInitListener listener) {
                uiInitInstalled.set(true);
                return super.addUIInitListener(listener);
            }
        };
        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(appConfig.isProductionMode()).then(i -> vaadinService
                .getDeploymentConfiguration().isProductionMode());
        Mockito.when(lookup.lookup(ApplicationConfigurationFactory.class))
                .thenReturn(context -> appConfig);
        vaadinService.getContext().setAttribute(Lookup.class, lookup);
        Hotswapper.register(vaadinService);

        Assert.assertTrue(
                "Expected hotswapper SessionInitListener to be registered in development mode, but was not",
                sessionInitInstalled.get());
        Assert.assertTrue(
                "Expected hotswapper SessionDestroyListener to be registered in development mode, but was not",
                sessionDestroyInstalled.get());
        Assert.assertTrue(
                "Expected hotswapper ServiceDestroyListener to be registered in development mode, but was not",
                serviceDestroyInstalled.get());
        Assert.assertTrue(
                "Expected hotswapper UIInitListener to be registered in development mode, but was not",
                uiInitInstalled.get());
    }

    @Test
    public void register_productionMode_trackingListenerNotInstalled() {
        AtomicBoolean sessionInitInstalled = new AtomicBoolean();
        AtomicBoolean sessionDestroyInstalled = new AtomicBoolean();
        AtomicBoolean serviceDestroyInstalled = new AtomicBoolean();
        AtomicBoolean uiInitInstalled = new AtomicBoolean();
        MockDeploymentConfiguration configuration = new MockDeploymentConfiguration();
        configuration.setProductionMode(true);
        VaadinService vaadinService = new MockVaadinServletService(
                configuration) {
            @Override
            public Registration addSessionInitListener(
                    SessionInitListener listener) {
                sessionInitInstalled.set(true);
                return super.addSessionInitListener(listener);
            }

            @Override
            public Registration addSessionDestroyListener(
                    SessionDestroyListener listener) {
                sessionDestroyInstalled.set(true);
                return super.addSessionDestroyListener(listener);
            }

            @Override
            public Registration addServiceDestroyListener(
                    ServiceDestroyListener listener) {
                serviceDestroyInstalled.set(true);
                return super.addServiceDestroyListener(listener);
            }
        };
        Hotswapper.register(vaadinService);

        Assert.assertFalse(
                "Expected hotswapper SessionInitListener not to be registered in production mode, but it was",
                sessionInitInstalled.get());
        Assert.assertFalse(
                "Expected hotswapper  SessionDestroyListener not to be registered in production mode, but it was",
                sessionDestroyInstalled.get());
        Assert.assertFalse(
                "Expected hotswapper  ServiceDestroyListener not to be registered in production mode, but it was",
                serviceDestroyInstalled.get());
        Assert.assertFalse(
                "Expected hotswapper  UIInitListener not to be registered in production mode, but it was",
                uiInitInstalled.get());
    }

    @Tag("my-route")
    public static class MyRoute extends Component {

    }

    @Tag("my-route-with-child")
    public static class MyRouteWithChild extends Component
            implements HasComponents {
        public MyRouteWithChild() {
            add(new MyComponent());
        }
    }

    @Tag("my-layout")
    public static class MyLayout extends Component implements RouterLayout {
    }

    @Tag("my-layout-with-child")
    public static class MyLayoutWithChild extends Component
            implements HasComponents, RouterLayout {

        @Override
        public void showRouterLayoutContent(HasElement content) {
            RouterLayout.super.showRouterLayoutContent(content);
            getElement().appendChild(new MyComponent().getElement());
        }
    }

    @Tag("my-nested-layout")
    public static class MyNestedLayout extends Component
            implements RouterLayout {
    }

    @Tag("my-nested-layout-with-child")
    public static class MyNestedLayoutWithChild extends Component
            implements HasComponents, RouterLayout {
        @Override
        public void showRouterLayoutContent(HasElement content) {
            RouterLayout.super.showRouterLayoutContent(content);
            getElement().appendChild(new MyComponent().getElement());
        }
    }

    @Tag("my-component")
    public static class MyComponent extends Component {

    }

    @Route("my-auto-layout")
    @Tag("my-auto-layout-route")
    public static class MyAutoLayoutRoute extends Component {

    }

    @SafeVarargs
    private RefreshTestingUI initUIAndNavigateTo(VaadinSession session,
            Class<? extends Component> route,
            Class<? extends RouterLayout>... parentChain) {
        return initUIAndNavigateTo(session, route, UUID.randomUUID().toString(),
                parentChain);
    }

    @SafeVarargs
    private RefreshTestingUI initUIAndNavigateTo(VaadinSession session,
            Class<? extends Component> route, String path,
            Class<? extends RouterLayout>... parentChain) {
        ApplicationRouteRegistry registry = ApplicationRouteRegistry
                .getInstance(service.getContext());
        registry.setRoute(path, route, List.of(parentChain));
        return withSessionLock(session, () -> {
            RefreshTestingUI ui = new RefreshTestingUI(session);
            ui.doInit(Mockito.mock(VaadinRequest.class), session.getNextUIid(),
                    UUID.randomUUID().toString());
            session.addUI(ui);
            ui.navigate(route);
            return ui;
        });
    }

    private static class RefreshTestingUI extends MockUI {

        private Boolean refreshRouteChainRequested;

        public RefreshTestingUI(VaadinSession session) {
            super(session);
        }

        @Override
        public void refreshCurrentRoute(boolean refreshRouteChain) {
            refreshRouteChainRequested = refreshRouteChain;
            super.refreshCurrentRoute(refreshRouteChain);
        }

        void assertNotRefreshed() {
            Assert.assertNull(
                    "Expecting refreshCurrentRoute not to be called, but was invoked",
                    refreshRouteChainRequested);
        }

        void assertRouteRefreshed() {
            Assert.assertNotNull(
                    "Expecting refreshCurrentRoute to be called but was not",
                    refreshRouteChainRequested);
            Assert.assertFalse(
                    "Expecting refreshCurrentRoute to refresh only route, but layout refresh was requested",
                    refreshRouteChainRequested);
        }

        void assertChainRefreshed() {
            Assert.assertNotNull(
                    "Expecting refreshCurrentRoute to be called but was not",
                    refreshRouteChainRequested);
            Assert.assertTrue(
                    "Expecting refreshCurrentRoute to refresh all chain, but only route refresh was requested",
                    refreshRouteChainRequested);
        }

        void enablePush() {
            this.accessSynchronously(() -> getPushConfiguration()
                    .setPushMode(PushMode.AUTOMATIC));
        }
    }

    private VaadinSession createMockVaadinSession() {
        WrappedSession wrappedSession = Mockito.mock(WrappedSession.class);
        when(wrappedSession.getId()).thenReturn(UUID.randomUUID().toString());

        MockVaadinSession session = new MockVaadinSession(service) {
            @Override
            public WrappedSession getSession() {
                return wrappedSession;
            }
        };
        session.getLockInstance().lock();
        session.setConfiguration(service.getDeploymentConfiguration());
        session.getLockInstance().unlock();
        return session;
    }

    private String[] toClassNameArray(Collection<Class<?>> classes) {
        return classes.stream().map(Class::getName).toArray(String[]::new);
    }

    private static <T> T withSessionLock(VaadinSession session,
            Supplier<T> op) {
        session.getLockInstance().lock();
        try {
            return op.get();
        } finally {
            session.getLockInstance().unlock();
        }
    }
}
