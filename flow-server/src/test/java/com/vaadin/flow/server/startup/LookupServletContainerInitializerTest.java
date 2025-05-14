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
package com.vaadin.flow.server.startup;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.template.internal.DeprecatedPolymerPublishedEventHandler;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.LookupInitializer;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.function.VaadinApplicationInitializationBootstrap;
import com.vaadin.flow.router.RoutePathProvider;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.startup.testdata.TestResourceProvider;

import elemental.json.JsonObject;
import elemental.json.JsonValue;

public class LookupServletContainerInitializerTest {

    private LookupServletContainerInitializer initializer = new LookupServletContainerInitializer();

    public static class TestPolymerPublishedEventHandler
            implements DeprecatedPolymerPublishedEventHandler {

        @Override
        public boolean isTemplateModelValue(Component instance,
                JsonValue argValue, Class<?> convertedType) {
            return false;
        }

        @Override
        public Object getTemplateItem(Component template, JsonObject argValue,
                Type convertedType) {
            return null;
        }

    }

    public static class TestApplicationConfigurationFactory
            implements ApplicationConfigurationFactory {
        @Override
        public ApplicationConfiguration create(VaadinContext context) {
            return null;
        }
    }

    public static class TestRoutePathProvider implements RoutePathProvider {

        @Override
        public String getRoutePath(Class<?> navigationTarget) {
            return null;
        }

    }

    private static class TestLookup implements Lookup {

        private Map<Class<?>, Collection<Class<?>>> services;

        @Override
        public <T> T lookup(Class<T> serviceClass) {
            return null;
        }

        @Override
        public <T> Collection<T> lookupAll(Class<T> serviceClass) {
            return null;
        }

    }

    public static class TestLookupInitializer extends LookupInitializer {

        @Override
        public void initialize(VaadinContext context,
                Map<Class<?>, Collection<Class<?>>> services,
                VaadinApplicationInitializationBootstrap bootstrap)
                throws ServletException {
            TestLookup lookup = new TestLookup();
            lookup.services = services;
            bootstrap.bootstrap(lookup);
        }
    }

    @Test
    public void processLookupServletContainerInitializer_resourceProviderIsProvidedAsScannedClass_lookupReturnsTheProviderInstance()
            throws ServletException {
        Lookup lookup = mockLookup(TestResourceProvider.class);

        ResourceProvider provider = lookup.lookup(ResourceProvider.class);
        Assert.assertNotNull(provider);
        Assert.assertEquals(TestResourceProvider.class, provider.getClass());

        Collection<ResourceProvider> allProviders = lookup
                .lookupAll(ResourceProvider.class);
        Assert.assertEquals(1, allProviders.size());

        Assert.assertEquals(TestResourceProvider.class,
                allProviders.iterator().next().getClass());
    }

    @Test
    public void processLookupServletContainerInitializer_polymerPublishedEventHandlerIsProvidedAsScannedClass_lookupReturnsTheProviderInstance()
            throws ServletException {
        Lookup lookup = mockLookup(TestPolymerPublishedEventHandler.class);

        DeprecatedPolymerPublishedEventHandler handler = lookup
                .lookup(DeprecatedPolymerPublishedEventHandler.class);
        Assert.assertNotNull(handler);
        Assert.assertEquals(TestPolymerPublishedEventHandler.class,
                handler.getClass());

        Collection<DeprecatedPolymerPublishedEventHandler> allHandlers = lookup
                .lookupAll(DeprecatedPolymerPublishedEventHandler.class);
        Assert.assertEquals(1, allHandlers.size());

        Assert.assertEquals(TestPolymerPublishedEventHandler.class,
                allHandlers.iterator().next().getClass());
    }

    @Test
    public void processLookupServletContainerInitializer_routePathProviderIsProvidedAsScannedClass_lookupReturnsTheProviderInstance()
            throws ServletException {
        Lookup lookup = mockLookup(TestRoutePathProvider.class);

        RoutePathProvider handler = lookup.lookup(RoutePathProvider.class);
        Assert.assertNotNull(handler);
        Assert.assertEquals(TestRoutePathProvider.class, handler.getClass());

        Collection<RoutePathProvider> allHandlers = lookup
                .lookupAll(RoutePathProvider.class);
        Assert.assertEquals(1, allHandlers.size());

        Assert.assertEquals(TestRoutePathProvider.class,
                allHandlers.iterator().next().getClass());
    }

