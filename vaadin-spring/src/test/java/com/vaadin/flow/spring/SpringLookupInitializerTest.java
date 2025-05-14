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
package com.vaadin.flow.spring;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.web.context.WebApplicationContext;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.function.VaadinApplicationInitializationBootstrap;
import com.vaadin.flow.server.VaadinServletContext;

public class SpringLookupInitializerTest {

    private SpringLookupInitializer initializer = new SpringLookupInitializer();

    private WebApplicationContext webAppContext = Mockito
            .mock(WebApplicationContext.class);

    private VaadinServletContext context = Mockito
            .mock(VaadinServletContext.class);

    private ServletContext servletContext = Mockito.mock(ServletContext.class);

    public static class TestSpi {

    }

    public static class ServiceImpl extends TestSpi {

    }

    @Before
    public void setUp() {
        Mockito.when(context.getContext()).thenReturn(servletContext);
        Mockito.when(servletContext.getAttribute(
                WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
                .thenReturn(webAppContext);
    }

    @Test
    public void initialize_contextIsAvailable_bootstrapImmidiately()
            throws ServletException {
        VaadinApplicationInitializationBootstrap bootstrap = Mockito
                .mock(VaadinApplicationInitializationBootstrap.class);
        initializer.initialize(context, new HashMap<>(), bootstrap);

        Mockito.verify(bootstrap).bootstrap(Mockito.any());
    }

    @Test
    public void initialize_contextIsNotAvailable_bootstrapIsPostponed()
            throws ServletException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        Mockito.when(servletContext.getAttribute(
                WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
                .thenReturn(null);

        VaadinApplicationInitializationBootstrap bootstrap = Mockito
                .mock(VaadinApplicationInitializationBootstrap.class);
        initializer.initialize(context, new HashMap<>(), bootstrap);

        Mockito.verifyNoInteractions(bootstrap);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        Mockito.verify(context).setAttribute(Mockito.any(), captor.capture());

        Object value = captor.getValue();
        Assert.assertTrue(value.getClass().getName()
                .startsWith(SpringLookupInitializer.class.getName()));

        List<Method> methods = Stream.of(value.getClass().getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers())
                        && !Modifier.isStatic(method.getModifiers())
                        && !method.isSynthetic())
                .collect(Collectors.toList());

        // self check
        Assert.assertEquals(1, methods.size());

        // at this moment the context should be available
        Mockito.when(servletContext.getAttribute(
                WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
                .thenReturn(webAppContext);

        methods.get(0).invoke(value);

        Mockito.verify(bootstrap).bootstrap(Mockito.any());
    }

    @Test
    public void createLookup_noBeans_lookup_returnsServiceImpl() {
        Lookup lookup = initializer.createLookup(context,
                Collections.singletonMap(List.class,
                        Collections.singletonList(ArrayList.class)));
        assertSingleServiceInLookup(lookup, List.class, ArrayList.class);
    }

    @Test(expected = IllegalStateException.class)
    public void createLookup_severalBeansAndDefaultServiceImpl_lookupThrows() {
        Map<String, TestSpi> map = new HashMap<>();
        map.put("foo", new TestSpi());
        map.put("bar", new TestSpi());
        Mockito.when(webAppContext.getBeansOfType(TestSpi.class))
                .thenReturn(map);
        Lookup lookup = initializer.createLookup(context,
                Collections.singletonMap(TestSpi.class,
                        Collections.singletonList(ServiceImpl.class)));

        lookup.lookup(TestSpi.class);
    }

    @Test
    public void createLookup_oneBeanAndDefaultServiceImpl_lookupReturnsTheBeanAndIgnoresDefaultServiceImpl() {
        Mockito.when(webAppContext.getBeansOfType(TestSpi.class))
                .thenReturn(Collections.singletonMap("foo", new TestSpi()));
        Lookup lookup = initializer.createLookup(context,
                Collections.singletonMap(TestSpi.class,
                        Collections.singletonList(ServiceImpl.class)));

        Assert.assertEquals(TestSpi.class,
                lookup.lookup(TestSpi.class).getClass());

        Collection<TestSpi> services = lookup.lookupAll(TestSpi.class);
        Assert.assertEquals(2, services.size());
        Iterator<TestSpi> iterator = services.iterator();
        Assert.assertEquals(TestSpi.class, iterator.next().getClass());
        Assert.assertEquals(ServiceImpl.class, iterator.next().getClass());
    }

    @Test
    public void createLookup_oneBean_noServiceImpl_lookup_returnsBean() {
        Mockito.when(webAppContext.getBeansOfType(TestSpi.class))
                .thenReturn(Collections.singletonMap("foo", new TestSpi()));
        Lookup lookup = initializer.createLookup(context,
                Collections.emptyMap());

        assertSingleServiceInLookup(lookup, TestSpi.class, TestSpi.class);
    }

    @Test(expected = IllegalStateException.class)
    public void createLookup_oneBeanAndOneServiceImpl_lookupThrows() {
        Mockito.when(webAppContext.getBeansOfType(List.class))
                .thenReturn(Collections.singletonMap("foo", new LinkedList()));
        Lookup lookup = initializer.createLookup(context,
                Collections.singletonMap(List.class,
                        Collections.singletonList(ArrayList.class)));

        lookup.lookup(List.class);
    }

    @Test
    public void createLookup_oneBeanExtendsServiceImpl_lookupReturnsBeanAndIgnoresServiceImpl() {
        Mockito.when(webAppContext.getBeansOfType(List.class))
                .thenReturn(Collections.singletonMap("foo", new Stack<>()));
        Lookup lookup = initializer.createLookup(context,
                Collections.singletonMap(List.class,
                        Collections.singletonList(Vector.class)));

        // Stack extends Vector so Stack bean is a Vector and Vector service
        // impl class is ignored

        assertSingleServiceInLookup(lookup, List.class, Stack.class);
    }

    @Test
    public void createLookup_lookupAll_beans_oneBeanExtendsServiceImpl_beansAndServiceImplsAreReturned_overriddenServiceImplIsIgnored() {
        Map<String, List> map = new HashMap<>();
        map.put("foo", new LinkedList<>());
        map.put("bar", new Stack());
        Mockito.when(webAppContext.getBeansOfType(List.class)).thenReturn(map);

        Lookup lookup = initializer.createLookup(context,
                Collections.singletonMap(List.class,
                        Arrays.asList(Vector.class, ArrayList.class)));

        Collection<List> lists = lookup.lookupAll(List.class);
        Assert.assertEquals(3, lists.size());
        Set<?> serviceClasses = lists.stream().map(Object::getClass)
                .collect(Collectors.toSet());

        Assert.assertTrue(serviceClasses.contains(LinkedList.class));
        Assert.assertTrue(serviceClasses.contains(Stack.class));
        Assert.assertTrue(serviceClasses.contains(ArrayList.class));
    }

    private <T> void assertSingleServiceInLookup(Lookup lookup, Class<T> spi,
            Class<? extends T> impl) {
        Assert.assertEquals(impl, lookup.lookup(spi).getClass());

        Collection<T> services = lookup.lookupAll(spi);
        Assert.assertEquals(1, services.size());
        Assert.assertEquals(impl, services.iterator().next().getClass());
    }

}
