/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.generator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.generator.metadata.ComponentBasicType;
import com.vaadin.generator.metadata.ComponentEventData;
import com.vaadin.generator.metadata.ComponentMetadata;
import com.vaadin.generator.metadata.ComponentPropertyData;

/**
 * Unit tests for {@link PropertyNameRemapRegistryTest}.
 */
public class PropertyNameRemapRegistryTest {

    private static final String TEST_COMPONENT_NAME = "ThisShouldNeverBeInTheActualRegistry";

    private String generatedSource;

    @Before
    public void init() throws NoSuchMethodException, SecurityException,
            IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {
        Method putMethod = PropertyNameRemapRegistry.class.getDeclaredMethod(
                "put", String.class, String.class, String.class);
        putMethod.setAccessible(true);
        putMethod.invoke(null, TEST_COMPONENT_NAME, "original-property-name",
                "renamed");

        ComponentGenerator generator = new ComponentGenerator();

        ComponentMetadata componentMetadata = new ComponentMetadata();
        componentMetadata.setName(TEST_COMPONENT_NAME);
        componentMetadata.setTag("some-tag");
        componentMetadata.setBaseUrl("");

        ComponentPropertyData propertyData = new ComponentPropertyData();
        propertyData.setName("original-property-name");
        propertyData.setType(Arrays.asList(ComponentBasicType.STRING));
        componentMetadata.setProperties(Arrays.asList(propertyData));
        
        ComponentEventData eventData = new ComponentEventData();
        eventData.setName("original-property-name-changed");
        componentMetadata.setEvents(Arrays.asList(eventData));

        generatedSource = generator.generateClass(componentMetadata,
                "com.my.test", null);
    }

    @Test
    public void notInRegistry_returnsEmptyOptional() {
        Assert.assertFalse(PropertyNameRemapRegistry.getOptionalMappingFor(
                TEST_COMPONENT_NAME, "mapping that doesn't exist").isPresent());
        Assert.assertFalse(PropertyNameRemapRegistry
                .getOptionalMappingFor("not$in$the$registry", "neither$is$this")
                .isPresent());
    }

    @Test
    public void getterAndSetterRemapping() {
        Assert.assertTrue("Getter for renamed property should be present",
                generatedSource.contains("String getRenamed()"));
        Assert.assertTrue(
                "Getter should use the original property name for accessing the value through the element API",
                generatedSource.contains(
                "return getElement().getProperty(\"original-property-name\");"));

        Assert.assertTrue("Setter for renamed property should be present",
                generatedSource
                        .contains("setRenamed(java.lang.String renamed)"));
        Assert.assertTrue(
                "Setter should use the original property name for setting the value through the element API",
                generatedSource.contains(
                        "getElement().setProperty(\"original-property-name\","));
    }

    @Test
    public void eventRemapping() {
        Assert.assertTrue(
                "Change event class name for renamed property should be renamed",
                generatedSource
                .contains("public static class RenamedChangeEvent"));
        Assert.assertTrue(
                "Renamed change event class should still be bound to the original property name",
                generatedSource.contains(
                        "@DomEvent(\"original-property-name-changed\")"));
    }
}
