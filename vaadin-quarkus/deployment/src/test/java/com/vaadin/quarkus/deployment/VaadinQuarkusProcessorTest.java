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
package com.vaadin.quarkus.deployment;

import jakarta.servlet.annotation.HandlesTypes;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.undertow.deployment.IgnoredServletContainerInitializerBuildItem;
import io.quarkus.undertow.deployment.ServletBuildItem;
import io.quarkus.undertow.deployment.ServletContainerInitializerBuildItem;
import org.atmosphere.cpr.ApplicationConfig;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.js.JsInvokers;
import com.vaadin.quarkus.QuarkusVaadinServlet;
import com.vaadin.quarkus.graal.QuarkusJSR356AsyncSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class VaadinQuarkusProcessorTest {

    private final List<ServletBuildItem> servlets = new ArrayList<>();
    private final List<VaadinServletBuildItem> vaadinServlets = new ArrayList<>();

    @Test
    void mapVaadinServletPaths_nativeBuild_servletInitializedAtRuntimeWithQuarkusAsyncSupport() {
        mapVaadinServletPaths(true);

        ServletBuildItem servlet = servlets.get(0);
        // Without it, Atmosphere would pick JSR356AsyncSupport, which fails
        // to register the push endpoint at RUNTIME_INIT, so push would
        // silently fall back to long polling
        assertEquals(QuarkusJSR356AsyncSupport.class.getName(), servlet
                .getInitParams().get(ApplicationConfig.PROPERTY_COMET_SUPPORT));
        // Not loaded on startup by the servlet container, but by the
        // extension once Atmosphere is initialized
        assertEquals(0, servlet.getLoadOnStartup());
        assertEquals(1, vaadinServlets.size());
        assertEquals(servlet.getName(), vaadinServlets.get(0).getServletName());
        assertEquals(1, vaadinServlets.get(0).getLoadOnStartup());
    }

    @Test
    void mapVaadinServletPaths_jvmBuild_servletLoadedOnStartupWithDefaultAsyncSupport() {
        mapVaadinServletPaths(false);

        ServletBuildItem servlet = servlets.get(0);
        assertEquals(QuarkusVaadinServlet.class.getName(), servlet.getName());
        assertFalse(servlet.getInitParams()
                .containsKey(ApplicationConfig.PROPERTY_COMET_SUPPORT));
        assertEquals(1, servlet.getLoadOnStartup());
        assertTrue(vaadinServlets.isEmpty());
    }

    @Test
    void collectHandledTypes_findsWhatUndertowDoesAndSubInterfaces()
            throws IOException {
        Indexer indexer = new Indexer();
        for (Class<?> type : List.of(Listener.class, JsInvokers.class,
                GreeterJs.class, Greeter.class, Base.class, Child.class,
                GrandChild.class, Marked.class, AnnotatedType.class,
                AnnotatedMethod.class, AnnotatedField.class)) {
            indexer.indexClass(type);
        }
        Index index = indexer.complete();

        Set<String> handled = VaadinQuarkusProcessor.collectHandledTypes(index,
                index.getClassByName(Listener.class).declaredAnnotation(
                        DotName.createSimple(HandlesTypes.class)));

        assertEquals(Set.of(GreeterJs.class.getName(), Greeter.class.getName(),
                Child.class.getName(), GrandChild.class.getName(),
                AnnotatedType.class.getName(), AnnotatedMethod.class.getName(),
                AnnotatedField.class.getName()), handled);
    }

    @Test
    void registerDevModeStartupListener_noDevServer_registersNothing() {
        Index emptyIndex = new Indexer().complete();
        BuildProducer<IgnoredServletContainerInitializerBuildItem> ignored = mock();
        BuildProducer<ServletContainerInitializerBuildItem> initializers = mock();
        BuildProducer<AdditionalBeanBuildItem> beans = mock();

        new VaadinQuarkusProcessor().registerDevModeStartupListener(
                new CombinedIndexBuildItem(emptyIndex, emptyIndex), ignored,
                initializers, beans);

        verifyNoInteractions(ignored, initializers, beans);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface Marked {
    }

    @HandlesTypes({ JsInvokers.class, Base.class, Marked.class })
    static class Listener {
    }

    interface GreeterJs extends JsInvokers {
    }

    static class Greeter implements GreeterJs {
    }

    static class Base {
    }

    static class Child extends Base {
    }

    static class GrandChild extends Child {
    }

    @Marked
    static class AnnotatedType {
    }

    static class AnnotatedMethod {
        @Marked
        void method() {
            // Only the annotation on the method is looked at
        }
    }

    static class AnnotatedField {
        @Marked
        String field;
    }

    private void mapVaadinServletPaths(boolean nativeBuild) {
        NativeConfig nativeConfig = mock(NativeConfig.class);
        when(nativeConfig.enabled()).thenReturn(nativeBuild);
        Index emptyIndex = new Indexer().complete();

        new VaadinQuarkusProcessor().mapVaadinServletPaths(
                new BeanArchiveIndexBuildItem(emptyIndex, emptyIndex, Set.of()),
                nativeConfig, servlets::add, vaadinServlets::add);

        assertEquals(1, servlets.size());
    }
}
