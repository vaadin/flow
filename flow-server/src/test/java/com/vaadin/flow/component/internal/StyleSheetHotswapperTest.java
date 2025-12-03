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
package com.vaadin.flow.component.internal;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.Set;
import java.util.stream.Stream;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.hotswap.HotswapClassEvent;
import com.vaadin.flow.hotswap.HotswapClassSessionEvent;
import com.vaadin.flow.hotswap.HotswapResourceEvent;
import com.vaadin.flow.hotswap.UIUpdateStrategy;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.AppShellRegistry;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.server.startup.ApplicationConfigurationFactory;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;
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

    private StyleSheetHotswapper hotswapper;
    private MockVaadinServletService service;
    private VaadinSession session;
    private MockUI ui;
    private AppShellRegistry appShellRegistry;

    @Rule
    public TemporaryFolder tempProjectDir = new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
        CurrentInstance.clearAll();

        MockDeploymentConfiguration dc = new MockDeploymentConfiguration();
        // Use TemporaryFolder for the project directory required for build
        // resources
        dc.setProjectFolder(tempProjectDir.getRoot());

        service = new MockVaadinServletService(dc);
        session = new AlwaysLockedVaadinSession(service);
        hotswapper = new StyleSheetHotswapper();

        // Setup UI
        ui = new MockUI(session);
        ui.doInit(null, 42, "foo");
        session.addUI(ui);

        appShellRegistry = AppShellRegistry.getInstance(service.getContext());

        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(appConfig.isProductionMode()).thenAnswer(
                i -> service.getDeploymentConfiguration().isProductionMode());
        Mockito.when(service.getLookup()
                .lookup(ApplicationConfigurationFactory.class))
                .thenReturn(context -> appConfig);
    }

    @After
    public void tearDown() {
        appShellRegistry.reset();
        CurrentInstance.clearAll();
    }

    // === Resources hotswap

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

    // ==== Stylesheet annotation hotswap

    @Test
    public void onInit_storesInitialState() {
        appShellRegistry.setShell(TestAppShell.class);
        hotswapper.onInit(service);

        // Class is reloaded but without changes stylesheet annotation
        var event = new HotswapClassSessionEvent(service, session,
                Set.of(TestAppShell.class), true);
        hotswapper.onClassesChange(event);

        Assert.assertFalse("Should not force page reload",
                event.anyUIRequiresPageReload());
        Assert.assertFalse("Should not require refresh",
                event.getUIUpdateStrategy(ui).isPresent());
    }

    @Test
    public void onClassesChange_newClass_storesState() {
        // When a new stylesheet annotated class is loaded, it should store the
        // state but not apply
        // changes
        var event = new HotswapClassSessionEvent(service, session,
                Set.of(TestAppShell.class), false);
        hotswapper.onClassesChange(event);

        Assert.assertFalse("Should not force page reload",
                event.anyUIRequiresPageReload());
        Assert.assertFalse("Should not require refresh",
                event.getUIUpdateStrategy(ui).isPresent());

        // Class is reloaded but without changes stylesheet annotation
        // should compare with store state
        event = new HotswapClassSessionEvent(service, session,
                Set.of(TestAppShell.class), true);
        hotswapper.onClassesChange(event);

        Assert.assertFalse("Should not force page reload",
                event.anyUIRequiresPageReload());
        Assert.assertFalse("Should not require refresh",
                event.getUIUpdateStrategy(ui).isPresent());
    }

    @Test
    public void onClassesChange_appShellAddAnnotation_addsStylesheet() {
        appShellRegistry.setShell(TestAppShellNoAnnotation.class);
        hotswapper.onInit(service);

        int initialCount = countStylesheets();

        Class<?> appShell = modifyStyleSheetAnnotation(
                TestAppShellNoAnnotation.class, TestAppShell.class);
        var event = new HotswapClassSessionEvent(service, session,
                Set.of(appShell), true);
        hotswapper.onClassesChange(event);

        Assert.assertTrue("Should have app.css stylesheet",
                hasStylesheet("styles/app.css"));
        Assert.assertEquals("Should add one stylesheet", initialCount + 1,
                countStylesheets());

        Assert.assertFalse("Should not require page reload",
                event.anyUIRequiresPageReload());
        Assert.assertEquals("Should require page refresh",
                UIUpdateStrategy.REFRESH,
                event.getUIUpdateStrategy(ui).orElse(null));
    }

    @Test
    public void onClassesChange_appShellRemoveAnnotation_removesStylesheet() {
        appShellRegistry.setShell(TestAppShell.class);
        hotswapper.onInit(service);

        Class<?> appShell = modifyStyleSheetAnnotation(TestAppShell.class,
                TestAppShellNoAnnotation.class);
        var event = new HotswapClassSessionEvent(service, session,
                Set.of(appShell), true);
        hotswapper.onClassesChange(event);

        assertAppShellStyleSheetRemoved("styles/app.css");

        Assert.assertFalse("Should not require page reload",
                event.anyUIRequiresPageReload());
        Assert.assertEquals("Should require page refresh",
                UIUpdateStrategy.REFRESH,
                event.getUIUpdateStrategy(ui).orElse(null));
    }

    @Test
    public void onClassesChange_appShellModifyAnnotation_updatesStylesheet() {
        appShellRegistry.setShell(TestAppShell.class);
        hotswapper.onInit(service);

        Class<?> appShell = modifyStyleSheetAnnotation(TestAppShell.class,
                TestAppShellModified.class);
        var event = new HotswapClassSessionEvent(service, session,
                Set.of(appShell), true);
        hotswapper.onClassesChange(event);

        assertAppShellStyleSheetRemoved("styles/app.css");
        Assert.assertTrue("Should have modified.css stylesheet",
                hasStylesheet("styles/modified.css"));
        Assert.assertEquals("Should add one stylesheet", 1, countStylesheets());

        Assert.assertFalse("Should not require page reload",
                event.anyUIRequiresPageReload());
        Assert.assertEquals("Should require page refresh",
                UIUpdateStrategy.REFRESH,
                event.getUIUpdateStrategy(ui).orElse(null));
    }

    @Test
    public void onClassesChange_appShellMultipleAnnotations_handlesMultiple() {
        appShellRegistry.setShell(TestAppShellNoAnnotation.class);
        hotswapper.onInit(service);

        int initialCount = countStylesheets();
        Assert.assertEquals("Should not have any stylesheet initially", 0,
                initialCount);

        Class<?> appShell = modifyStyleSheetAnnotation(
                TestAppShellNoAnnotation.class, TestAppShellMultiple.class);
        var event = new HotswapClassSessionEvent(service, session,
                Set.of(appShell), true);
        hotswapper.onClassesChange(event);

        Assert.assertEquals("Should add one stylesheet", 2, countStylesheets());
        Assert.assertTrue("Should have modified.css stylesheet",
                hasStylesheet("styles/app.css"));
        Assert.assertTrue("Should have modified.css stylesheet",
                hasStylesheet("styles/theme.css"));

        Assert.assertFalse("Should not require page reload",
                event.anyUIRequiresPageReload());
        Assert.assertEquals("Should require page refresh",
                UIUpdateStrategy.REFRESH,
                event.getUIUpdateStrategy(ui).orElse(null));
    }

    @Test
    public void onClassesChange_componentInUse_addsStylesheet()
            throws Exception {
        TestComponentNoAnnotation component = new TestComponentNoAnnotation();
        ui.add(component);
        // simulate initial roundtrip
        ui.getInternals()
                .addComponentDependencies(TestComponentNoAnnotation.class);

        Class<?> componentClass = modifyStyleSheetAnnotation(
                TestComponentNoAnnotation.class, TestComponent.class);
        // Replace component with modified version otherwise it will not be
        // matched
        ui.remove(component);
        ui.add((Component) componentClass.getConstructor().newInstance());

        // Simulates getting metadata on class hot reload when the class is seen
        // for the first time
        // Not possible to do with the modified test class since it uses a
        // different classloader,
        // and the lookup in ComponentUtil metadata cache will fail
        hotswapper.onClassesChange(new HotswapClassEvent(service,
                Set.of(TestComponentNoAnnotation.class), true));

        var event = new HotswapClassSessionEvent(service, session,
                Set.of(componentClass), true);
        hotswapper.onClassesChange(event);

        Assert.assertTrue("Should have component.css stylesheet",
                hasStylesheet("styles/component.css"));

        Assert.assertFalse("Should not require page reload",
                event.anyUIRequiresPageReload());
        Assert.assertEquals("Should require page refresh",
                UIUpdateStrategy.REFRESH,
                event.getUIUpdateStrategy(ui).orElse(null));
    }

    @Test
    public void onClassesChange_componentNotInUse_doesNotAddStylesheet() {
        int initialCount = countStylesheets();

        var event = new HotswapClassSessionEvent(service, session,
                Set.of(TestComponentNoAnnotation.class), false);
        hotswapper.onClassesChange(event);

        Assert.assertEquals(
                "Should not add stylesheets when not annotated component not in UI",
                initialCount, countStylesheets());

        event = new HotswapClassSessionEvent(service, session,
                Set.of(TestComponent.class), true);
        hotswapper.onClassesChange(event);

        Assert.assertEquals(
                "Should not add stylesheets when annotated component not in UI",
                initialCount, countStylesheets());

        Assert.assertFalse("Should not require page reload",
                event.anyUIRequiresPageReload());
        Assert.assertFalse("Should not require page refresh",
                event.getUIUpdateStrategy(ui).isPresent());
    }

    @Test
    public void onClassesChange_componentModifyAnnotation_updatesStylesheet()
            throws Exception {
        var event = new HotswapClassSessionEvent(service, session,
                Set.of(TestComponent.class), false);
        hotswapper.onClassesChange(event);

        TestComponent component = new TestComponent();
        ui.add(component);
        // simulate initial roundtrip
        ui.getInternals().addComponentDependencies(TestComponent.class);

        int initialCount = countStylesheets();
        Assert.assertTrue("Should have modified-component.css stylesheet",
                hasStylesheet("styles/component.css"));
        ui.getInternals().getDependencyList().clearPendingSendToClient();

        Class<?> componentClass = modifyStyleSheetAnnotation(
                TestComponent.class, TestComponentModified.class);
        // Replace component with modified version otherwise it will not be
        // matched
        ui.remove(component);
        ui.add((Component) componentClass.getConstructor().newInstance());

        event = new HotswapClassSessionEvent(service, session,
                Set.of(componentClass), true);
        hotswapper.onClassesChange(event);

        Assert.assertTrue("Should have modified-component.css stylesheet",
                hasStylesheet("styles/modified-component.css"));
        Assert.assertEquals("Should add one stylesheet", initialCount,
                countStylesheets());

        Assert.assertFalse("Should not require page reload",
                event.anyUIRequiresPageReload());
        Assert.assertEquals("Should require page refresh",
                UIUpdateStrategy.REFRESH,
                event.getUIUpdateStrategy(ui).orElse(null));
    }

    @Test
    public void onClassesChange_noStylesheetChanges_doesNothing() {
        // Setup: register class with annotation
        appShellRegistry.setShell(TestAppShell.class);
        var event = new HotswapClassSessionEvent(service, session,
                Set.of(TestAppShell.class), false);
        hotswapper.onClassesChange(event);

        int initialCount = countStylesheets();

        // Simulate: same class reloaded with same annotations
        event = new HotswapClassSessionEvent(service, session,
                Set.of(TestAppShell.class), true);
        hotswapper.onClassesChange(event);

        // Verify: no changes applied
        Assert.assertEquals("Should not add stylesheets when nothing changed",
                initialCount, countStylesheets());
        Assert.assertFalse("Should not require page reload",
                event.anyUIRequiresPageReload());
        Assert.assertFalse("Should not require page refresh",
                event.getUIUpdateStrategy(ui).isPresent());
    }

    @Test
    public void onClassesChange_nonAppShellNonComponent_ignored() {
        // Test with a class that is neither AppShellConfigurator nor Component
        @StyleSheet("styles/ignored.css")
        class NonComponentClass {
        }

        var event = new HotswapClassSessionEvent(service, session,
                Set.of(NonComponentClass.class), false);
        hotswapper.onClassesChange(event);

        int initialCount = countStylesheets();
        Assert.assertEquals(
                "Should not have any stylesheet pending for non-Component, non-AppShell classes",
                0, initialCount);

        event = new HotswapClassSessionEvent(service, session,
                Set.of(NonComponentClass.class), true);
        hotswapper.onClassesChange(event);

        // Verify: no stylesheets added
        Assert.assertEquals(
                "Should not add stylesheets for non-Component, non-AppShell classes",
                initialCount, countStylesheets());
        Assert.assertFalse("Should not require page reload",
                event.anyUIRequiresPageReload());
        Assert.assertFalse("Should not require page refresh",
                event.getUIUpdateStrategy(ui).isPresent());
    }

    @Test
    public void onClassesChange_appShellNotRegistered_doesNothing() {
        // Setup: different AppShell is registered
        appShellRegistry.setShell(TestAppShellNoAnnotation.class);
        hotswapper.onInit(service);

        var event = new HotswapClassSessionEvent(service, session,
                Set.of(TestAppShellNoAnnotation.class), false);
        hotswapper.onClassesChange(event);

        int initialCount = countStylesheets();

        // Simulate: change to a different AppShell class that isn't registered
        event = new HotswapClassSessionEvent(service, session,
                Set.of(TestAppShell.class), true);
        hotswapper.onClassesChange(event);

        Assert.assertEquals(
                "Should not add stylesheets for non-registered AppShell",
                initialCount, countStylesheets());
        Assert.assertFalse("Should not require page reload",
                event.anyUIRequiresPageReload());
        Assert.assertFalse("Should not require page refresh",
                event.getUIUpdateStrategy(ui).isPresent());
    }

    @Test
    public void onClassesChange_closingUI_skipped() {
        // Setup: mark UI as closing
        ui.close();

        appShellRegistry.setShell(TestAppShellNoAnnotation.class);
        var event = new HotswapClassSessionEvent(service, session,
                Set.of(TestAppShellNoAnnotation.class), false);
        hotswapper.onClassesChange(event);

        int initialCount = countStylesheets();

        // Simulate: annotation added
        appShellRegistry.reset();
        appShellRegistry.setShell(TestAppShell.class);
        event = new HotswapClassSessionEvent(service, session,
                Set.of(TestAppShell.class), true);
        hotswapper.onClassesChange(event);

        // Verify: no stylesheets added because UI is closing
        Assert.assertEquals("Should not add stylesheets to closing UI",
                initialCount, countStylesheets());
    }

    @Test
    public void onClassesChange_exceptionDuringProcessing_doesNotCrash() {
        // Setup: create a scenario that might cause issues
        VaadinSession faultySession = Mockito.mock(VaadinSession.class);
        Mockito.when(faultySession.getUIs())
                .thenThrow(new RuntimeException("Test exception"));
        Mockito.when(faultySession.getService()).thenReturn(service);

        // Simulate: class change that would access UIs
        var event = new HotswapClassSessionEvent(service, faultySession,
                Set.of(TestAppShell.class), true);
        hotswapper.onClassesChange(event);

        // Verify: no crash, returns false
        Assert.assertFalse("Should not crash on exception",
                event.anyUIRequiresPageReload());
    }

    @Test
    public void onClassesChange_emptyStylesheetValue_ignored() {
        // Test with annotation that has empty value
        @Tag("div")
        @StyleSheet("")
        class EmptyStyleSheetComponent extends Component {
        }

        EmptyStyleSheetComponent component = new EmptyStyleSheetComponent();
        ui.add(component);

        var event = new HotswapClassSessionEvent(service, session,
                Set.of(EmptyStyleSheetComponent.class), false);
        hotswapper.onClassesChange(event);

        int initialCount = countStylesheets();

        event = new HotswapClassSessionEvent(service, session,
                Set.of(EmptyStyleSheetComponent.class), true);
        hotswapper.onClassesChange(event);

        // Verify: no stylesheets added for empty value
        Assert.assertEquals("Should ignore empty stylesheet values",
                initialCount, countStylesheets());
    }

    // ==== Helper method and classes

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

    private File createResource(String resourcePath) throws IOException {
        File buildResources = service.getDeploymentConfiguration()
                .getOutputResourceFolder();
        // Mimic a static resources folder under build resources
        File css = new File(buildResources, resourcePath);
        css.getParentFile().mkdirs();
        Files.writeString(css.toPath(), "body{}\n");
        return css;
    }

    /**
     * Helper method to count stylesheets in dependency list
     */
    private int countStylesheets() {
        return (int) currentStylesheets().count();
    }

    /**
     * Helper method to check if a specific stylesheet URL exists
     */
    private boolean hasStylesheet(String urlPart) {
        return currentStylesheets().anyMatch(url -> url.contains(urlPart));
    }

    private Stream<String> currentStylesheets() {
        return ui.getInternals().getDependencyList().getPendingSendToClient()
                .stream()
                .filter(dep -> dep.getType() == Dependency.Type.STYLESHEET)
                .map(Dependency::getUrl);
    }

    private void assertAppShellStyleSheetRemoved(String path) {
        Assert.assertTrue("Should have removed app shell stylesheet " + path,
                ui.getInternals().getPendingStyleSheetRemovals()
                        .contains("appShell-" + path));
    }

    /**
     * Simulates the modification of the {@code source} class, creating a new
     * dynamic class with the bytecode of {@code target}.
     *
     * @param source
     *            the class that should be "modified"
     * @param target
     *            the class to be used as a template for the modified class.
     * @return the newly created dynamic class with the bytecode of
     *         {@code target}.
     */
    private Class<?> modifyStyleSheetAnnotation(Class<?> source,
            Class<?> target) {
        Class<?> generatedClass = new ByteBuddy().redefine(target)
                .name(source.getName()).make()
                .load(new URLClassLoader(new URL[0],
                        getClass().getClassLoader()),
                        ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        Assert.assertEquals(
                "Generated class should have same name as source class",
                generatedClass.getName(), source.getName());
        return generatedClass;
    }

    // Test classes for AppShellConfigurator
    @StyleSheet("styles/app.css")
    public static class TestAppShell implements AppShellConfigurator {
    }

    @StyleSheet("styles/app.css")
    @StyleSheet("styles/theme.css")
    public static class TestAppShellMultiple implements AppShellConfigurator {
    }

    public static class TestAppShellNoAnnotation
            implements AppShellConfigurator {
    }

    @StyleSheet("styles/modified.css")
    public static class TestAppShellModified implements AppShellConfigurator {
    }

    // Test classes for Components
    @Tag("div")
    @StyleSheet("styles/component.css")
    public static class TestComponent extends Component {
    }

    @Tag("div")
    @StyleSheet("styles/component.css")
    @StyleSheet("styles/extra.css")
    public static class TestComponentMultiple extends Component {
    }

    @Tag("div")
    public static class TestComponentNoAnnotation extends Component {
    }

    @Tag("div")
    @StyleSheet("styles/modified-component.css")
    public static class TestComponentModified extends Component {
    }

}
