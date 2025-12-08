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
package com.vaadin.flow.hotswap;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.BrowserLiveReloadAccessor;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.SessionInitEvent;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.server.startup.ApplicationConfigurationFactory;

import static com.vaadin.flow.hotswap.HotswapperTest.createMockVaadinSession;
import static com.vaadin.flow.hotswap.HotswapperTest.initUIAndNavigateTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;

public class HotswapperResourcesTest {

    private MockVaadinServletService service;
    private BrowserLiveReload liveReload;
    private Hotswapper hotswapper;
    private VaadinHotswapper flowHotswapper;
    private VaadinHotswapper hillaHotswapper;

    @Rule
    public TemporaryFolder tempProjectDir = new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
        service = new MockVaadinServletService();

        // Wire BrowserLiveReload into Lookup via BrowserLiveReloadAccessor
        liveReload = Mockito.mock(BrowserLiveReload.class);
        Mockito.when(
                service.getLookup().lookup(BrowserLiveReloadAccessor.class))
                .thenReturn(context -> liveReload);
        flowHotswapper = Mockito.mock(VaadinHotswapper.class);
        hillaHotswapper = Mockito.mock(VaadinHotswapper.class);
        Mockito.when(service.getLookup().lookupAll(VaadinHotswapper.class))
                .thenReturn(List.of(flowHotswapper, hillaHotswapper));

        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(appConfig.isProductionMode()).thenAnswer(
                i -> service.getDeploymentConfiguration().isProductionMode());
        Mockito.when(service.getLookup()
                .lookup(ApplicationConfigurationFactory.class))
                .thenReturn(context -> appConfig);

        hotswapper = new Hotswapper(service);
    }

    protected BrowserLiveReload getLiveReload() {
        return liveReload;
    }

    @Test
    public void resourceChange_noLiveReloadAvailable_noCrash()
            throws IOException {
        // Simulate no live reload
        liveReload = null;
        hotswapper = new Hotswapper(service);

        Mockito.doAnswer(i -> {
            i.getArgument(0, HotswapResourceEvent.class)
                    .sendHmrEvent("my-event", JacksonUtils.createObjectNode());
            return null;
        }).when(flowHotswapper)
                .onResourcesChange(any(HotswapResourceEvent.class));

        // Should not throw even though live reload is not available; just logs
        hotswapper.onHotswap(new URI[0],
                new URI[] { URI.create("/some/resources/somewhere.txt") },
                new URI[0]);

    }

    @Test
    public void onResourceHotswap_hotswapperRequestsReload_liveReloadTriggered()
            throws ServiceException {
        Mockito.doAnswer(i -> {
            i.getArgument(0, HotswapResourceEvent.class)
                    .triggerUpdate(UIUpdateStrategy.REFRESH);
            return null;
        }).when(flowHotswapper)
                .onResourcesChange(any(HotswapResourceEvent.class));
        Mockito.doAnswer(i -> {
            i.getArgument(0, HotswapResourceEvent.class)
                    .triggerUpdate(UIUpdateStrategy.RELOAD);
            return null;
        }).when(hillaHotswapper)
                .onResourcesChange(any(HotswapResourceEvent.class));
        VaadinSession session = createMockVaadinSession(service);
        hotswapper.sessionInit(new SessionInitEvent(service, session, null));
        HotswapperTest.RefreshTestingUI ui = initUIAndNavigateTo(service,
                session, HotswapperTest.MyRoute.class);

        hotswapper.onHotswap(new URI[0],
                new URI[] { URI.create("some-file.txt") }, new URI[0]);
        Mockito.verify(liveReload).reload();
        Mockito.verify(liveReload, never()).refresh(anyBoolean());
    }

    @Test
    public void onResourceHotswap_hotswapperRequestsRefresh_refreshTriggered()
            throws ServiceException {
        Mockito.doAnswer(i -> {
            i.getArgument(0, HotswapResourceEvent.class)
                    .triggerUpdate(UIUpdateStrategy.REFRESH);
            return null;
        }).when(flowHotswapper)
                .onResourcesChange(any(HotswapResourceEvent.class));
        VaadinSession session = createMockVaadinSession(service);
        hotswapper.sessionInit(new SessionInitEvent(service, session, null));
        HotswapperTest.RefreshTestingUI ui = initUIAndNavigateTo(service,
                session, HotswapperTest.MyRoute.class);

        hotswapper.onHotswap(new URI[0],
                new URI[] { URI.create("some-file.txt") }, new URI[0]);
        Mockito.verify(liveReload, never()).reload();
        Mockito.verify(liveReload).refresh(true);
    }

    @Test
    public void onHotswap_pushDisabled_hotswapperRequestsRefresh_UINotRefreshedButLiveReloadTriggered()
            throws ServiceException {
        VaadinSession session = createMockVaadinSession(service);
        hotswapper.sessionInit(new SessionInitEvent(service, session, null));
        HotswapperTest.RefreshTestingUI ui = initUIAndNavigateTo(service,
                session, HotswapperTest.MyRoute.class);

        Mockito.doAnswer(i -> {
            i.getArgument(0, HotswapResourceEvent.class)
                    .triggerUpdate(UIUpdateStrategy.REFRESH);
            return null;
        }).when(flowHotswapper)
                .onResourcesChange(any(HotswapResourceEvent.class));

        hotswapper.onHotswap(new URI[0],
                new URI[] { URI.create("some-file.txt") }, new URI[0]);
        ui.assertNotRefreshed();
        Mockito.verify(liveReload).refresh(true);
        Mockito.verify(liveReload, never()).reload();
    }

    @Test
    public void onHotswap_pushEnabled_hotswapperRequestRefresh_allUIsRefreshed()
            throws ServiceException {
        VaadinSession session = createMockVaadinSession(service);
        hotswapper.sessionInit(new SessionInitEvent(service, session, null));
        Mockito.doAnswer(i -> {
            i.getArgument(0, HotswapResourceEvent.class)
                    .triggerUpdate(UIUpdateStrategy.REFRESH);
            return null;
        }).when(flowHotswapper)
                .onResourcesChange(any(HotswapResourceEvent.class));

        HotswapperTest.RefreshTestingUI ui = initUIAndNavigateTo(service,
                session, HotswapperTest.MyRouteWithModal.class);
        ui.enablePush();

        hotswapper.onHotswap(new URI[0],
                new URI[] { URI.create("some-file.txt") }, new URI[0]);

        ui.assertChainRefreshed();
        Mockito.verify(liveReload, never()).reload();
        Mockito.verify(liveReload, never()).refresh(anyBoolean());
    }

}
