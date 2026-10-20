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
import java.util.Optional;
import java.util.Set;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.base.devserver.hotswap.HotswapClassEvent;
import com.vaadin.base.devserver.hotswap.HotswapClassSessionEvent;
import com.vaadin.base.devserver.hotswap.UIUpdateStrategy;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;
import com.vaadin.tests.util.MockDeploymentConfiguration;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaScriptHotswapperTest {

    private JavaScriptHotswapper hotswapper;
    private MockVaadinServletService service;
    private VaadinSession session;
    private MockUI ui;

    @BeforeEach
    void setUp() {
        CurrentInstance.clearAll();
        service = new MockVaadinServletService(
                new MockDeploymentConfiguration());
        session = new AlwaysLockedVaadinSession(service);
        hotswapper = new JavaScriptHotswapper();

        ui = new MockUI(session);
        ui.doInit(null, 42, "foo");
        session.addUI(ui);
    }

    @AfterEach
    void tearDown() {
        CurrentInstance.clearAll();
    }

    @Test
    void runtimeScriptChanged_componentInUse_reloadsPage() throws Exception {
        assertEquals(Optional.of(UIUpdateStrategy.RELOAD),
                hotswap(RuntimeScript.class, RuntimeScriptChanged.class, true));
    }

    @Test
    void runtimeScriptTypeChanged_componentInUse_reloadsPage()
            throws Exception {
        assertEquals(Optional.of(UIUpdateStrategy.RELOAD), hotswap(
                RuntimeScript.class, RuntimeScriptAsModule.class, true));
    }

    @Test
    void runtimeScriptAdded_noReload() throws Exception {
        // The refresh adds the new script, and the previous one is still
        // wanted, so nothing is left behind
        assertEquals(Optional.empty(),
                hotswap(RuntimeScript.class, RuntimeScriptAdded.class, true));
    }

    @Test
    void bundledScriptChanged_noReload() throws Exception {
        assertEquals(Optional.empty(),
                hotswap(BundledScript.class, BundledScriptChanged.class, true));
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
    @JavaScript("bundled.js")
    public static class BundledScript extends Component {
    }

    @Tag("div")
    @JavaScript("changed-bundled.js")
    public static class BundledScriptChanged extends Component {
    }
}
