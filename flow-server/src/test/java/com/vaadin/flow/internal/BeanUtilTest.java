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
