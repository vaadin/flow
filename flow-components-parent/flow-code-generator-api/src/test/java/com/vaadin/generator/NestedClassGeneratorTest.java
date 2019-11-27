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
package com.vaadin.generator;

import java.util.Arrays;

import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.JsonSerializable;
import com.vaadin.generator.metadata.ComponentBasicType;
import com.vaadin.generator.metadata.ComponentObjectType;
import com.vaadin.generator.metadata.ComponentObjectType.ComponentObjectTypeInnerType;

import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Unit tests for the {@link NestedClassGenerator} class.
 */
public class NestedClassGeneratorTest {

    private NestedClassGenerator generator;
    private ComponentObjectType type;

    @Before
    public void init() {
        generator = new NestedClassGenerator();
        type = new ComponentObjectType();
    }

    @Test
    public void emptyClassDefinition_onlyJsonSerializableMethods() {
        JavaClassSource javaClass = generator.withNameHint("test")
                .withType(type).build();

        assertJsonSerializableWasImplemented(javaClass);
    }

    @Test
    public void simpleObjectTypesClassDefinition_classHasGettersAndSetters() {
        type.getInnerTypes().add(createComponentObjectType("booleanProperty",
                ComponentBasicType.BOOLEAN));
        type.getInnerTypes().add(createComponentObjectType("stringProperty",
                ComponentBasicType.STRING));
        type.getInnerTypes().add(createComponentObjectType("numberProperty",
                ComponentBasicType.NUMBER));
        type.getInnerTypes().add(createComponentObjectType("objectProperty",
                ComponentBasicType.OBJECT));
        type.getInnerTypes().add(createComponentObjectType("arrayProperty",
                ComponentBasicType.ARRAY));
        type.getInnerTypes().add(createComponentObjectType("undefinedProperty",
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
        type.getInnerTypes()
                .add(createComponentObjectType("multipleSimpleProperty",
                ComponentBasicType.BOOLEAN, ComponentBasicType.STRING,
                ComponentBasicType.NUMBER));
        type.getInnerTypes()
                .add(createComponentObjectType("multipleObjectProperty",
                ComponentBasicType.STRING, ComponentBasicType.OBJECT,
                ComponentBasicType.BOOLEAN));
        type.getInnerTypes()
                .add(createComponentObjectType("multipleRepeatedProperty",
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
                "The generated class should have the \"readJson\" method.",
                javaClass.hasMethodSignature("readJson", JsonObject.class));
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

    private ComponentObjectTypeInnerType createComponentObjectType(String name,
            ComponentBasicType... basicTypes) {
        ComponentObjectTypeInnerType objectType = new ComponentObjectTypeInnerType();
        objectType.setType(Arrays.asList(basicTypes));
        objectType.setName(name);
        return objectType;
    }
}
