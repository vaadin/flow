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
package com.vaadin.flow.component.dependency;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.hotswap.HotswapResourceEvent;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.server.startup.ApplicationConfigurationFactory;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.tests.util.MockDeploymentConfiguration;
import com.vaadin.tests.util.MockUI;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class StyleSheetHotswapperTest {

    StyleSheetHotswapper hotswapper = new StyleSheetHotswapper();
    MockVaadinServletService service;

    @Rule
    public TemporaryFolder tempProjectDir = new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
        MockDeploymentConfiguration dc = new MockDeploymentConfiguration();
        // Use TemporaryFolder for the project directory required for build
        // resources
        dc.setProjectFolder(tempProjectDir.getRoot());

        service = new MockVaadinServletService(dc);

        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(appConfig.isProductionMode()).thenAnswer(
                i -> service.getDeploymentConfiguration().isProductionMode());
        Mockito.when(service.getLookup()
                .lookup(ApplicationConfigurationFactory.class))
                .thenReturn(context -> appConfig);
    }

    @Test
    public void resourceChanged_notCss_ignore() throws IOException {
        assertLiveReloadNotTriggered("META-INF/resources", "foo.txt");
        assertLiveReloadNotTriggered("resources", "foo.txt");
        assertLiveReloadNotTriggered("public", "foo.txt");
        assertLiveReloadNotTriggered("static", "foo.txt");
    }

    @Test
    public void resourceChanged_cssNotIKnownPublicPath_ignore()
            throws IOException {
        assertLiveReloadNotTriggered("assets", "foo.css");
    }

    @Test
    public void cssResourceChange_knownPublicPaths_triggersLiveReloadUpdateWithRelativePath()
            throws Exception {
        assertLiveReloadTriggered("META-INF/resources");
        assertLiveReloadTriggered("resources");
        assertLiveReloadTriggered("public");
        assertLiveReloadTriggered("static");
    }

    private void assertLiveReloadNotTriggered(String resourceBasePath,
            String resourcePath) throws IOException {
        File css = createResource(resourceBasePath + "/" + resourcePath);

        URI modified = css.toURI();
        HotswapResourceEvent event = spy(
                new HotswapResourceEvent(service, Set.of(modified)));
        hotswapper.onResourcesChange(event);

        assertFalse("Page reload is not necessary",
                event.anyUIRequiresPageReload());
        assertTrue("Should not refresh UIs",
                event.getUIUpdateStrategy(new MockUI()).isEmpty());

        verify(event, never()).updateClientResource(anyString(), any());
    }

    private void assertLiveReloadTriggered(String resourceBasePath)
            throws IOException {
        File css = createResource(resourceBasePath + "/styles/app.css");

        URI modified = css.toURI();
        HotswapResourceEvent event = spy(
                new HotswapResourceEvent(service, Set.of(modified)));
        hotswapper.onResourcesChange(event);

        assertFalse("Page reload is not necessary",
                event.anyUIRequiresPageReload());
        assertTrue("Should not refresh UIs",
                event.getUIUpdateStrategy(new MockUI()).isEmpty());

        // Expect BrowserLiveReload.update to be called with relative URL path
        // "styles/app.css"
        verify(event).updateClientResource(
                ApplicationConstants.CONTEXT_PROTOCOL_PREFIX + "styles/app.css",
                null);
    }

    private File createResource(String resourcePath) throws IOException {
        File buildResources = service.getDeploymentConfiguration()
                .getOutputResourceFolder();
        // Mimic a static resources folder under build resources
        File css = new File(buildResources, resourcePath);
        css.getParentFile().mkdirs();
        Files.writeString(css.toPath(), "body{}\n");
        return css;
    }

}
