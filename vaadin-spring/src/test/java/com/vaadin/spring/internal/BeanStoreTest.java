/*
 * Copyright 2015-2017 The original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vaadin.spring.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectFactory;

public class BeanStoreTest {

    String beanName1 = "TestBean1";
    String beanName2 = "TestBean2";
    String beanStoreName = "TestBeanStore";
    Object bean1 = new Object();
    Object bean2 = new Object();
    ObjectFactory<Object> objFactory1;
    ObjectFactory<Object> objFactory2;
    BeanStore beanStore = new BeanStore(beanStoreName);

    @SuppressWarnings("unchecked")
    @Before
    public void initBeanFactory() {
        // Create dummy bean factories that return a simple bean
        objFactory1 = mock(ObjectFactory.class);
        when(objFactory1.getObject()).thenReturn(bean1);
        objFactory2 = mock(ObjectFactory.class);
        when(objFactory2.getObject()).thenReturn(bean2);
    }

    @Test
    public void testCreateBean() {
        assertSame(bean1, beanStore.create(beanName1, objFactory1));
    }

    @Test
    public void testGetBean() {
        assertSame(bean1, beanStore.get(beanName1, objFactory1));

        assertNotSame(bean2, beanStore.get(beanName1, objFactory1));

        assertNotSame(bean1, beanStore.get(beanName2, objFactory2));
    }

    @Test
    public void testGetConsistent() {
        // Make sure the same name gives the same instance
        assertSame(beanStore.get(beanName1, objFactory1),
                beanStore.get(beanName1, objFactory1));
    }

    @Test
    public void testGetSameInstance() {

        // First time should at most create the factory once
        beanStore.get(beanName1, objFactory1);

        // Make sure it will not be created more than once
        beanStore.get(beanName1, objFactory1);

        verify(objFactory1, atMost(1)).getObject();
    }

    @Test
    public void testRemoveBean() {
        // Make sure to create a new bean if not already there
        beanStore.get(beanName1, objFactory1);

        // Make sure the bean is removed
        assertSame(bean1, beanStore.remove(beanName1));

        // Make sure it's already removed
        assertNull(beanStore.remove(beanName1));
    }

    @Test
    public void testRegisterDestructionCallbackAndDestroy() {

        Runnable destructionCallback = mock(Runnable.class);

        beanStore.registerDestructionCallback(beanStoreName,
                destructionCallback);

        // If registered it will be destroyed
        beanStore.destroy();

        // Make sure destructionCallback won't run again
        beanStore.destroy();

        // Make sure destroy() ran the registered destructionCallback once
        verify(destructionCallback).run();
    }

    @Test
    public void testDestroyClearStore() {

        // Make sure to create a new bean if not already there
        beanStore.get(beanName1, objFactory1);

        beanStore.destroy();

        // The bean should not be there anymore
        assertNull(beanStore.remove(beanName1));
    }

    @Test
    public void testToStringConsistent() {
        // Make sure the format is always the same
        assertEquals(beanStore.toString(), beanStore.toString());
    }

    @After
    public void validate() {
        Mockito.validateMockitoUsage();
    }
}
