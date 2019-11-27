/*
 * Copyright 2000-2019 Vaadin Ltd.
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
package com.vaadin.generator.registry;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.generator.ComponentGenerator;
import com.vaadin.generator.metadata.ComponentBasicType;
import com.vaadin.generator.metadata.ComponentEventData;
import com.vaadin.generator.metadata.ComponentMetadata;
import com.vaadin.generator.metadata.ComponentPropertyData;

/**
 * Unit tests for {@link PropertyNameRemapRegistryTest}.
 */
public class PropertyNameRemapRegistryTest {

    private static final String TEST_COMPONENT_TAG = "test-tag";

    private String generatedSource;

    @Before
    public void init() throws NoSuchMethodException, SecurityException,
            IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchFieldException {
        Field registryField = PropertyNameRemapRegistry.class
                .getDeclaredField("REGISTRY");
        registryField.setAccessible(true);
        @SuppressWarnings("rawtypes")
        Map registry = (Map) registryField.get(null);
        registry.clear();

        Method putMethod = PropertyNameRemapRegistry.class.getDeclaredMethod(
                "put", String.class, String.class, String.class);
        putMethod.setAccessible(true);
        putMethod.invoke(null, TEST_COMPONENT_TAG, "original-property-name",
                "renamed");
        putMethod.invoke(null, TEST_COMPONENT_TAG, "value", "other-value");
        putMethod.invoke(null, TEST_COMPONENT_TAG, "map-to-value", "value");

        ComponentGenerator generator = new ComponentGenerator();

        ComponentMetadata componentMetadata = new ComponentMetadata();
        componentMetadata.setName("test-name");
        componentMetadata.setTag(TEST_COMPONENT_TAG);
        componentMetadata.setBaseUrl("");

        ComponentPropertyData propertyData = new ComponentPropertyData();
        propertyData.setName("original-property-name");
        propertyData.setType(Collections.singleton(ComponentBasicType.STRING));
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
                TEST_COMPONENT_TAG, "mapping that doesn't exist").isPresent());
        Assert.assertFalse(PropertyNameRemapRegistry
                .getOptionalMappingFor("mapping that doesn't exist",
                        "mapping that doesn't exist")
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
                generatedSource.contains("setRenamed(String renamed)"));
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
                "Renamed property change listener should internally use the original property name",
                StringUtils.deleteWhitespace(generatedSource).contains(
                        "addPropertyChangeListener(\"originalPropertyName\","));
    }

    @Test
    public void hasValueWithRemapping_remapFromValueToOther() {
        ComponentGenerator generator = new ComponentGenerator();

        ComponentMetadata componentMetadata = new ComponentMetadata();
        componentMetadata.setName("test-name");
        componentMetadata.setTag(TEST_COMPONENT_TAG);
        componentMetadata.setBaseUrl("");

        ComponentPropertyData propertyData = new ComponentPropertyData();
        propertyData.setName("value");
        propertyData.setType(Collections.singleton(ComponentBasicType.STRING));
        componentMetadata.setProperties(Arrays.asList(propertyData));

        ComponentEventData eventData = new ComponentEventData();
        eventData.setName("value-changed");
        componentMetadata.setEvents(Arrays.asList(eventData));

        String generated = generator.generateClass(componentMetadata,
                "com.my.test", null);

        Assert.assertFalse(
                "Remapped value property should not generate the HasValue interface",
                generated.contains("HasValue"));
        Assert.assertTrue("Remapped change event should be generated",
                generated.contains("OtherValueChangeEvent"));
    }

    @Test
    public void hasValueWithRemapping_remapFromOtherToValue() {
        ComponentGenerator generator = new ComponentGenerator();

        ComponentMetadata componentMetadata = new ComponentMetadata();
        componentMetadata.setName("test-name");
        componentMetadata.setTag(TEST_COMPONENT_TAG);
        componentMetadata.setBaseUrl("");

        ComponentPropertyData propertyData = new ComponentPropertyData();
        propertyData.setName("map-to-value");
        propertyData.setType(Collections.singleton(ComponentBasicType.STRING));
        componentMetadata.setProperties(Arrays.asList(propertyData));

        ComponentEventData eventData = new ComponentEventData();
        eventData.setName("map-to-value-changed");
        componentMetadata.setEvents(Arrays.asList(eventData));

        String generated = generator.generateClass(componentMetadata,
                "com.my.test", null);

        Assert.assertThat(generated, CoreMatchers
                .containsString("AbstractSinglePropertyField<R, T>"));

        Assert.assertFalse(
                "Remapped value change event should not be generated",
                generated.contains("ValueChangeEvent"));
    }
}
