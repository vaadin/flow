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
package com.vaadin.base.devserver;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.internal.ActiveStyleSheetTracker;
import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.BrowserLiveReloadAccessor;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Verifies that PublicResourcesLiveUpdater reacts to a CSS change and bundles
 * imported files, pushing the merged result via BrowserLiveReload.update.
 */
public class PublicResourcesLiveUpdaterTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private MockedStatic<ApplicationConfiguration> appConfigStatic;
    private MockedStatic<BrowserLiveReloadAccessor> liveReloadAccessorStatic;

    @After
    public void cleanupStatics() {
        if (appConfigStatic != null) {
            appConfigStatic.close();
        }
        if (liveReloadAccessorStatic != null) {
            liveReloadAccessorStatic.close();
        }
    }

    @Test
    public void cssChange_triggersBundlingAndUpdate_forActiveUrl()
            throws Exception {
        // Arrange a fake project structure with public resources
        File project = temporaryFolder.newFolder("project");
        File publicRoot = new File(project, "src/main/resources/public");
        assertTrue(publicRoot.mkdirs());

        File mainCss = new File(publicRoot, "main.css");
        File importedCss = new File(publicRoot, "imported.css");

        String importedV1 = ".imp{background:blue;}";
        String mainContent = "@import './imported.css';\n.main{color:red;}\n";
        Files.writeString(importedCss.toPath(), importedV1,
                StandardCharsets.UTF_8);
        Files.writeString(mainCss.toPath(), mainContent,
                StandardCharsets.UTF_8);

        // Mock ApplicationConfiguration.get(context) -> project folder
        ApplicationConfiguration config = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(config.getProjectFolder()).thenReturn(project);
        appConfigStatic = Mockito.mockStatic(ApplicationConfiguration.class);

        // Use the lightweight MockVaadinContext in this module
        VaadinContext context = new MockVaadinContext();
        appConfigStatic
                .when(() -> ApplicationConfiguration.get(Mockito.eq(context)))
                .thenReturn(config);

        // Mock BrowserLiveReload available from context
        BrowserLiveReload liveReload = Mockito.mock(BrowserLiveReload.class);
        liveReloadAccessorStatic = Mockito
                .mockStatic(BrowserLiveReloadAccessor.class);
        liveReloadAccessorStatic
                .when(() -> BrowserLiveReloadAccessor
                        .getLiveReloadFromContext(Mockito.eq(context)))
                .thenReturn(Optional.of(liveReload));

        // Register active stylesheet URL so updater knows what to rebundle
        ActiveStyleSheetTracker.get(context)
                .trackForAppShell(Set.of("context://main.css"));

        // Start the updater watching the public root

        try (PublicResourcesLiveUpdater ignored = new PublicResourcesLiveUpdater(
                List.of(publicRoot.getAbsolutePath()), context)) {
            // Act: modify the imported file to trigger the watcher
            String importedV2 = ".imp{background:green;}";
            Files.writeString(importedCss.toPath(), importedV2,
                    StandardCharsets.UTF_8);

            // Assert: BrowserLiveReload.update("context://main.css",
            // mergedContent) is called
            // with both imported and main rules present, with imported first.
            Awaitility.await().untilAsserted(() -> Mockito
                    .verify(liveReload, Mockito.atLeastOnce())
                    .update(eq("context://main.css"), argThat(content -> {
                        if (content == null)
                            return false;
                        String normalized = content.replaceAll("\\s+", " ")
                                .trim();
                        int idxImp = normalized
                                .indexOf(".imp{background:green;}");
                        int idxMain = normalized.indexOf(".main{color:red;}");
                        return idxImp >= 0 && idxMain >= 0 && idxImp < idxMain;
                    })));
        }
    }

    @Test
    public void cssChange_triggersUpdates_forMultipleActiveUrls()
            throws Exception {
        // Arrange a fake project structure with public resources
        File project = temporaryFolder.newFolder("project2");
        File publicRoot = new File(project, "src/main/resources/public");
        assertTrue(publicRoot.mkdirs());

        File importedCss = new File(publicRoot, "shared.css");
        Files.writeString(importedCss.toPath(), ".shared{padding:0;}",
                StandardCharsets.UTF_8);

        File main1 = new File(publicRoot, "a.css");
        File main2 = new File(publicRoot, "b.css");
        Files.writeString(main1.toPath(), "@import './shared.css';\n.a{c:1;}",
                StandardCharsets.UTF_8);
        Files.writeString(main2.toPath(), "@import './shared.css';\n.b{c:2;}",
                StandardCharsets.UTF_8);

        // Mock ApplicationConfiguration.get(context) -> project folder
        ApplicationConfiguration config = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(config.getProjectFolder()).thenReturn(project);
        appConfigStatic = Mockito.mockStatic(ApplicationConfiguration.class);

        // Use the lightweight MockVaadinContext in this module
        VaadinContext context = new MockVaadinContext();
        appConfigStatic
                .when(() -> ApplicationConfiguration.get(Mockito.eq(context)))
                .thenReturn(config);

        // Mock BrowserLiveReload available from context
        BrowserLiveReload liveReload = Mockito.mock(BrowserLiveReload.class);
        liveReloadAccessorStatic = Mockito
                .mockStatic(BrowserLiveReloadAccessor.class);
        liveReloadAccessorStatic
                .when(() -> BrowserLiveReloadAccessor
                        .getLiveReloadFromContext(Mockito.eq(context)))
                .thenReturn(Optional.of(liveReload));

        // Register two active stylesheet URLs
        ActiveStyleSheetTracker.get(context)
                .trackForAppShell(Set.of("context://a.css", "context://b.css"));

        try (PublicResourcesLiveUpdater ignored = new PublicResourcesLiveUpdater(
                List.of(publicRoot.getAbsolutePath()), context)) {
            // Change shared import
            Files.writeString(importedCss.toPath(), ".shared{padding:4px;}",
                    StandardCharsets.UTF_8);

            // Assert we push updates for both a.css and b.css
            Awaitility.await().untilAsserted(() -> {
                Mockito.verify(liveReload, Mockito.atLeastOnce()).update(
                        eq("context://a.css"),
                        argThat(c -> c != null && c.contains(".a{c:1;}")));
                Mockito.verify(liveReload, Mockito.atLeastOnce()).update(
                        eq("context://b.css"),
                        argThat(c -> c != null && c.contains(".b{c:2;}")));
            });
        }
    }

    @Test
    public void cssTempFiles_areIgnored_noLiveReloadInteractions()
            throws Exception {
        // Arrange a fake project structure with public resources
        File project = temporaryFolder.newFolder("project3");
        File publicRoot = new File(project, "src/main/resources/public");
        assertTrue(publicRoot.mkdirs());

        // Mock ApplicationConfiguration.get(context) -> project folder
        ApplicationConfiguration config = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(config.getProjectFolder()).thenReturn(project);
        appConfigStatic = Mockito.mockStatic(ApplicationConfiguration.class);

        // Use the lightweight MockVaadinContext in this module
        VaadinContext context = new MockVaadinContext();
        appConfigStatic
                .when(() -> ApplicationConfiguration.get(Mockito.eq(context)))
                .thenReturn(config);

        // Mock BrowserLiveReload available from context
        BrowserLiveReload liveReload = Mockito.mock(BrowserLiveReload.class);
        liveReloadAccessorStatic = Mockito
                .mockStatic(BrowserLiveReloadAccessor.class);
        liveReloadAccessorStatic
                .when(() -> BrowserLiveReloadAccessor
                        .getLiveReloadFromContext(Mockito.eq(context)))
                .thenReturn(Optional.of(liveReload));

        try (PublicResourcesLiveUpdater ignored = new PublicResourcesLiveUpdater(
                List.of(publicRoot.getAbsolutePath()), context)) {
            // Create and modify a temporary IDE backup file that should be
            // ignored
            File temp = new File(publicRoot, "temp.css~");
            Files.writeString(temp.toPath(), "/* draft */",
                    StandardCharsets.UTF_8);
            Files.writeString(temp.toPath(), "/* draft v2 */",
                    StandardCharsets.UTF_8);

            // Wait a bit to allow any file watcher events to be processed
            Awaitility.await().untilAsserted(() -> {
                Mockito.verifyNoInteractions(liveReload);
            });
        }
    }

    @Test
    public void cssChange_ignoresVaadinThemeUrls_noLiveReloadUpdates()
            throws Exception {
        // Arrange a fake project structure with public resources
        File project = temporaryFolder.newFolder("project4");
        File publicRoot = new File(project, "src/main/resources/public");
        assertTrue(publicRoot.mkdirs());

        // Create a css file to trigger the watcher
        File dummyCss = new File(publicRoot, "dummy.css");
        Files.writeString(dummyCss.toPath(), "body{margin:0;}",
                StandardCharsets.UTF_8);

        // Mock ApplicationConfiguration.get(context) -> project folder
        ApplicationConfiguration config = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(config.getProjectFolder()).thenReturn(project);
        appConfigStatic = Mockito.mockStatic(ApplicationConfiguration.class);

        // Use the lightweight MockVaadinContext in this module
        VaadinContext context = new MockVaadinContext();
        appConfigStatic
                .when(() -> ApplicationConfiguration.get(Mockito.eq(context)))
                .thenReturn(config);

        // Mock BrowserLiveReload available from context
        BrowserLiveReload liveReload = Mockito.mock(BrowserLiveReload.class);
        liveReloadAccessorStatic = Mockito
                .mockStatic(BrowserLiveReloadAccessor.class);
        liveReloadAccessorStatic
                .when(() -> BrowserLiveReloadAccessor
                        .getLiveReloadFromContext(Mockito.eq(context)))
                .thenReturn(Optional.of(liveReload));

        ActiveStyleSheetTracker.get(context)
                .trackForAppShell(Set.of("context://lumo/lumo.css"));
        ActiveStyleSheetTracker.get(context)
                .trackForAppShell(Set.of("context://aura/aura.css"));

        try (PublicResourcesLiveUpdater ignored = new PublicResourcesLiveUpdater(
                List.of(publicRoot.getAbsolutePath()), context)) {
            // Modify the css file to trigger the watcher
            Files.writeString(dummyCss.toPath(), "body{margin:1;}",
                    StandardCharsets.UTF_8);

            // Assert: no BrowserLiveReload interactions since the only active
            // URL
            // is a Vaadin theme which should be skipped (lines 125-128)
            Awaitility.await().untilAsserted(() -> {
                Mockito.verifyNoInteractions(liveReload);
            });
        }
    }
}
