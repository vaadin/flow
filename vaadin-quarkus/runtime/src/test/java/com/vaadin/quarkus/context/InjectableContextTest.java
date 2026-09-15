/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.quarkus.context;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableContext;
import io.quarkus.arc.InjectableContext.ContextState;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.spi.CreationalContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.internal.ReflectTools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Basic tests for {@link InjectableContext} implementations.
 *
 * @param <C>
 *            a context type
 */
public abstract class InjectableContextTest<C extends InjectableContext> {

    private List<UnderTestContext> contexts;

    @SuppressWarnings("unchecked")
    final CreationalContext<TestBean> creationalContext = Mockito
            .mock(CreationalContext.class);
    final InjectableBean<TestBean> contextual = Mockito
            .mock(InjectableBean.class);

    private C context;

    private Set<TestBean> destroyedBeans = new HashSet<>();

    private int createdBeans;

    @BeforeEach
    public void setUp() {
        createdBeans = 0;
        contexts = new ArrayList<>();
        destroyedBeans.clear();

        Mockito.doAnswer(invocation -> createBean()).when(contextual)
                .create(creationalContext);

        Mockito.doAnswer(invocation -> {
            destroyedBeans.add(invocation.getArgument(0));
            return null;
        }).when(contextual).destroy(Mockito.any(),
                Mockito.eq(creationalContext));

        context = createQuarkusContext();
    }

    @AfterEach
    public void tearDown() {
        newContextUnderTest().tearDownAll();
        contexts = null;
    }

    @Test
    public void get_contextNotActive_ExceptionThrown() {
        Assertions.assertThrows(ContextNotActiveException.class, () -> {
            C context = createQuarkusContext();
            context.get(contextual);
        });
    }

    @Test
    public void get_sameContextActive_beanCreatedOnce() {
        createContext().activate();

        TestBean referenceA = context.get(contextual, creationalContext);
        referenceA.setState("hello");
        assertEquals("hello", referenceA.getState());
        TestBean referenceB = context.get(contextual, creationalContext);
        assertEquals("hello", referenceB.getState());
        assertEquals(0, destroyedBeans.size());

        referenceB = context.get(contextual);
        assertSame(referenceA, referenceB);

        assertEquals(1, createdBeans);
    }

    @Test
    public void get_newContextActive_newBeanCreated() {
        createContext().activate();

        TestBean referenceA = context.get(contextual, creationalContext);
        referenceA.setState("hello");

        createContext().activate();

        TestBean referenceB = context.get(contextual, creationalContext);
        assertEquals("", referenceB.getState());
        assertEquals(0, destroyedBeans.size());
        assertNotSame(referenceA, referenceB);

        referenceB = context.get(contextual);
        assertNotSame(referenceA, referenceB);

        assertEquals(2, createdBeans);
    }

    @Test
    public void destroyContext_beanExistsInContext_beanDestroyed() {
        destroyContext_beanExistsInContext_beanDestroyed(false);
    }

    @Test
    public void destroy_beanExistsInContext_beanDestroyed() {
        createContext().activate();

        TestBean referenceA = context.get(contextual, creationalContext);
        referenceA.setState("hello");

        context.destroy(contextual);
        assertEquals(1, destroyedBeans.size());
    }

    @Test
    public void destroyQuarkusContext_beanExistsInContext_beanDestroyed() {
        createContext().activate();

        TestBean referenceA = context.get(contextual, creationalContext);
        referenceA.setState("hello");

        context.destroy();
        assertEquals(1, destroyedBeans.size());
    }

    @Test
    public void getState_beanExistsInContext_contextualInstanceAndBeanAreReturned() {
        createContext().activate();

        TestBean reference = context.get(contextual, creationalContext);
        reference.setState("hello");

        ContextState state = context.getState();

        Map<InjectableBean<?>, Object> instances = state
                .getContextualInstances();
        assertNotNull(instances);
        assertSame(reference, instances.get(contextual));
    }

    protected UnderTestContext createContext() {
        UnderTestContext underTestContext = newContextUnderTest();
        /*
         * UnderTestContext implementations set fields to Vaadin
         * CurrentInstance. Need to hold a hard reference to prevent possible
         * GC, because CurrentInstance works with weak reference.
         */
        contexts.add(underTestContext);
        return underTestContext;
    }

    protected abstract UnderTestContext newContextUnderTest();

    protected abstract Class<C> getContextType();

    protected void destroyContext_beanExistsInContext_beanDestroyed(
            boolean bothContextDestroyed) {
        UnderTestContext contextUnderTestA = createContext();
        contextUnderTestA.activate();
        TestBean referenceA = context.get(contextual, creationalContext);

        referenceA.setState("hello");

        final UnderTestContext contextUnderTestB = createContext();
        contextUnderTestB.activate();
        TestBean referenceB = context.get(contextual, creationalContext);

        referenceB.setState("foo");

        assertEquals(2, createdBeans);

        contextUnderTestA.destroy();

        if (bothContextDestroyed) {
            assertEquals(2, destroyedBeans.size());
        } else {
            assertEquals(1, destroyedBeans.size());

            contextUnderTestB.destroy();
            assertEquals(2, destroyedBeans.size());
        }
    }

    protected Set<TestBean> getDestroyedBeans() {
        return destroyedBeans;
    }

    protected C getContext() {
        return context;
    }

    protected int getCreatedBeansCount() {
        return createdBeans;
    }

    private C createQuarkusContext() {
        return getContextType()
                .cast(ReflectTools.createInstance(getContextType()));
    }

    private TestBean createBean() {
        createdBeans++;
        return new TestBean();
    }
}
