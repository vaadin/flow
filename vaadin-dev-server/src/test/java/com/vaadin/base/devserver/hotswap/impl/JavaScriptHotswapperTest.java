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
package com.vaadin.base.devserver.hotswap.impl;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.base.devserver.hotswap.HotswapClassEvent;
import com.vaadin.base.devserver.hotswap.HotswapClassSessionEvent;
import com.vaadin.base.devserver.hotswap.HotswapCompleteEvent;
import com.vaadin.base.devserver.hotswap.UIUpdateStrategy;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.ReflectionCache;
import com.vaadin.flow.server.AppShellRegistry;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;
import com.vaadin.tests.util.MockDeploymentConfiguration;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaScriptHotswapperTest {

    // Records what a change is warned about instead of logging it.
    private static class TestHotswapper extends JavaScriptHotswapper {
        private final List<String> warned = new ArrayList<>();

        @Override
        void warnAboutChangedBundledScripts(Class<?> type) {
            warned.add(type.getName());
        }
    }

    private TestHotswapper hotswapper;
    private MockVaadinServletService service;
    private VaadinSession session;
    private MockUI ui;

    @BeforeEach
    void setUp() {
        CurrentInstance.clearAll();
        service = new MockVaadinServletService(
                new MockDeploymentConfiguration());
        session = new AlwaysLockedVaadinSession(service);
        hotswapper = new TestHotswapper();

        ui = new MockUI(session);
        ui.doInit(null, 42, "foo");
        session.addUI(ui);
    }

    @AfterEach
    void tearDown() throws Exception {
        CurrentInstance.clearAll();
        AppShellRegistry.getInstance(service.getContext()).reset();
        for (Class<?> type : List.of(Base.class, Helper.class)) {
            ClassReloadingStrategy.fromInstalledAgent().reset(type);
        }
        List.of(Base.class, Helper.class, ExtendsBase.class,
                AlsoExtendsBase.class, UsesHelper.class)
                .forEach(ReflectionCache::clearAll);
    }

    @Test
    void runtimeScriptChanged_componentInUse_reloadsPage() throws Exception {
        assertEquals(Optional.of(UIUpdateStrategy.RELOAD),
                hotswap(RuntimeScript.class, RuntimeScriptChanged.class, true));
        assertEquals(List.of(), hotswapper.warned);
    }

    @Test
    void runtimeScriptTypeChanged_componentInUse_reloadsPage()
            throws Exception {
        assertEquals(Optional.of(UIUpdateStrategy.RELOAD), hotswap(
                RuntimeScript.class, RuntimeScriptAsModule.class, true));
    }

    @Test
    void moduleScriptChanged_componentInUse_reloadsPage() throws Exception {
        // A bare Type.MODULE value is loaded at runtime too, unlike a bare
        // classic script
        assertEquals(Optional.of(UIUpdateStrategy.RELOAD),
                hotswap(ModuleScript.class, ModuleScriptChanged.class, true));
    }

    @Test
    void usesRuntimeScript_unchanged_noReload() throws Exception {
        // The script of the @Uses target is part of the dependencies of the
        // component before and after the redefinition
        assertEquals(Optional.empty(), hotswap(UsesRuntimeScript.class,
                UsesRuntimeScript.class, true));
    }

    @Test
    void runtimeScriptChanged_uiClosing_noReload() throws Exception {
        ui.close();
        assertEquals(Optional.empty(),
                hotswap(RuntimeScript.class, RuntimeScriptChanged.class, true));
    }

    @Test
    void hotswapComplete_previousValuesForgotten() throws Exception {
        hotswap(RuntimeScript.class, RuntimeScriptChanged.class, true);
        Class<?> redefined = ui.getChildren().findFirst().orElseThrow()
                .getClass();
        hotswapper.onHotswapComplete(
                new HotswapCompleteEvent(service, Set.of(redefined), true));

        // A later session event without a new class event has nothing to
        // compare against
        HotswapClassSessionEvent event = new HotswapClassSessionEvent(service,
                session, Set.of(redefined), true);
        hotswapper.onClassesChange(event);
        assertEquals(Optional.empty(), event.getUIUpdateStrategy(ui));
    }

    @Test
    void runtimeScriptAdded_noReload() throws Exception {
        // The refresh adds the new script, and the previous one is still
        // wanted, so nothing is left behind
        assertEquals(Optional.empty(),
                hotswap(RuntimeScript.class, RuntimeScriptAdded.class, true));
    }

    @Test
    void bundledScriptChanged_warnsWithoutReload() throws Exception {
        assertEquals(Optional.empty(),
                hotswap(BundledScript.class, BundledScriptChanged.class, true));
        assertEquals(List.of(BundledScript.class.getName()), hotswapper.warned);

        // Warned once per hotswap, also when another session event sees the
        // same change
        Class<?> redefined = ui.getChildren().findFirst().orElseThrow()
                .getClass();
        hotswapper.onClassesChange(new HotswapClassSessionEvent(service,
                session, Set.of(redefined), true));
        assertEquals(1, hotswapper.warned.size());
    }

    @Test
    void bundledScriptMovedToRuntime_warnsWithoutReload() throws Exception {
        // The bundle still runs the script, so the added runtime script runs
        // it a second time until a restart
        assertEquals(Optional.empty(), hotswap(BundledScript.class,
                BundledScriptAsRuntime.class, true));
        assertEquals(List.of(BundledScript.class.getName()), hotswapper.warned);
    }

    @Test
    void runtimeScriptMovedToBundled_warnsAndReloads() throws Exception {
        assertEquals(Optional.of(UIUpdateStrategy.RELOAD), hotswap(
                RuntimeScript.class, RuntimeScriptAsBundled.class, true));
        assertEquals(List.of(RuntimeScript.class.getName()), hotswapper.warned);
    }

    @Test
    void superclassRuntimeScriptChanged_subclassInUse_reloadsWithNewValue()
            throws Exception {
        assertEquals(Optional.of(UIUpdateStrategy.RELOAD), hotswapInPlace(
                new ExtendsBase(), Base.class, BaseChanged.class));
        // The cached metadata of the subclass was read before the change, so
        // it is cleared for a new page to get the new value
        assertEquals(Set.of("/changed.js"),
                javaScriptValues(ExtendsBase.class));
    }

    @Test
    void superclassRuntimeScriptChanged_nestedSubclassesInUse_reloadsWithNewValues()
            throws Exception {
        Layout layout = new Layout();
        layout.add(new ExtendsBase(), new AlsoExtendsBase());
        assertEquals(Optional.of(UIUpdateStrategy.RELOAD),
                hotswapInPlace(layout, Base.class, BaseChanged.class));
        // Both are read again, not only the first one needing a reload
        assertEquals(Set.of("/changed.js"),
                javaScriptValues(ExtendsBase.class));
        assertEquals(Set.of("/changed.js"),
                javaScriptValues(AlsoExtendsBase.class));
    }

    @Test
    void usesCycleInUse_unrelatedClassChanged_noReload() throws Exception {
        ui.add(new UsesCycleA());
        assertEquals(Optional.empty(), hotswap(RuntimeScript.class,
                RuntimeScriptChanged.class, false));
    }

    @Test
    void appShellRuntimeScriptAdded_reloadsAllUIs() {
        AppShellRegistry.getInstance(service.getContext())
                .setShell(AppShell.class);
        hotswapper.onInit(service);

        Class<?> redefined = redefine(AppShell.class, AppShellChanged.class);
        hotswapper.onClassesChange(
                new HotswapClassEvent(service, Set.of(redefined), true));
        HotswapClassSessionEvent event = new HotswapClassSessionEvent(service,
                session, Set.of(redefined), true);
        hotswapper.onClassesChange(event);

        // The app shell scripts are rendered into the initial page, so even
        // an added one needs a reload
        assertEquals(Optional.of(UIUpdateStrategy.RELOAD),
                event.getUIUpdateStrategy(ui));
        assertEquals(List.of(AppShell.class.getName()), hotswapper.warned);
    }

    @Test
    void usesTargetRuntimeScriptChanged_componentInUse_reloadsWithNewValue()
            throws Exception {
        assertEquals(Optional.of(UIUpdateStrategy.RELOAD), hotswapInPlace(
                new UsesHelper(), Helper.class, HelperChanged.class));
        assertEquals(Set.of("/changed.js"), javaScriptValues(UsesHelper.class));
    }

    @Test
    void runtimeScriptChanged_componentNotInUse_noReload() throws Exception {
        assertEquals(Optional.empty(), hotswap(RuntimeScript.class,
                RuntimeScriptChanged.class, false));
    }

    /**
     * Runs the hotswap sequence for {@code original} being redefined with the
     * annotations of {@code modified}, and returns the update requested for the
     * UI.
     */
    private Optional<UIUpdateStrategy> hotswap(
            Class<? extends Component> original, Class<?> modified,
            boolean inUse) throws Exception {
        if (inUse) {
            ui.add(original.getConstructor().newInstance());
        }
        // The dependencies of the component are cached when it is used, which
        // is what the hotswapper reads the previous values from
        ui.getInternals().addComponentDependencies(original);

        hotswapper.onClassesChange(
                new HotswapClassEvent(service, Set.of(original), true));

        Class<?> redefined = redefine(original, modified);
        if (inUse) {
            // Instances of the redefined class are what is in use afterwards
            ui.removeAll();
            ui.add((Component) redefined.getConstructor().newInstance());
        }
        HotswapClassSessionEvent event = new HotswapClassSessionEvent(service,
                session, Set.of(redefined), true);
        hotswapper.onClassesChange(event);
        return event.getUIUpdateStrategy(ui);
    }

    /**
     * Runs the hotswap sequence for {@code original} being redefined in place
     * with the annotations of {@code modified}, the way the JVM redefines a
     * class, while {@code inUse} is in the UI. Returns the update requested for
     * the UI.
     */
    private Optional<UIUpdateStrategy> hotswapInPlace(Component inUse,
            Class<?> original, Class<?> modified) {
        ui.add(inUse);
        Stream.concat(Stream.of(inUse), inUse.getChildren())
                .forEach(component -> ui.getInternals()
                        .addComponentDependencies(component.getClass()));

        hotswapper.onClassesChange(
                new HotswapClassEvent(service, Set.of(original), true));

        ByteBuddyAgent.install();
        new ByteBuddy().redefine(modified).name(original.getName()).make().load(
                original.getClassLoader(),
                ClassReloadingStrategy.fromInstalledAgent());
        // Done by ReflectionCacheHotswapper before the session events
        ReflectionCache.clearAll(original);

        HotswapClassSessionEvent event = new HotswapClassSessionEvent(service,
                session, Set.of(original), true);
        hotswapper.onClassesChange(event);
        return event.getUIUpdateStrategy(ui);
    }

    private Set<String> javaScriptValues(Class<? extends Component> type) {
        return ComponentUtil.getDependencies(service, type).getJavaScripts()
                .stream().map(JavaScript::value).collect(Collectors.toSet());
    }

    /**
     * Creates a class with the name of {@code source} and the bytecode of
     * {@code target}, simulating the redefinition of {@code source}.
     */
    private Class<?> redefine(Class<?> source, Class<?> target) {
        return new ByteBuddy().redefine(target).name(source.getName()).make()
                .load(new URLClassLoader(new URL[0],
                        getClass().getClassLoader()),
                        ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
    }

    @Tag("div")
    @JavaScript("/script.js")
    public static class RuntimeScript extends Component {
    }

    @Tag("div")
    @JavaScript("/changed.js")
    public static class RuntimeScriptChanged extends Component {
    }

    @Tag("div")
    @JavaScript(value = "/script.js", type = JavaScript.Type.MODULE)
    public static class RuntimeScriptAsModule extends Component {
    }

    @Tag("div")
    @JavaScript("/script.js")
    @JavaScript("/added.js")
    public static class RuntimeScriptAdded extends Component {
    }

    @Tag("div")
    @JavaScript(value = "a.js", type = JavaScript.Type.MODULE)
    public static class ModuleScript extends Component {
    }

    @Tag("div")
    @JavaScript(value = "b.js", type = JavaScript.Type.MODULE)
    public static class ModuleScriptChanged extends Component {
    }

    @Tag("div")
    @Uses(RuntimeScript.class)
    public static class UsesRuntimeScript extends Component {
    }

    @Tag("div")
    @JavaScript("bundled.js")
    public static class BundledScript extends Component {
    }

    @Tag("div")
    @JavaScript("changed-bundled.js")
    public static class BundledScriptChanged extends Component {
    }

    @Tag("div")
    @JavaScript("script.js")
    public static class RuntimeScriptAsBundled extends Component {
    }

    @Tag("div")
    @JavaScript("context://bundled.js")
    public static class BundledScriptAsRuntime extends Component {
    }

    // Redefined in place, so only used by hotswapInPlace
    @Tag("div")
    @JavaScript("/script.js")
    public static class Base extends Component {
    }

    @Tag("div")
    @JavaScript("/changed.js")
    public static class BaseChanged extends Component {
    }

    public static class ExtendsBase extends Base {
    }

    public static class AlsoExtendsBase extends Base {
    }

    @Tag("div")
    public static class Layout extends Component implements HasComponents {
    }

    @Tag("div")
    @Uses(UsesCycleB.class)
    public static class UsesCycleA extends Component {
    }

    @Tag("div")
    @Uses(UsesCycleA.class)
    public static class UsesCycleB extends Component {
    }

    @JavaScript("context://shell.js")
    @JavaScript("bundled.js")
    public static class AppShell implements AppShellConfigurator {
    }

    @JavaScript("context://shell.js")
    @JavaScript("context://added.js")
    public static class AppShellChanged implements AppShellConfigurator {
    }

    // Redefined in place, so only used by hotswapInPlace
    @Tag("div")
    @JavaScript("/script.js")
    public static class Helper extends Component {
    }

    @Tag("div")
    @JavaScript("/changed.js")
    public static class HelperChanged extends Component {
    }

    @Tag("div")
    @Uses(Helper.class)
    public static class UsesHelper extends Component {
    }
}
