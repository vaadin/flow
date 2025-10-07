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
package com.vaadin.flow.internal;

import java.beans.PropertyDescriptor;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class BeanUtilTest {

    public interface FirstInterface {
        default void setOneInterface(boolean bothInterfaces) {
        }

        default boolean isOneInterface() {
            return true;
        }

        default void setExistsInAllPlaces(boolean visible) {
        }

        default boolean isExistsInAllPlaces() {
            return true;
        }
    }

    public interface SecondInterface {
        default void setExistsInAllPlaces(boolean visible) {
        }

        default boolean isExistsInAllPlaces() {
            return true;
        }
    }

    public class TestSomething implements FirstInterface, SecondInterface {
        public void setExistsInAllPlaces(boolean visible) {
        }

        public boolean isExistsInAllPlaces() {
            return true;
        }
    }

    @Test
    public void duplicatesAreRemoved() throws Exception {
        List<PropertyDescriptor> descriptors = BeanUtil
                .getBeanPropertyDescriptors(TestSomething.class);
        List<PropertyDescriptor> existsInAllPlacesProperties = descriptors
                .stream()
                .filter(desc -> desc.getName().equals("existsInAllPlaces"))
                .toList();
        Assert.assertEquals(
                "There should be only one 'existsInAllPlaces' property descriptor",
                1, existsInAllPlacesProperties.size());

        // The property from the class should be retained
        // but we cannot test this as some introspector implementations
        // return the read method from the interface when introspecting the
        // class
        // Assert.assertEquals(TestSomething.class,
        // existsInAllPlacesProperties.get(0).getReadMethod().getDeclaringClass());

        List<PropertyDescriptor> oneInterfaceProperties = descriptors.stream()
                .filter(desc -> desc.getName().equals("oneInterface")).toList();
        Assert.assertEquals(
                "There should be only one 'oneInterface' property descriptor",
                1, oneInterfaceProperties.size());

        PropertyDescriptor oneInterfaceProperty = oneInterfaceProperties.get(0);
        // The property from thedefault method
        Assert.assertEquals(FirstInterface.class,
                oneInterfaceProperty.getReadMethod().getDeclaringClass());

    }
}
