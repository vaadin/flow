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
package com.vaadin.flow.di;

import javax.servlet.ServletException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.di.LookupInitializer.ResourceProviderImpl;
import com.vaadin.flow.function.VaadinApplicationInitializationBootstrap;
import com.vaadin.flow.server.startup.ApplicationConfigurationFactory;
import com.vaadin.flow.server.startup.DefaultApplicationConfigurationFactory;
import com.vaadin.flow.server.startup.testdata.AnotherTestInstantiatorFactory;
import com.vaadin.flow.server.startup.testdata.OneMoreTestInstantiatorFactory;
import com.vaadin.flow.server.startup.testdata.TestInstantiatorFactory;
import com.vaadin.flow.server.startup.testdata.TestResourceProvider;

import elemental.json.Json;
import elemental.json.JsonObject;

public class LookupIntializerTest {

    private LookupInitializer initializer = new LookupInitializer();

    @Test(expected = IllegalStateException.class)
    public void createLookup_instantiatorsAreProvidedAsAService_lookupThrows()
            throws ServletException {
        // Java standard SPI is used to register several instantiators via
        // META-INF/services
        Lookup lookup = initializer.createLookup(new HashMap<>());

        lookup.lookup(InstantiatorFactory.class);
    }

    @Test(expected = IllegalStateException.class)
    public void createLookup_instantiatorsAreProvidedAsScannedClasses_lookupThrows()
            throws ServletException {
        Lookup lookup = initializer.createLookup(
                Collections.singletonMap(InstantiatorFactory.class,
                        Arrays.asList(TestInstantiatorFactory.class,
                                AnotherTestInstantiatorFactory.class)));

        lookup.lookup(InstantiatorFactory.class);
    }

    @Test
    public void initialize_noResourcePorvider_defaultResourceProviderIsCreated()
            throws ServletException, IOException {
        AtomicReference<Lookup> capture = new AtomicReference<>();
        initializer.initialize(null, new HashMap<>(), capture::set);

        Lookup lookup = capture.get();
        assertResourceProvider(lookup.lookup(ResourceProvider.class));
    }

    @Test
    public void ensureResourceProvider_defaultImplClassIsStoredAsAService() {
        HashMap<Class<?>, Collection<Class<?>>> map = new HashMap<>();
        initializer.ensureResourceProviders(map);

        Collection<Class<?>> collection = map.get(ResourceProvider.class);
        Assert.assertEquals(1, collection.size());
        Class<?> clazz = collection.iterator().next();
        Assert.assertEquals(ResourceProviderImpl.class, clazz);
    }

    @Test
    public void ensureResourceProvider_defaultImplClassIsProvided_defaultImplIsStoredAsAService() {
        HashMap<Class<?>, Collection<Class<?>>> map = new HashMap<>();
        map.put(ResourceProvider.class,
                Collections.singletonList(ResourceProviderImpl.class));
        initializer.ensureResourceProviders(map);

        Collection<Class<?>> collection = map.get(ResourceProvider.class);
        Assert.assertEquals(1, collection.size());
        Class<?> clazz = collection.iterator().next();
        Assert.assertEquals(ResourceProviderImpl.class, clazz);
    }

    @Test
    public void ensureApplicationConfigurationFactories_defaultFactoryOnly_defaultFactoryIsReturned()
            throws ServletException {
        HashMap<Class<?>, Collection<Class<?>>> map = new HashMap<>();
        map.put(ApplicationConfigurationFactory.class, Collections
                .singletonList(DefaultApplicationConfigurationFactory.class));
        initializer.ensureApplicationConfigurationFactories(map);

        Collection<Class<?>> factories = map
                .get(ApplicationConfigurationFactory.class);
        Assert.assertEquals(1, factories.size());
        Assert.assertEquals(DefaultApplicationConfigurationFactory.class,
                factories.iterator().next());
    }

    @Test
    public void ensureApplicationConfigurationFactories_noAvailableFactory_defaultFactoryIsReturned()
            throws ServletException {
        HashMap<Class<?>, Collection<Class<?>>> map = new HashMap<>();
        map.put(ApplicationConfigurationFactory.class, Collections.emptyList());
        initializer.ensureApplicationConfigurationFactories(map);

        Collection<Class<?>> collection = map
                .get(ApplicationConfigurationFactory.class);
        Assert.assertEquals(1, collection.size());
        Class<?> clazz = collection.iterator().next();
        Assert.assertEquals(DefaultApplicationConfigurationFactory.class,
                clazz);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void createLookup_createLookupIsInvoked_lookupcontainsProvidedServices()
            throws ServletException {
        HashMap<Class<?>, Collection<Class<?>>> map = new HashMap<>();
        map.put(List.class, Arrays.asList(ArrayList.class, LinkedList.class));
        map.put(ResourceProvider.class,
                Collections.singletonList(TestResourceProvider.class));

        Lookup lookup = initializer.createLookup(map);

        ResourceProvider resourceProvider = lookup
                .lookup(ResourceProvider.class);
        Assert.assertEquals(TestResourceProvider.class,
                resourceProvider.getClass());

        Collection<List> lists = lookup.lookupAll(List.class);
        Assert.assertEquals(2, lists.size());

        Iterator<List> iterator = lists.iterator();
        List next = iterator.next();
        Assert.assertEquals(ArrayList.class, next.getClass());

        next = iterator.next();
        Assert.assertEquals(LinkedList.class, next.getClass());
    }

    @Test
    public void createLookup_instantiatorsAreProvidedAsScannedClassAndAsAService_lookupReturnsTheProviderInstance_lookupAllReturnsAllInstances()
            throws ServletException {
        HashMap<Class<?>, Collection<Class<?>>> map = new HashMap<>();
        map.put(InstantiatorFactory.class,
                Collections.singleton(TestInstantiatorFactory.class));

        Lookup lookup = initializer.createLookup(map);

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

    @Test
    public void resourceProviderImpl_returnsClassPathResources()
            throws IOException {
        assertResourceProvider(new ResourceProviderImpl());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void initialize_callEnsureMethodsAndBootstrap()
            throws ServletException {
        LookupInitializer initializer = Mockito.spy(LookupInitializer.class);
        Map<Class<?>, Collection<Class<?>>> services = Mockito
                .mock(HashMap.class);
        VaadinApplicationInitializationBootstrap bootstrap = Mockito
                .mock(VaadinApplicationInitializationBootstrap.class);
        initializer.initialize(null, services, bootstrap);

        Mockito.verify(initializer)
                .ensureApplicationConfigurationFactories(services);
        Mockito.verify(initializer).ensureResourceProviders(services);
        Mockito.verify(bootstrap).bootstrap(Mockito.any());
    }

    private void assertResourceProvider(ResourceProvider resourceProvider)
            throws IOException {
        Assert.assertEquals(ResourceProviderImpl.class,
                resourceProvider.getClass());
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
}
