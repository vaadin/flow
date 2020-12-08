/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.template.internal.DeprecatedPolymerPublishedEventHandler;
import com.vaadin.flow.di.InstantiatorFactory;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.server.startup.testdata.AnotherTestInstantiatorFactory;
import com.vaadin.flow.server.startup.testdata.OneMoreTestInstantiatorFactory;
import com.vaadin.flow.server.startup.testdata.TestInstantiatorFactory;
import com.vaadin.flow.server.startup.testdata.TestResourceProvider;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

public class LookupInitializerTest {

    private LookupInitializer initializer = new LookupInitializer();

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

    @Test
    public void processLookupInitializer_resourceProviderIsProvidedAsScannedClass_lookupReturnsTheProviderInstance()
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
    public void processLookupInitializer_rpolymerPubkishedEventHandlerIsProvidedAsScannedClass_lookupReturnsTheProviderInstance()
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
    public void processLookupInitializer_instantiatorsAreProvidedAsScannedClassAndAsAService_lookupReturnsTheProviderInstance_lookupAllReturnsAllInstances()
            throws ServletException {
        Lookup lookup = mockLookup(TestInstantiatorFactory.class);

        InstantiatorFactory factory = lookup.lookup(InstantiatorFactory.class);
        Assert.assertNotNull(factory);
        Assert.assertEquals(TestInstantiatorFactory.class, factory.getClass());

        Collection<InstantiatorFactory> factories = lookup
                .lookupAll(InstantiatorFactory.class);

        Assert.assertEquals(3, factories.size());

        Iterator<InstantiatorFactory> iterator = factories.iterator();
        Assert.assertEquals(TestInstantiatorFactory.class,
                iterator.next().getClass());

        Set<Class<?>> factoryClasses = new HashSet<>();
        factoryClasses.add(iterator.next().getClass());
        factoryClasses.add(iterator.next().getClass());

        Assert.assertTrue(
                factoryClasses.contains(AnotherTestInstantiatorFactory.class));
        Assert.assertTrue(
                factoryClasses.contains(OneMoreTestInstantiatorFactory.class));
    }

    @Test(expected = IllegalStateException.class)
    public void processLookupInitializer_instantiatorsAreProvidedAsAService_lookupThrows()
            throws ServletException {
        Lookup lookup = mockLookup();

        lookup.lookup(InstantiatorFactory.class);
    }

    @Test(expected = IllegalStateException.class)
    public void processLookupInitializer_instantiatorsAreProvidedAsScannedClasses_lookupThrows()
            throws ServletException {
        Lookup lookup = mockLookup(TestInstantiatorFactory.class,
                AnotherTestInstantiatorFactory.class);

        lookup.lookup(InstantiatorFactory.class);
    }

    @Test
    public void processLookupInitializer_noResourcePorvider_defaultResourceProviderIsCreated()
            throws ServletException, IOException {
        Lookup lookup = mockLookup();

        ResourceProvider resourceProvider = lookup
                .lookup(ResourceProvider.class);

        // ======== resourceProvider.getApplicationResource(s)(String)
        URL applicationResource = resourceProvider
                .getApplicationResource("resource-provider/some-resource.json");

        Assert.assertNotNull(applicationResource);

        List<URL> resources = resourceProvider.getApplicationResources(
                "resource-provider/some-resource.json");

        Assert.assertEquals(1, resources.size());

        Assert.assertNotNull(resources.get(0));

        URL nonExistent = resourceProvider
                .getApplicationResource("resource-provider/non-existent.txt");

        Assert.assertNull(nonExistent);

        // =========== resourceProvider.getClientResource

        URL clientResource = resourceProvider
                .getClientResource("resource-provider/some-resource.json");

        Assert.assertNotNull(clientResource);

        InputStream stream = resourceProvider.getClientResourceAsStream(
                "resource-provider/some-resource.json");

        String content = IOUtils.readLines(stream, StandardCharsets.UTF_8)
                .stream().collect(Collectors.joining("\n"));
        JsonObject object = Json.parse(content);
        Assert.assertTrue(object.getBoolean("client-resource"));
    }

    @Test
    public void processLookupInitializer_contextHasDeferredInitializers_runInitializersAndClearAttribute()
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
    public void createLookup_createLookupIsInvoked_lookupIsInstantiatedCreateLookup()
            throws ServletException {
        Lookup lookup = Mockito.mock(Lookup.class);
        initializer = new LookupInitializer() {
            @Override
            protected Lookup createLookup(
                    Map<Class<?>, Collection<Object>> services) {
                Assert.assertTrue(services.containsKey(ResourceProvider.class));
                Assert.assertTrue(
                        services.containsKey(InstantiatorFactory.class));
                Assert.assertTrue(services.containsKey(
                        DeprecatedPolymerPublishedEventHandler.class));
                Collection<Object> serviceObjects = services
                        .get(ResourceProvider.class);
                Assert.assertEquals(1, serviceObjects.size());
                Assert.assertTrue(TestResourceProvider.class
                        .isInstance(serviceObjects.iterator().next()));
                return lookup;
            }

            @Override
            protected Collection<Class<?>> getServiceTypes() {
                return new LookupInitializer().getServiceTypes();
            }
        };

        Lookup resultLookup = mockLookup(TestResourceProvider.class);
        Assert.assertSame(lookup, resultLookup);
    }

    @Test
    public void getServiceTypes_getServiceTypesIsInvoked_lookupContainsOnlyReturnedServiceTypes()
            throws ServletException {
        initializer = new LookupInitializer() {
            @Override
            protected Lookup createLookup(
                    Map<Class<?>, Collection<Object>> services) {
                Assert.assertFalse(
                        services.containsKey(ResourceProvider.class));
                Assert.assertFalse(
                        services.containsKey(InstantiatorFactory.class));
                Assert.assertFalse(services.containsKey(
                        DeprecatedPolymerPublishedEventHandler.class));
                Collection<Object> serviceObjects = services.get(List.class);
                Assert.assertEquals(1, serviceObjects.size());
                Assert.assertTrue(ArrayList.class
                        .isInstance(serviceObjects.iterator().next()));
                return super.createLookup(services);
            }

            @Override
            protected Collection<Class<?>> getServiceTypes() {
                return Collections.singleton(List.class);
            }
        };

        Lookup resultLookup = mockLookup(ArrayList.class);
        List<?> list = resultLookup.lookup(List.class);
        Assert.assertTrue(list instanceof ArrayList<?>);
    }

    private Lookup mockLookup(ServletContext context, Class<?>... classes)
            throws ServletException {
        ArgumentCaptor<Lookup> lookupCapture = ArgumentCaptor
                .forClass(Lookup.class);

        initializer.process(new HashSet<>(Arrays.asList(classes)), context);

        Mockito.verify(context).setAttribute(Mockito.eq(Lookup.class.getName()),
                lookupCapture.capture());

        return lookupCapture.getValue();
    }

    private Lookup mockLookup(Class<?>... classes) throws ServletException {
        return mockLookup(Mockito.mock(ServletContext.class), classes);
    }
}
