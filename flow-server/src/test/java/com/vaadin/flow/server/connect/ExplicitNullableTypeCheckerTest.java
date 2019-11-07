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

package com.vaadin.flow.server.connect;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class ExplicitNullableTypeCheckerTest {
    private ExplicitNullableTypeChecker explicitNullableTypeChecker;
    private Type stringListType;
    private Type stringArrayType;

    @Before
    public void setup() throws NoSuchMethodException {
        explicitNullableTypeChecker = new ExplicitNullableTypeChecker();

        stringListType = getClass()
                .getMethod("parametrizedListMethod", String[].class)
                .getGenericReturnType();

        stringArrayType = getClass().getMethod("arrayMethod", String[].class)
                .getGenericReturnType();
    }

    @Test
    public void should_ReturnNull_When_GivenNonNullValue_ForPrimitiveType() {
        Assert.assertNull(
                explicitNullableTypeChecker.checkValueForType(0, byte.class));
        Assert.assertNull(
                explicitNullableTypeChecker.checkValueForType(0, short.class));
        Assert.assertNull(
                explicitNullableTypeChecker.checkValueForType(0, int.class));
        Assert.assertNull(
                explicitNullableTypeChecker.checkValueForType(0L, long.class));
        Assert.assertNull(explicitNullableTypeChecker.checkValueForType(0.0f,
                float.class));
        Assert.assertNull(explicitNullableTypeChecker.checkValueForType(0.0d,
                double.class));
        Assert.assertNull(explicitNullableTypeChecker.checkValueForType(true,
                boolean.class));
        Assert.assertNull(
                explicitNullableTypeChecker.checkValueForType('a', char.class));
    }

    @Test
    public void should_ReturnNull_When_GivenNullValue_ForVoidType() {
        Assert.assertNull(explicitNullableTypeChecker.checkValueForType(null,
                void.class));
        Assert.assertNull(explicitNullableTypeChecker.checkValueForType(null,
                Void.class));
    }

    @Test
    public void should_ReturnNull_When_GivenNonNullValue_ForStringType() {
        Assert.assertNull(explicitNullableTypeChecker.checkValueForType("",
                String.class));
    }

    @Test
    public void should_ReturnError_When_GivenNullValue_ForStringType() {
        String error = explicitNullableTypeChecker.checkValueForType(null,
                String.class);

        Assert.assertNotNull(error);
        Assert.assertTrue(error.contains("null"));
        Assert.assertTrue(error.contains("String"));
    }

    @Test
    public void should_ReturnNull_When_GivenNonNullValue_ForDateType() {
        Assert.assertNull(explicitNullableTypeChecker
                .checkValueForType(new Date(), Date.class));
    }

    @Test
    public void should_ReturnError_When_GivenNullValue_ForDateType() {
        String error = explicitNullableTypeChecker.checkValueForType(null,
                Date.class);

        Assert.assertNotNull(error);
        Assert.assertTrue(error.contains("null"));
        Assert.assertTrue(error.contains("Date"));
    }

    @Test
    public void should_ReturnNull_When_GivenNonNullValue_ForDateTimeType() {
        Assert.assertNull(explicitNullableTypeChecker
                .checkValueForType(LocalDateTime.now(), LocalDateTime.class));
    }

    @Test
    public void should_ReturnError_When_GivenNullValue_ForDateTimeType() {
        String error = explicitNullableTypeChecker.checkValueForType(null,
                LocalDateTime.class);

        Assert.assertNotNull(error);
        Assert.assertTrue(error.contains("null"));
        Assert.assertTrue(error.contains("LocalDateTime"));
    }

    @Test
    public void should_ReturnNull_When_GivenNonNullValue_ForMapType() {
        Assert.assertNull(explicitNullableTypeChecker
                .checkValueForType(new HashMap<String, String>(), Map.class));
    }

    @Test
    public void should_ReturnError_When_GivenNullValue_ForMapType() {
        String error = explicitNullableTypeChecker.checkValueForType(null,
                Map.class);

        Assert.assertNotNull(error);
        Assert.assertTrue(error.contains("null"));
        Assert.assertTrue(error.contains("Map"));
    }

    @Test
    public void should_ReturnNull_When_GivenNonNullValue_ForObjectType() {
        Assert.assertNull(explicitNullableTypeChecker
                .checkValueForType(new Object(), Object.class));
    }

    @Test
    public void should_ReturnError_When_GivenNullValue_ForObjectType() {
        String error = explicitNullableTypeChecker.checkValueForType(null,
                Object.class);

        Assert.assertNotNull(error);
        Assert.assertTrue(error.contains("null"));
        Assert.assertTrue(error.contains("Object"));
    }

    @Test
    public void should_ReturnNull_When_GivenNonNullValue_ForOptionalType() {
        Assert.assertNull(explicitNullableTypeChecker
                .checkValueForType(Optional.empty(), Optional.class));
    }

    @Test
    public void should_ReturnError_When_GivenNullValue_ForOptionalType() {
        String error = explicitNullableTypeChecker.checkValueForType(null,
                Optional.class);

        Assert.assertNotNull(error);
        Assert.assertTrue(error.contains("null"));
        Assert.assertTrue(error.contains("Optional.empty"));
    }

    @Test
    public void should_ReturnNull_When_GivenNonNullValue_ForCollectionType() {
        Assert.assertNull(explicitNullableTypeChecker
                .checkValueForType(new ArrayList<String>(), stringListType));
    }

    @Test
    public void should_ReturnError_When_GivenNullValue_ForCollectionType() {
        String error = explicitNullableTypeChecker.checkValueForType(null,
                stringListType);

        Assert.assertNotNull(error);
        Assert.assertTrue(error.contains("null"));
        Assert.assertTrue(error.contains("List"));
        Assert.assertTrue(error.contains("String"));
    }

    @Test
    public void should_Recursively_Check_Collection_Items() {
        ExplicitNullableTypeChecker checker = spy(explicitNullableTypeChecker);

        List<String> list = parametrizedListMethod("foo", "bar");

        checker.checkValueForType(list, stringListType);
        // The first interaction is the obvious
        verify(checker).checkValueForType(list, stringListType);

        verify(checker).checkValueForType("foo", String.class);
        verify(checker).checkValueForType("bar", String.class);
    }

    @Test
    public void should_ReturnNull_When_GivenNonNullItems_InCollectionType() {
        Assert.assertNull(explicitNullableTypeChecker
                .checkValueForType(Arrays.asList("", ""), stringListType));
    }

    @Test
    public void should_ReturnError_When_GivenNullItem_InCollectionType() {
        String error = explicitNullableTypeChecker
                .checkValueForType(Arrays.asList("", null, ""), stringListType);

        Assert.assertNotNull(error);
        Assert.assertTrue(error.contains("null"));
        Assert.assertTrue(error.contains("List"));
        Assert.assertTrue(error.contains("String"));
    }

    @Test
    public void should_Recursively_Check_GenericArray_Items() {
        ExplicitNullableTypeChecker checker = spy(explicitNullableTypeChecker);

        String[] array = arrayMethod("foo", "bar");

        checker.checkValueForType(array, stringArrayType);
        // The first interaction is the obvious
        verify(checker).checkValueForType(array, stringArrayType);

        verify(checker).checkValueForType("foo", String.class);
        verify(checker).checkValueForType("bar", String.class);
    }

    @Test
    @Ignore("implementation pending")
    public void should_ReturnNull_When_GivenNonNull_BeanProperties() {
        Assert.assertNull(explicitNullableTypeChecker
                .checkValueForType(new Bean("foo"), Bean.class));
    }

    @Test
    @Ignore("implementation pending")
    public void should_ReturnError_When_GivenNull_BeanProperty() {
        String error = explicitNullableTypeChecker
                .checkValueForType(new Bean(null), Bean.class);

        Assert.assertNotNull(error);
        Assert.assertTrue(error.contains("null"));
        Assert.assertTrue(error.contains("Bean"));
    }

    public List<String> parametrizedListMethod(String... args) {
        final List<String> list = new ArrayList<String>();
        for (String arg : args) {
            list.add(arg);
        }
        return list;
    }

    public String[] arrayMethod(String... args) {
        return args;
    }

    private class Bean {
        private String title;

        public Bean(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }
    }
}