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
package com.vaadin.base.devserver;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.internal.ActiveStyleSheetTracker;
import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.BrowserLiveReloadAccessor;
import com.vaadin.flow.server.MockVaadinContext;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Verifies that PublicResourcesLiveUpdater reacts to a CSS change and bundles
 * imported files, pushing the merged result via BrowserLiveReload.update.
 */
class PublicResourcesLiveUpdaterTest {

    @TempDir
    File temporaryFolder;

    private MockedStatic<ApplicationConfiguration> appConfigStatic;
    private MockedStatic<BrowserLiveReloadAccessor> liveReloadAccessorStatic;

    @AfterEach
    void cleanupStatics() {
        if (appConfigStatic != null) {
            appConfigStatic.close();
        }
        if (liveReloadAccessorStatic != null) {
            liveReloadAccessorStatic.close();
        }
    }

    @Test
    void cssChange_triggersBundlingAndUpdate_forActiveUrl() throws Exception {
        // Arrange a fake project structure with public resources
        File project = new File(temporaryFolder, "project");
        project.mkdirs();
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
    void cssChange_triggersUpdates_forMultipleActiveUrls() throws Exception {
        // Arrange a fake project structure with public resources
        File project = new File(temporaryFolder, "project2");
        project.mkdirs();
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
    void cssTempFiles_areIgnored_noLiveReloadInteractions() throws Exception {
        // Arrange a fake project structure with public resources
        File project = new File(temporaryFolder, "project3");
        project.mkdirs();
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
    void cssChange_ignoresVaadinThemeUrls_noLiveReloadUpdates()
            throws Exception {
        // Arrange a fake project structure with public resources
        File project = new File(temporaryFolder, "project4");
        project.mkdirs();
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

    @Test
    void cssChange_skipsUpdateForClasspathStylesheet() throws Exception {
        // Arrange a project with one local CSS, and a stylesheet that only
        // exists on the classpath (e.g. from an addon JAR)
        File project = new File(temporaryFolder, "project-cp");
        project.mkdirs();
        File publicRoot = new File(project, "src/main/resources/public");
        assertTrue(publicRoot.mkdirs());

        File localCss = new File(publicRoot, "styles.css");
        Files.writeString(localCss.toPath(), "body{margin:0;}",
                StandardCharsets.UTF_8);

        ApplicationConfiguration config = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(config.getProjectFolder()).thenReturn(project);
        appConfigStatic = Mockito.mockStatic(ApplicationConfiguration.class);

        VaadinContext context = new MockVaadinContext();
        appConfigStatic
                .when(() -> ApplicationConfiguration.get(Mockito.eq(context)))
                .thenReturn(config);

        // Mock ResourceProvider to report the classpath stylesheet exists
        ResourceProvider resourceProvider = Mockito
                .mock(ResourceProvider.class);
        Mockito.when(resourceProvider.getApplicationResource(
                "META-INF/resources/frontend/addon.styles.css"))
                .thenReturn(getClass().getResource("/"));
        Lookup lookup = context.getAttribute(Lookup.class);
        Mockito.when(lookup.lookup(ResourceProvider.class))
                .thenReturn(resourceProvider);

        BrowserLiveReload liveReload = Mockito.mock(BrowserLiveReload.class);
        liveReloadAccessorStatic = Mockito
                .mockStatic(BrowserLiveReloadAccessor.class);
        liveReloadAccessorStatic
                .when(() -> BrowserLiveReloadAccessor
                        .getLiveReloadFromContext(Mockito.eq(context)))
                .thenReturn(Optional.of(liveReload));

        // Register both a local and a classpath-only stylesheet
        ActiveStyleSheetTracker.get(context).trackForAppShell(Set.of(
                "context://styles.css", "context://frontend/addon.styles.css"));

        try (PublicResourcesLiveUpdater ignored = new PublicResourcesLiveUpdater(
                List.of(publicRoot.getAbsolutePath()), context)) {
            // Modify local CSS to trigger the watcher
            Files.writeString(localCss.toPath(), "body{margin:8px;}",
                    StandardCharsets.UTF_8);

            // The local stylesheet should be updated with content
            Awaitility.await().untilAsserted(() -> Mockito
                    .verify(liveReload, Mockito.atLeastOnce())
                    .update(eq("context://styles.css"), argThat(c -> c != null
                            && c.contains("body{margin:8px;}"))));

            // The classpath stylesheet must NOT be updated with null
            Mockito.verify(liveReload, Mockito.never()).update(
                    eq("context://frontend/addon.styles.css"),
                    Mockito.isNull());
        }
    }

    @Test
    void cssChange_updatesStylesheetFromJarResources() throws Exception {
        // When the CSS exists in jar-resources (passed as a source root),
        // the bundler finds it and pushes content for both local and
        // jar-resources stylesheets.
        File project = new File(temporaryFolder, "project-jar-res");
        project.mkdirs();
        File publicRoot = new File(project, "src/main/resources/public");
        assertTrue(publicRoot.mkdirs());

        // Simulate the jar-resources folder at its real location inside the
        // project's frontend generated directory
        File jarResources = new File(project,
                "src/main/frontend/generated/jar-resources");
        assertTrue(jarResources.mkdirs());

        File localCss = new File(publicRoot, "app.css");
        Files.writeString(localCss.toPath(), ".app{display:block;}",
                StandardCharsets.UTF_8);

        File addonCss = new File(jarResources, "addon.css");
        Files.writeString(addonCss.toPath(), ".addon{color:blue;}",
                StandardCharsets.UTF_8);

        ApplicationConfiguration config = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(config.getProjectFolder()).thenReturn(project);
        appConfigStatic = Mockito.mockStatic(ApplicationConfiguration.class);

        VaadinContext context = new MockVaadinContext();
        appConfigStatic
                .when(() -> ApplicationConfiguration.get(Mockito.eq(context)))
                .thenReturn(config);

        BrowserLiveReload liveReload = Mockito.mock(BrowserLiveReload.class);
        liveReloadAccessorStatic = Mockito
                .mockStatic(BrowserLiveReloadAccessor.class);
        liveReloadAccessorStatic
                .when(() -> BrowserLiveReloadAccessor
                        .getLiveReloadFromContext(Mockito.eq(context)))
                .thenReturn(Optional.of(liveReload));

        // Both local and jar-resources CSS are active
        ActiveStyleSheetTracker.get(context).trackForAppShell(
                Set.of("context://app.css", "context://addon.css"));

        // Pass both the publicRoot and jar-resources as source roots
        try (PublicResourcesLiveUpdater ignored = new PublicResourcesLiveUpdater(
                List.of(publicRoot.getAbsolutePath(),
                        jarResources.getAbsolutePath()),
                context)) {
            // Trigger watcher by modifying the local CSS
            Files.writeString(localCss.toPath(), ".app{display:flex;}",
                    StandardCharsets.UTF_8);

            // Both stylesheets should be updated with content
            Awaitility.await().untilAsserted(() -> {
                Mockito.verify(liveReload, Mockito.atLeastOnce())
                        .update(eq("context://app.css"), argThat(c -> c != null
                                && c.contains(".app{display:flex;}")));
                Mockito.verify(liveReload, Mockito.atLeastOnce()).update(
                        eq("context://addon.css"), argThat(c -> c != null
                                && c.contains(".addon{color:blue;}")));
            });
        }
    }

    @Test
    void cssChange_updatesJarResourceStylesheetWithFrontendPrefix()
            throws Exception {
        // Addon stylesheets are referenced with frontend/ prefix in
        // @StyleSheet but TaskCopyFrontendFiles strips it when copying
        // to jar-resources. The bundler should find the file without the
        // prefix.
        File project = new File(temporaryFolder, "project-frontend-prefix");
        project.mkdirs();
        File publicRoot = new File(project, "src/main/resources/public");
        assertTrue(publicRoot.mkdirs());

        File jarResources = new File(project,
                "src/main/frontend/generated/jar-resources");
        assertTrue(jarResources.mkdirs());

        File localCss = new File(publicRoot, "local.css");
        Files.writeString(localCss.toPath(), ".local{display:block;}",
                StandardCharsets.UTF_8);

        // The addon CSS sits at jar-resources/addon.css (no frontend/ prefix)
        File addonCss = new File(jarResources, "addon.css");
        Files.writeString(addonCss.toPath(), ".addon{color:teal;}",
                StandardCharsets.UTF_8);

        ApplicationConfiguration config = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(config.getProjectFolder()).thenReturn(project);
        appConfigStatic = Mockito.mockStatic(ApplicationConfiguration.class);

        VaadinContext context = new MockVaadinContext();
        appConfigStatic
                .when(() -> ApplicationConfiguration.get(Mockito.eq(context)))
                .thenReturn(config);

        BrowserLiveReload liveReload = Mockito.mock(BrowserLiveReload.class);
        liveReloadAccessorStatic = Mockito
                .mockStatic(BrowserLiveReloadAccessor.class);
        liveReloadAccessorStatic
                .when(() -> BrowserLiveReloadAccessor
                        .getLiveReloadFromContext(Mockito.eq(context)))
                .thenReturn(Optional.of(liveReload));

        // Active URL uses frontend/ prefix, but file is at jar-resources root
        ActiveStyleSheetTracker.get(context).trackForAppShell(
                Set.of("context://local.css", "context://frontend/addon.css"));

        try (PublicResourcesLiveUpdater ignored = new PublicResourcesLiveUpdater(
                List.of(publicRoot.getAbsolutePath(),
                        jarResources.getAbsolutePath()),
                context)) {
            // Trigger watcher by modifying the local CSS
            Files.writeString(localCss.toPath(), ".local{display:flex;}",
                    StandardCharsets.UTF_8);

            // Both stylesheets should be updated with content
            Awaitility.await().untilAsserted(() -> {
                Mockito.verify(liveReload, Mockito.atLeastOnce()).update(
                        eq("context://local.css"), argThat(c -> c != null
                                && c.contains(".local{display:flex;}")));
                Mockito.verify(liveReload, Mockito.atLeastOnce()).update(
                        eq("context://frontend/addon.css"),
                        argThat(c -> c != null
                                && c.contains(".addon{color:teal;}")));
            });
        }
    }

    @Test
    void cssChange_removesDeletedStylesheet() throws Exception {
        // When a CSS file is deleted and does not exist on the classpath,
        // the updater should push null content (removal).
        File project = new File(temporaryFolder, "project-deleted");
        project.mkdirs();
        File publicRoot = new File(project, "src/main/resources/public");
        assertTrue(publicRoot.mkdirs());

        File localCss = new File(publicRoot, "keep.css");
        Files.writeString(localCss.toPath(), ".keep{color:red;}",
                StandardCharsets.UTF_8);

        // Create a CSS file that will be "deleted" — it exists initially so
        // that the bundler can find it, but we delete it before triggering
        File deletedCss = new File(publicRoot, "gone.css");
        Files.writeString(deletedCss.toPath(), ".gone{color:gray;}",
                StandardCharsets.UTF_8);

        ApplicationConfiguration config = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(config.getProjectFolder()).thenReturn(project);
        appConfigStatic = Mockito.mockStatic(ApplicationConfiguration.class);

        VaadinContext context = new MockVaadinContext();
        appConfigStatic
                .when(() -> ApplicationConfiguration.get(Mockito.eq(context)))
                .thenReturn(config);

        BrowserLiveReload liveReload = Mockito.mock(BrowserLiveReload.class);
        liveReloadAccessorStatic = Mockito
                .mockStatic(BrowserLiveReloadAccessor.class);
        liveReloadAccessorStatic
                .when(() -> BrowserLiveReloadAccessor
                        .getLiveReloadFromContext(Mockito.eq(context)))
                .thenReturn(Optional.of(liveReload));

        ActiveStyleSheetTracker.get(context).trackForAppShell(
                Set.of("context://keep.css", "context://gone.css"));

        try (PublicResourcesLiveUpdater ignored = new PublicResourcesLiveUpdater(
                List.of(publicRoot.getAbsolutePath()), context)) {
            // Delete the file, then trigger watcher with a change to keep.css
            assertTrue(deletedCss.delete());
            Files.writeString(localCss.toPath(), ".keep{color:blue;}",
                    StandardCharsets.UTF_8);

            // keep.css should be updated with content
            Awaitility.await().untilAsserted(() -> Mockito
                    .verify(liveReload, Mockito.atLeastOnce())
                    .update(eq("context://keep.css"), argThat(c -> c != null
                            && c.contains(".keep{color:blue;}"))));

            // gone.css should be updated with null (removal) since it's
            // not on classpath either
            Mockito.verify(liveReload, Mockito.atLeastOnce())
                    .update(eq("context://gone.css"), Mockito.isNull());
        }
    }

    @Test
    void vaadinThemeUrls_areNotBundled_whenCssChangesDetected()
            throws Exception {
        // Prepare a temp directory to watch
        File root = new File(temporaryFolder, "watched-root");
        root.mkdirs();

        // Mocks for Vaadin context and configuration (dev mode)
        VaadinContext ctx = Mockito.mock(VaadinContext.class);
        ApplicationConfiguration config = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(config.isProductionMode()).thenReturn(false);

        // Mock BrowserLiveReload plumbing via Lookup
        Lookup lookup = Mockito.mock(Lookup.class);
        BrowserLiveReload liveReload = Mockito.mock(BrowserLiveReload.class);
        BrowserLiveReloadAccessor accessor = Mockito
                .mock(BrowserLiveReloadAccessor.class);
        Mockito.when(accessor.getLiveReload(ctx)).thenReturn(liveReload);
        Mockito.when(lookup.lookup(BrowserLiveReloadAccessor.class))
                .thenReturn(accessor);
        Mockito.when(ctx.getAttribute(eq(Lookup.class))).thenReturn(lookup);

        // Mock Static: ApplicationConfiguration.get(context)
        try (MockedStatic<ApplicationConfiguration> appConfig = Mockito
                .mockStatic(ApplicationConfiguration.class);
                // Mock Static:
                // PublicStyleSheetBundler.forResourceLocations(...)
                MockedStatic<PublicStyleSheetBundler> bundlerStatic = Mockito
                        .mockStatic(PublicStyleSheetBundler.class)) {

            appConfig.when(() -> ApplicationConfiguration.get(Mockito.any()))
                    .thenReturn(config);

            // Active URLs include two Lumo URLs and one regular URL
            Set<String> activeUrls = new HashSet<>(Arrays.asList(
                    "/lumo/utility.css", "lumo/utility.css",
                    "/lumo/presets/compact.css", "lumo/presets/compact.css",
                    "/aura/aura.css", "aura/aura.css", "/css/app.css",
                    "context://css/app.css", "base://css/app.css",
                    "http://localhost:8080/hello"));
            ActiveStyleSheetTracker tracker = Mockito
                    .mock(ActiveStyleSheetTracker.class);
            Mockito.when(tracker.getActiveUrls()).thenReturn(activeUrls);
            // Ensure context returns our tracker for attribute-based access
            Mockito.when(ctx.getAttribute(
                    Mockito.eq(ActiveStyleSheetTracker.class), Mockito.any()))
                    .thenReturn(tracker);

            // Bundler mock
            PublicStyleSheetBundler bundler = Mockito
                    .mock(PublicStyleSheetBundler.class);
            // Return some Optional content for non-Lumo URLs to allow
            // liveReload.update
            Mockito.when(bundler.bundle(anyString(), anyString()))
                    .thenReturn(Optional.of("css"));
            bundlerStatic
                    .when(() -> PublicStyleSheetBundler
                            .forResourceLocations(Mockito.anyList()))
                    .thenReturn(bundler);

            // Start the updater watching the temp root
            try (PublicResourcesLiveUpdater ignored = new PublicResourcesLiveUpdater(
                    List.of(root.getAbsolutePath()), ctx)) {
                // Trigger a CSS change under the watched root
                File changed = new File(root, "trigger.css");
                Files.writeString(changed.toPath(), "body{}\n");

                // Wait until the non-Lumo URL is processed (bundled at least
                // once)
                Awaitility.await().untilAsserted(() -> {
                    Mockito.verify(bundler, Mockito.atLeastOnce())
                            .bundle(eq("/css/app.css"), anyString());
                    Mockito.verify(bundler, Mockito.atLeastOnce())
                            .bundle(eq("context://css/app.css"), anyString());
                    Mockito.verify(bundler, Mockito.atLeastOnce())
                            .bundle(eq("base://css/app.css"), anyString());
                });

                // Verify bundler.bundle was NEVER called for Lumo URLs
                Mockito.verify(bundler, Mockito.never())
                        .bundle(eq("/lumo/utility.css"), anyString());
                Mockito.verify(bundler, Mockito.never())
                        .bundle(eq("lumo/utility.css"), anyString());
                Mockito.verify(bundler, Mockito.never())
                        .bundle(eq("/lumo/presets/compact.css"), anyString());
                Mockito.verify(bundler, Mockito.never())
                        .bundle(eq("lumo/presets/compact.css"), anyString());
                Mockito.verify(bundler, Mockito.never())
                        .bundle(eq("/aura/aura.css"), anyString());
                Mockito.verify(bundler, Mockito.never())
                        .bundle(eq("aura/aura.css"), anyString());
                Mockito.verify(bundler, Mockito.never())
                        .bundle(eq("http://localhost:8080/hello"), anyString());
            }
        }
    }
}
