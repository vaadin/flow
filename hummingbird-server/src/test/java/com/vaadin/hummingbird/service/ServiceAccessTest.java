/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.service;

import java.util.Iterator;
import java.util.ServiceLoader;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Vaadin Ltd
 *
 */
public class ServiceAccessTest {

    @Test
    public void implementsOneService() {
        Assert.assertTrue(hasImpl(Iface1.class, Impl1.class));
    }

    @Test
    public void extendsOneService() {
        Assert.assertTrue(hasImpl(Class1.class, ClassImpl1.class));
    }

    @Test
    public void implementsTwoServices() {
        Assert.assertTrue(hasImpl(Iface1.class, Impl2.class));
        Assert.assertTrue(hasImpl(Iface2.class, Impl2.class));
    }

    @Test
    public void implementsAndExtends() {
        Assert.assertTrue(hasImpl(Iface1.class, ClassImpl2.class));
        Assert.assertTrue(hasImpl(Class2.class, ClassImpl2.class));
    }

    @Test
    public void inheritedService() {
        Assert.assertTrue(hasImpl(Iface1.class, Impl3.class));
    }

    @Test
    public void inheritedClassService() {
        Assert.assertTrue(hasImpl(Class1.class, ClassImpl3.class));
    }

    @Test
    public void overridenService_noUndeclaredService() {
        Assert.assertTrue(hasImpl(Iface2.class, Impl4.class));
        Assert.assertFalse(hasImpl(Iface1.class, Impl4.class));
    }

    @Test
    public void overridenClassService_noUndeclaredService() {
        Assert.assertTrue(hasImpl(ClassImpl3.class, ClassImpl4.class));
        Assert.assertFalse(hasImpl(ClassImpl1.class, ClassImpl4.class));
    }

    private <T> boolean hasImpl(Class<T> service, Class<?> impl) {
        ServiceLoader<T> loader = ServiceLoader.load(service);
        for (Iterator<T> iterator = loader.iterator(); iterator.hasNext();) {
            T next = iterator.next();
            if (next.getClass().equals(impl)) {
                return true;
            }
        }
        return false;
    }
}
