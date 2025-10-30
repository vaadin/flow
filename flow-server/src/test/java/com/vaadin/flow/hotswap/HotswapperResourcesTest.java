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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.BrowserLiveReloadAccessor;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.server.startup.ApplicationConfigurationFactory;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.tests.util.MockDeploymentConfiguration;

public class HotswapperResourcesTest {

    private MockVaadinServletService service;
    private BrowserLiveReload liveReload;
    private Hotswapper hotswapper;

    @Rule
    public TemporaryFolder tempProjectDir = new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
        MockDeploymentConfiguration dc = new MockDeploymentConfiguration();
        // Use TemporaryFolder for the project directory required for build
        // resources
        dc.setProjectFolder(tempProjectDir.getRoot());

        service = new MockVaadinServletService(dc);

        // Wire BrowserLiveReload into Lookup via BrowserLiveReloadAccessor
        liveReload = Mockito.mock(BrowserLiveReload.class);
        Mockito.when(
                service.getLookup().lookup(BrowserLiveReloadAccessor.class))
                .thenReturn(context -> liveReload);

        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(appConfig.isProductionMode()).thenAnswer(
                i -> service.getDeploymentConfiguration().isProductionMode());
        Mockito.when(service.getLookup()
                .lookup(ApplicationConfigurationFactory.class))
                .thenReturn(context -> appConfig);

        hotswapper = new Hotswapper(service);
    }

    @Test
    public void cssResourceChange_triggersLiveReloadUpdateWithRelativePath()
            throws Exception {
        File buildResources = service.getDeploymentConfiguration()
                .getOutputResourceFolder();
        // Mimic a static resources folder under build resources
        File publicDir = new File(buildResources, "public");
        File css = new File(publicDir, "styles/app.css");
        css.getParentFile().mkdirs();
        Files.writeString(css.toPath(), "body{}\n");

        URI modified = css.toURI();
        hotswapper.onHotswap(new URI[0], new URI[] { modified }, new URI[0]);

        // Expect BrowserLiveReload.update to be called with relative URL path
        // "styles/app.css"
        Mockito.verify(liveReload).update(
                ApplicationConstants.CONTEXT_PROTOCOL_PREFIX + "styles/app.css",
                null);
        Mockito.verifyNoMoreInteractions(liveReload);
    }

    @Test
    public void cssResourceChange_noLiveReloadAvailable_noCrash()
            throws Exception {
        // Create a new service without BrowserLiveReload in Lookup
        MockDeploymentConfiguration dc = new MockDeploymentConfiguration();
        dc.setProjectFolder(tempProjectDir.getRoot());
        MockVaadinServletService serviceNoLR = new MockVaadinServletService(dc);
        // Provide ApplicationConfiguration via factory to avoid NPE in
        // BrowserLiveReloadAccessor
        com.vaadin.flow.server.startup.ApplicationConfiguration appConfig = Mockito
                .mock(com.vaadin.flow.server.startup.ApplicationConfiguration.class);
        Mockito.when(appConfig.isProductionMode()).thenAnswer(i -> serviceNoLR
                .getDeploymentConfiguration().isProductionMode());
        Mockito.when(serviceNoLR.getLookup().lookup(
                com.vaadin.flow.server.startup.ApplicationConfigurationFactory.class))
                .thenReturn(context -> appConfig);

        Hotswapper noLR = new Hotswapper(serviceNoLR);

        File buildResources = serviceNoLR.getDeploymentConfiguration()
                .getOutputResourceFolder();
        File staticDir = new File(buildResources, "static");
        File css = new File(staticDir, "theme.css");
        css.getParentFile().mkdirs();
        Files.writeString(css.toPath(), "html{}\n");

        // Should not throw even though live reload is not available; just logs
        noLR.onHotswap(new URI[0], new URI[] { css.toURI() }, new URI[0]);
    }
}
