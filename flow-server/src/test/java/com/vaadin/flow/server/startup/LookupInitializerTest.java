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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.vaadin.flow.di.InstantiatorFactory;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.startup.testdata.AnotherTestInstantiatorFactory;
import com.vaadin.flow.server.startup.testdata.OneMoreTestInstantiatorFactory;
import com.vaadin.flow.server.startup.testdata.TestInstantiatorFactory;
import com.vaadin.flow.server.startup.testdata.TestResourceProvider;

import elemental.json.Json;
import elemental.json.JsonObject;

public class LookupInitializerTest {

    private LookupInitializer initializer = new LookupInitializer();

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

        // ======== resourceProvider.getApplicationResource(s)(Class, String)
        URL applicationResource = resourceProvider.getApplicationResource(
                LookupInitializerTest.class,
                "resource-provider/some-resource.json");

        Assert.assertNotNull(applicationResource);

        List<URL> resources = resourceProvider.getApplicationResources(
                LookupInitializerTest.class,
                "resource-provider/some-resource.json");

        Assert.assertEquals(1, resources.size());

        Assert.assertNotNull(resources.get(0));

        URL nonExistent = resourceProvider.getApplicationResource(
                LookupInitializerTest.class,
                "resource-provider/non-existent.txt");

        Assert.assertNull(nonExistent);

        // ======== resourceProvider.getApplicationResource(s)(Object, String)
        String path = "foo/bar";

        // == sub test: check VaadinService instance
        VaadinService service = Mockito.mock(VaadinService.class);
        ClassLoader loader = Mockito.mock(ClassLoader.class);

        URL singleResourceURL = new URL("file:/baz");
        Mockito.when(loader.getResource(path)).thenReturn(singleResourceURL);
        Mockito.when(loader.getResources(path))
                .thenReturn(Collections.enumeration(Arrays
                        .asList(new URL("file:/foo"), new URL("file:/bar"))));

        Mockito.when(service.getClassLoader()).thenReturn(loader);
        URL serviceResource = resourceProvider.getApplicationResource(service,
                path);
        Assert.assertEquals(singleResourceURL, serviceResource);

        List<URL> serviceResources = resourceProvider
                .getApplicationResources(service, path);

        Assert.assertEquals(2, serviceResources.size());

        Assert.assertEquals(new URL("file:/foo"), serviceResources.get(0));
        Assert.assertEquals(new URL("file:/bar"), serviceResources.get(1));

        // == sub test: check non VaadinService instance
        applicationResource = resourceProvider.getApplicationResource(
                initializer, "resource-provider/some-resource.json");

        Assert.assertNotNull(applicationResource);

        resources = resourceProvider.getApplicationResources(initializer,
                "resource-provider/some-resource.json");

        Assert.assertEquals(1, resources.size());

        Assert.assertNotNull(resources.get(0));

        nonExistent = resourceProvider.getApplicationResource(initializer,
                "resource-provider/non-existent.txt");

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
