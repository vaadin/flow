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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.components.JsonSerializable;
import com.vaadin.generator.metadata.ComponentBasicType;
import com.vaadin.generator.metadata.ComponentObjectType;

import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Unit tests for the {@link NestedClassGenerator} class.
 */
public class NestedClassGeneratorTest {

    private NestedClassGenerator generator;
    private List<ComponentObjectType> type;

    @Before
    public void init() {
        generator = new NestedClassGenerator();
        type = new ArrayList<>();
    }

    @Test
    public void emptyClassDefinition_onlyJsonSerializableMethods() {
        JavaClassSource javaClass = generator.withNameHint("test")
                .withType(type).build();

        assertJsonSerializableWasImplemented(javaClass);
    }

    @Test
    public void simpleObjectTypesClassDefinition_classHasGettersAndSetters() {
        type.add(createComponentObjectType("booleanProperty",
                ComponentBasicType.BOOLEAN));
        type.add(createComponentObjectType("stringProperty",
                ComponentBasicType.STRING));
        type.add(createComponentObjectType("numberProperty",
                ComponentBasicType.NUMBER));
        type.add(createComponentObjectType("objectProperty",
                ComponentBasicType.OBJECT));
        type.add(createComponentObjectType("arrayProperty",
                ComponentBasicType.ARRAY));
        type.add(createComponentObjectType("undefinedProperty",
                ComponentBasicType.UNDEFINED));

        JavaClassSource javaClass = generator.withNameHint("test")
                .withType(type).build();

        assertJsonSerializableWasImplemented(javaClass);

        assertGetterAndSetterArePresent(javaClass, "isBooleanProperty",
                "setBooleanProperty", boolean.class);
        assertGetterAndSetterArePresent(javaClass, "getStringProperty",
                "setStringProperty", String.class);
        assertGetterAndSetterArePresent(javaClass, "getNumberProperty",
                "setNumberProperty", double.class);
        assertGetterAndSetterArePresent(javaClass, "getObjectProperty",
                "setObjectProperty", JsonObject.class);
        assertGetterAndSetterArePresent(javaClass, "getArrayProperty",
                "setArrayProperty", JsonArray.class);
        assertGetterAndSetterArePresent(javaClass, "getUndefinedProperty",
                "setUndefinedProperty", JsonValue.class);
    }

    @Test
    public void multipleObjectTypesClassDefinition_classHasGenericGetterAndSetter() {
        type.add(createComponentObjectType("multipleSimpleProperty",
                ComponentBasicType.BOOLEAN, ComponentBasicType.STRING,
                ComponentBasicType.NUMBER));
        type.add(createComponentObjectType("multipleObjectProperty",
                ComponentBasicType.STRING, ComponentBasicType.OBJECT,
                ComponentBasicType.BOOLEAN));
        type.add(createComponentObjectType("multipleRepeatedProperty",
                ComponentBasicType.NUMBER, ComponentBasicType.NUMBER));

        JavaClassSource javaClass = generator.withNameHint("test")
                .withType(type).build();

        assertJsonSerializableWasImplemented(javaClass);

        assertGetterAndSetterArePresent(javaClass, "getMultipleSimpleProperty",
                "setMultipleSimpleProperty", JsonValue.class);
        assertGetterAndSetterArePresent(javaClass, "getMultipleObjectProperty",
                "setMultipleObjectProperty", JsonObject.class);
        assertGetterAndSetterArePresent(javaClass,
                "getMultipleRepeatedProperty", "setMultipleRepeatedProperty",
                double.class);
    }

    private void assertJsonSerializableWasImplemented(
            JavaClassSource javaClass) {
        Assert.assertTrue(
                "The generated class should implement "
                        + JsonSerializable.class.getName(),
                javaClass.hasInterface(JsonSerializable.class));

        Assert.assertTrue(
                "The generated class should have an \"internalObject\" field.",
                javaClass.hasField("internalObject"));

        Assert.assertTrue(
                "The generated class should have the \"toJson\" method.",
                javaClass.hasMethodSignature("toJson"));

        Assert.assertTrue(
                "The generated class should have the \"fromJson\" method.",
                javaClass.hasMethodSignature("fromJson", JsonObject.class));
    }

    private void assertGetterAndSetterArePresent(JavaClassSource javaClass,
            String getterName, String setterName, Class<?> setterType) {
        Assert.assertTrue(
                "The generated class should have the getter " + getterName,
                javaClass.hasMethodSignature(getterName));
        Assert.assertTrue(
                "The generated class should have the setter " + setterName,
                javaClass.hasMethodSignature(setterName, setterType));
    }

    private ComponentObjectType createComponentObjectType(String name,
            ComponentBasicType... basicTypes) {
        ComponentObjectType objectType = new ComponentObjectType();
        objectType.setType(Arrays.asList(basicTypes));
        objectType.setName(name);
        return objectType;
    }

}
