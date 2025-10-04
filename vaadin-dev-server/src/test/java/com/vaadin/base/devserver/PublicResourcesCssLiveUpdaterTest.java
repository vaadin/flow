package com.vaadin.base.devserver;

import static org.mockito.Mockito.*;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.BrowserLiveReloadAccessor;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

public class PublicResourcesCssLiveUpdaterTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void cssChangeTriggersUpdate() throws Exception {
        File resourcesRoot = tmp.newFolder("META-INF", "resources");
        File cssDir = new File(resourcesRoot, "css");
        cssDir.mkdirs();
        File cssFile = new File(cssDir, "site.css");
        Files.writeString(cssFile.toPath(), ".a{color:red}",
                StandardCharsets.UTF_8);

        // Prepare context and live reload
        VaadinContext context = new MockVaadinContext();
        ApplicationConfiguration appConfig = mock(
                ApplicationConfiguration.class);
        when(appConfig.isProductionMode()).thenReturn(false);
        context.setAttribute(ApplicationConfiguration.class, appConfig);

        BrowserLiveReload liveReload = mock(BrowserLiveReload.class);
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(inv -> {
            latch.countDown();
            return null;
        }).when(liveReload).update(eq("/css/site.css"), anyString());

        // Provide a Lookup that returns an accessor yielding our liveReload
        Lookup lookup = mock(Lookup.class);
        BrowserLiveReloadAccessor accessor = new BrowserLiveReloadAccessor() {
            @Override
            public BrowserLiveReload getLiveReload(VaadinContext ctx) {
                return liveReload;
            }
        };
        when(lookup.lookup(BrowserLiveReloadAccessor.class))
                .thenReturn(accessor);
        context.setAttribute(Lookup.class, lookup);

        // PublicResourcesCssLiveUpdater starts watcher in the CTOR,
        // so no need to invoke it explicitly
        try (PublicResourcesCssLiveUpdater ignored = new PublicResourcesCssLiveUpdater(
                resourcesRoot, context)) {
            // Touch the file to trigger watcher
            Files.writeString(cssFile.toPath(), ".a{color:blue}",
                    StandardCharsets.UTF_8);
            // Wait for async callback
            latch.await(3, TimeUnit.SECONDS);
        }
        verify(liveReload, atLeastOnce()).update(eq("/css/site.css"),
                anyString());
        verify(liveReload, never()).reload();
    }

    @Test
    public void nonCssChangeIsIgnored() throws Exception {
        File resourcesRoot = tmp.newFolder("META-INF", "resources");
        File img = new File(resourcesRoot, "logo.png");
        Files.write(img.toPath(), new byte[] { 0, 1, 2, 3 });

        VaadinContext context = new MockVaadinContext();
        ApplicationConfiguration appConfig = mock(
                ApplicationConfiguration.class);
        when(appConfig.isProductionMode()).thenReturn(false);
        context.setAttribute(ApplicationConfiguration.class, appConfig);

        BrowserLiveReload liveReload = mock(BrowserLiveReload.class);
        Lookup lookup = mock(Lookup.class);
        BrowserLiveReloadAccessor accessor = new BrowserLiveReloadAccessor() {
            @Override
            public BrowserLiveReload getLiveReload(VaadinContext ctx) {
                return liveReload;
            }
        };
        when(lookup.lookup(BrowserLiveReloadAccessor.class))
                .thenReturn(accessor);
        context.setAttribute(Lookup.class, lookup);

        // PublicResourcesCssLiveUpdater starts watcher in the CTOR,
        // so no need to invoke it explicitly
        try (PublicResourcesCssLiveUpdater ignored = new PublicResourcesCssLiveUpdater(
                resourcesRoot, context)) {
            // Modify the image
            Files.write(img.toPath(), new byte[] { 9, 9, 9 });
            // Give watcher a chance
            Thread.sleep(300);
        }
        verify(liveReload, never()).update(anyString(), anyString());
    }

    @Test
    public void devModeHandler_watchesAllStaticResourcePathsForCss()
            throws Exception {
        // Create a fake project folder with all four resource roots
        File project = tmp.newFolder("project-root");
        String[] roots = new String[] { "src/main/resources/META-INF/resources",
                "src/main/resources/resources", "src/main/resources/static",
                "src/main/resources/public" };
        String[] files = new String[] { "site1.css", "site2.css", "site3.css",
                "site4.css" };

        File[] cssFiles = new File[4];
        for (int i = 0; i < roots.length; i++) {
            File root = new File(project, roots[i]);
            File cssDir = new File(root, "css");
            cssDir.mkdirs();
            cssFiles[i] = new File(cssDir, files[i]);
            Files.writeString(cssFiles[i].toPath(), ".a{color:red}",
                    StandardCharsets.UTF_8);
        }

        // Prepare context and live reload
        VaadinContext context = new MockVaadinContext();
        ApplicationConfiguration appConfig = mock(
                ApplicationConfiguration.class);
        when(appConfig.getProjectFolder()).thenReturn(project);
        when(appConfig.isProductionMode()).thenReturn(false);
        context.setAttribute(ApplicationConfiguration.class, appConfig);

        BrowserLiveReload liveReload = mock(BrowserLiveReload.class);
        CountDownLatch latch = new CountDownLatch(4);
        doAnswer(inv -> {
            latch.countDown();
            return null;
        }).when(liveReload).update(anyString(), anyString());

        // Provide a Lookup that returns an accessor yielding our liveReload
        Lookup lookup = mock(Lookup.class);
        BrowserLiveReloadAccessor accessor = new BrowserLiveReloadAccessor() {
            @Override
            public BrowserLiveReload getLiveReload(VaadinContext ctx) {
                return liveReload;
            }
        };
        when(lookup.lookup(BrowserLiveReloadAccessor.class))
                .thenReturn(accessor);
        context.setAttribute(Lookup.class, lookup);

        DevModeHandlerManagerImpl manager = new DevModeHandlerManagerImpl();
        try {
            // Call initDevModeHandler which (in this test override) sets up
            // public CSS watchers
            manager.startWatchingPublicResourcesCss(context, appConfig);

            // Touch all css files to trigger watchers
            for (int i = 0; i < cssFiles.length; i++) {
                Files.writeString(cssFiles[i].toPath(), ".a{color:blue}",
                        StandardCharsets.UTF_8);
            }

            // Wait for all four updates
            latch.await(3, TimeUnit.SECONDS);
        } finally {
            manager.stopDevModeHandler();
        }

        // Verify each file path was updated
        verify(liveReload, atLeastOnce()).update(eq("/css/site1.css"),
                anyString());
        verify(liveReload, atLeastOnce()).update(eq("/css/site2.css"),
                anyString());
        verify(liveReload, atLeastOnce()).update(eq("/css/site3.css"),
                anyString());
        verify(liveReload, atLeastOnce()).update(eq("/css/site4.css"),
                anyString());
        verify(liveReload, never()).reload();
    }
}
