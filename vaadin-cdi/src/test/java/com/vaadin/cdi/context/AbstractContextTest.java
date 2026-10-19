/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.cdi.context;

import jakarta.enterprise.context.ContextNotActiveException;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.cdi.AbstractWeldTest;
import com.vaadin.cdi.util.BeanProvider;

public abstract class AbstractContextTest<T extends TestBean>
        extends AbstractWeldTest {

    private List<UnderTestContext> contexts;

    @BeforeEach
    public void setUp() {
        TestBean.resetCount();
        contexts = new ArrayList<>();
    }

    @AfterEach
    public void tearDown() {
        newContextUnderTest().tearDownAll();
        contexts = null;
    }

    @Test
    public void get_contextNotActive_ExceptionThrown() {
        Assertions.assertThrows(ContextNotActiveException.class, () -> {
            final T reference = BeanProvider
                    .getContextualReference(getBeanType());
            reference.getState();
        });
    }

    @Test
    public void get_sameContextActive_beanCreatedOnce() {
        createContext().activate();
        T referenceA = BeanProvider.getContextualReference(getBeanType());
        referenceA.setState("hello");
        Assertions.assertEquals("hello", referenceA.getState());
        T referenceB = BeanProvider.getContextualReference(getBeanType());
        Assertions.assertEquals("hello", referenceB.getState());
        Assertions.assertEquals(1, TestBean.getBeanCount());
    }

    @Test
    public void get_newContextActive_newBeanCreated() {
        createContext().activate();
        final T referenceA = BeanProvider.getContextualReference(getBeanType());
        referenceA.setState("hello");
        createContext().activate();
        if (isNormalScoped()) {
            // proxy delegates to the active context automatically
            Assertions.assertEquals("", referenceA.getState());
        } else {
            // pseudo scoped bean ignores active context after creation
            Assertions.assertEquals("hello", referenceA.getState());
        }
        final T referenceB = BeanProvider.getContextualReference(getBeanType());
        Assertions.assertEquals("", referenceB.getState());
        Assertions.assertEquals(2, TestBean.getBeanCount());
    }

    @Test
    public void destroy_beanExistsInContext_beanDestroyed() {
        final UnderTestContext contextUnderTestA = createContext();
        contextUnderTestA.activate();
        final T referenceA = BeanProvider.getContextualReference(getBeanType());
        referenceA.setState("hello");
        final UnderTestContext contextUnderTestB = createContext();
        contextUnderTestB.activate();
        final T referenceB = BeanProvider.getContextualReference(getBeanType());
        referenceB.setState("hello");
        Assertions.assertEquals(2, TestBean.getBeanCount());
        contextUnderTestA.destroy();
        Assertions.assertEquals(1, TestBean.getBeanCount());
        contextUnderTestB.destroy();
        Assertions.assertEquals(0, TestBean.getBeanCount());
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

    protected abstract boolean isNormalScoped();

    protected abstract Class<T> getBeanType();

}