    @Test
    public void processLookupServletContainerInitializer_contextHasDeferredInitializers_runInitializersAndClearAttribute()
            throws ServletException {
        ServletContext context = Mockito.mock(ServletContext.class);
        DeferredServletContextInitializers deferredInitializers = Mockito
                .mock(DeferredServletContextInitializers.class);
        Mockito.when(context.getAttribute(
                DeferredServletContextInitializers.class.getName()))
                .thenReturn(deferredInitializers);
        mockLookup(context);

        Mockito.verify(deferredInitializers).runInitializers(context);
        Mockito.verify(context).removeAttribute(
                DeferredServletContextInitializers.class.getName());
    }

    @Test
    public void processApplicationConfigurationFactory_factoryIsProvided_providedFactoryIsCreated()
            throws ServletException {
        Lookup lookup = mockLookup(DefaultApplicationConfigurationFactory.class,
                TestApplicationConfigurationFactory.class);

        ApplicationConfigurationFactory config = lookup
                .lookup(ApplicationConfigurationFactory.class);
        Assert.assertNotNull(config);
        Assert.assertTrue(
                config instanceof TestApplicationConfigurationFactory);
    }

    @Test
    public void process_customLookupInitializerIsProvided_servicesHasCustomImpls_customInitializerIsCalledWithProvidedImpls()
            throws ServletException {
        Lookup lookup = mockLookup(TestPolymerPublishedEventHandler.class,
                TestResourceProvider.class,
                TestApplicationConfigurationFactory.class,
                TestLookupInitializer.class);
        Assert.assertTrue(lookup instanceof TestLookup);

        TestLookup customLookup = (TestLookup) lookup;
        Map<Class<?>, Collection<Class<?>>> services = customLookup.services;
        Assert.assertFalse(services.containsKey(LookupInitializer.class));
        Assert.assertEquals(TestPolymerPublishedEventHandler.class,
                services.get(DeprecatedPolymerPublishedEventHandler.class)
                        .iterator().next());
        Assert.assertEquals(TestResourceProvider.class,
                services.get(ResourceProvider.class).iterator().next());
        Assert.assertEquals(TestApplicationConfigurationFactory.class, services
                .get(ApplicationConfigurationFactory.class).iterator().next());
    }

    @Test
    public void getServiceTypes_getServiceTypesIsInvoked_initializerIsInvokdedWithProvidedServices()
            throws ServletException {
        initializer = new LookupServletContainerInitializer() {
            @Override
            protected Collection<Class<?>> getServiceTypes() {
                return Arrays.asList(List.class);
            }
        };

        Lookup lookup = mockLookup(TestLookupInitializer.class,
                ArrayList.class);

        Assert.assertTrue(lookup instanceof TestLookup);

        TestLookup customLookup = (TestLookup) lookup;
        Map<Class<?>, Collection<Class<?>>> services = customLookup.services;
        Assert.assertFalse(services.containsKey(LookupInitializer.class));
        Assert.assertTrue(services.containsKey(List.class));

        Collection<Class<?>> collection = services.get(List.class);
        Assert.assertEquals(1, collection.size());
        Assert.assertEquals(ArrayList.class, collection.iterator().next());
    }

    @Test(expected = ServletException.class)
    public void process_classSetIsNull_throws() throws ServletException {
        initializer.process(null, Mockito.mock(ServletContext.class));
    }

    private Lookup mockLookup(ServletContext context, Class<?>... classes)
            throws ServletException {
        ArgumentCaptor<Lookup> lookupCapture = ArgumentCaptor
                .forClass(Lookup.class);

        Stream<Class<? extends Object>> stream = Stream
                .concat(Stream.of(LookupInitializer.class), Stream.of(classes));
        initializer.process(stream.collect(Collectors.toSet()), context);

        Mockito.verify(context).setAttribute(Mockito.eq(Lookup.class.getName()),
                lookupCapture.capture());

        return lookupCapture.getValue();
    }

    private Lookup mockLookup(Class<?>... classes) throws ServletException {
        return mockLookup(Mockito.mock(ServletContext.class), classes);
    }
}
