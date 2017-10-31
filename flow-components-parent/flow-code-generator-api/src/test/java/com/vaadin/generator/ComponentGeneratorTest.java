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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.generator.metadata.ComponentBasicType;
import com.vaadin.generator.metadata.ComponentEventData;
import com.vaadin.generator.metadata.ComponentFunctionData;
import com.vaadin.generator.metadata.ComponentFunctionParameterData;
import com.vaadin.generator.metadata.ComponentMetadata;
import com.vaadin.generator.metadata.ComponentObjectType;
import com.vaadin.generator.metadata.ComponentObjectType.ComponentObjectTypeInnerType;
import com.vaadin.generator.metadata.ComponentPropertyBaseData;
import com.vaadin.generator.metadata.ComponentPropertyData;
import com.vaadin.ui.common.ComponentSupplier;
import com.vaadin.ui.common.HasClickListeners;
import com.vaadin.ui.common.HasComponents;
import com.vaadin.ui.common.HasStyle;
import com.vaadin.ui.common.HasText;
import com.vaadin.ui.common.HasValue;
import com.vaadin.ui.event.ComponentEvent;
import com.vaadin.ui.event.ComponentEventListener;
import com.vaadin.ui.event.DomEvent;
import com.vaadin.ui.event.EventData;

/**
 * Unit tests for the component generator
 */
public class ComponentGeneratorTest {
    private ComponentMetadata componentMetadata;
    private ComponentGenerator generator;

    @Before
    public void init() {
        generator = new ComponentGenerator();

        componentMetadata = new ComponentMetadata();
        componentMetadata.setTag("my-component");
        componentMetadata.setName("MyComponent");
        componentMetadata.setBaseUrl("my-component/my-component.html");
        componentMetadata.setVersion("0.0.1");
        componentMetadata
                .setDescription("Test java doc creation for class file");
    }

    @Test
    public void generateClass_containsClassJavaDoc() {
        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        Assert.assertTrue("Generated class didn't contain class JavaDoc",
                generatedClass
                        .contains("* " + componentMetadata.getDescription()));
    }

    @Test
    public void generateClass_generatedContainsVersions() {
        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        Assert.assertTrue("Generator version information missing",
                generatedClass.contains(
                        "Generator: " + generator.getClass().getName() + "#"));
        Assert.assertTrue("WebComponent version information missing",
                generatedClass.contains(
                        "\"WebComponent: " + componentMetadata.getName() + "#"
                                + componentMetadata.getVersion() + "\""));
        Assert.assertTrue("Flow version missing",
                generatedClass.contains("\"Flow#"));

    }

    @Test
    public void generateClass_generatedTagIsSameAsTag() {
        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        Pattern pattern = Pattern.compile("@Tag\\((.*?)\\)");
        Matcher matcher = pattern.matcher(generatedClass);
        if (matcher.find()) {
            Assert.assertEquals("Generated Tag had faulty content",
                    "@Tag(\"" + componentMetadata.getTag() + "\")",
                    matcher.group(0));
        } else {
            Assert.fail("@Tag annotation was not found for generated class.");
        }

    }

    @Test
    public void generateClass_generatedHtmlImportIsCorrect() {
        generator.withFrontendDirectory("test-directory");
        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        Pattern pattern = Pattern.compile("@HtmlImport\\((.*?)\\)");
        Matcher matcher = pattern.matcher(generatedClass);
        if (matcher.find()) {
            Assert.assertEquals(
                    "Generated HtmlImport did not match expectation.",
                    "@HtmlImport(\"frontend://test-directory/"
                            + componentMetadata.getBaseUrl() + "\")",
                    matcher.group(0));
        } else {
            Assert.fail(
                    "@HtmlImport annotation was not found for generated class.");
        }
    }

    @Test
    public void generateClass_classNameIsCamelCase() {
        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        Assert.assertTrue("Generated class name was faulty",
                generatedClass.contains("public class MyComponent"));
    }

    @Test
    public void generateClassWithMethod_callBackMethod_methodIsNotGenerated() {
        assertCallBackMethodIsNotGenerated("connectedCallback");
        assertCallBackMethodIsNotGenerated("disconnectedCallback");
        assertCallBackMethodIsNotGenerated("attributeChangedCallback");
    }

    @Test
    public void generateClassWithMethod_methodContainsJavaDoc() {
        ComponentFunctionData functionData = new ComponentFunctionData();
        functionData.setName("my-method");
        functionData.setDescription("This is my method documentation.");
        componentMetadata.setMethods(Collections.singletonList(functionData));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        Assert.assertTrue("Method javaDoc was not found",
                generatedClass.contains("* " + functionData.getDescription()));
    }

    @Test
    public void generateClassWithMethodWithParameters_methodContainsParamInJavaDoc() {
        ComponentFunctionData functionData = new ComponentFunctionData();
        functionData.setName("my-method");
        functionData.setDescription("This is my method documentation.");

        ComponentFunctionParameterData parameter = new ComponentFunctionParameterData();
        parameter.setName("text");
        parameter.setType(Collections.singleton(ComponentBasicType.STRING));

        functionData.setParameters(Collections.singletonList(parameter));
        componentMetadata.setMethods(Collections.singletonList(functionData));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        Assert.assertTrue("JavaDoc for method parameter text was not found",
                generatedClass.contains("* @param " + parameter.getName()));
    }

    @Test
    public void generateClassWithGetterAndNonFluentSetter_methodContainsJavaDoc() {
        ComponentPropertyData propertyData = new ComponentPropertyData();
        propertyData.setName("name");
        propertyData.setType(Collections.singleton(ComponentBasicType.STRING));
        propertyData
                .setDescription("This is the name property of the component.");
        componentMetadata
                .setProperties(Collections.singletonList(propertyData));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        Assert.assertTrue("No getter found",
                generatedClass.contains("public String getName()"));
        Assert.assertTrue("No setter found", generatedClass
                .contains("public void setName(java.lang.String name)"));

        Assert.assertTrue("Method javaDoc was not found",
                generatedClass.contains("* " + propertyData.getDescription()));

        Assert.assertTrue("JavaDoc parameter for setter was not found",
                generatedClass.contains("* @param " + propertyData.getName()));
    }

    @Test
    public void generateClassWithGetterAndSetter_methodContainsJavaDoc() {
        ComponentPropertyData propertyData = new ComponentPropertyData();
        propertyData.setName("name");
        propertyData.setType(Collections.singleton(ComponentBasicType.STRING));
        propertyData
                .setDescription("This is the name property of the component.");
        componentMetadata
                .setProperties(Collections.singletonList(propertyData));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        Assert.assertTrue("No getter found",
                generatedClass.contains("public String getName()"));
        Assert.assertTrue("No setter found", generatedClass
                .contains("public void setName(java.lang.String name)"));

        Assert.assertTrue("Method javaDoc was not found",
                generatedClass.contains("* " + propertyData.getDescription()));

        Assert.assertTrue("JavaDoc parameter for setter was not found",
                generatedClass.contains("* @param " + propertyData.getName()));
    }

    @Test
    public void generateClassWithGetter_methodContainsJavaDoc_noSetter() {
        ComponentPropertyData propertyData = new ComponentPropertyData();
        propertyData.setName("name");
        propertyData.setType(Collections.singleton(ComponentBasicType.STRING));
        propertyData
                .setDescription("This is the name property of the component.");
        propertyData.setReadOnly(true);
        componentMetadata
                .setProperties(Collections.singletonList(propertyData));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        Assert.assertTrue("No getter found",
                generatedClass.contains("public String getName()"));
        Assert.assertFalse("Found setter even if it shouldn't exist",
                generatedClass.contains("setName"));

        Assert.assertTrue("Method javaDoc was not found",
                generatedClass.contains("* " + propertyData.getDescription()));
    }

    @Test
    public void generateClassWithGetter_methodContainsJavaDocWithAtCodeWrap() {
        ComponentPropertyData propertyData = new ComponentPropertyData();
        propertyData.setName("name");
        propertyData.setType(Collections.singleton(ComponentBasicType.STRING));
        propertyData.setDescription(
                "This is the `<input value=\"name\">` property of the component.");
        propertyData.setReadOnly(true);
        componentMetadata
                .setProperties(Collections.singletonList(propertyData));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        Assert.assertTrue("No getter found",
                generatedClass.contains("public String getName()"));

        Assert.assertTrue("Method javaDoc was not found",
                generatedClass.contains(
                        "* This is the {@code <input value=\"name\">} property of the component."));
    }

    @Test
    public void generateClassWithGetterJavaDocBlock_methodContainsJavaDocWithAtCodeWrap() {
        ComponentPropertyData propertyData = new ComponentPropertyData();
        propertyData.setName("name");
        propertyData.setType(Collections.singleton(ComponentBasicType.STRING));
        propertyData.setDescription(
                "This is the ```<input value=\"name\">``` property of the component.");
        propertyData.setReadOnly(true);
        componentMetadata
                .setProperties(Collections.singletonList(propertyData));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        Assert.assertTrue("No getter found",
                generatedClass.contains("public String getName()"));

        Assert.assertTrue("Method javaDoc was not found",
                generatedClass.contains(
                        "* This is the {@code <input value=\"name\">} property of the component."));
    }

    @Test
    public void generateClassWithLicenseNote_classContainsLicenseHeader() {
        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", "some license header");

        Assert.assertTrue("No license header found", generatedClass.startsWith(
                "/*\n * some license header\n */\npackage com.my.test"));
    }

    @Test
    public void generateClassWithEvent_classTypedComponentEvent() {
        ComponentEventData eventData = new ComponentEventData();
        eventData.setName("change");
        eventData.setDescription("Component change event.");
        componentMetadata.setEvents(Collections.singletonList(eventData));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        generatedClass = ComponentGeneratorTestUtils
                .removeIndentation(generatedClass);

        Assert.assertTrue("Custom event class was not found.",
                generatedClass.contains(
                        "public static class ChangeEvent<R extends MyComponent<R>> extends ComponentEvent<R> {"));

        Assert.assertTrue("No DomEvent annotation found",
                generatedClass.contains("@DomEvent(\"change\")"));

        // Using matcher as the formatter may cut the method.
        Pattern pattern = Pattern.compile(
                "addChangeListener\\((\\w?)(\\s*?)ComponentEventListener<ChangeEvent<R>> listener\\)");
        Matcher matcher = pattern.matcher(generatedClass);
        Assert.assertTrue("Couldn't find correct listener for event.",
                matcher.find());

        Assert.assertTrue("Missing DomEvent import", generatedClass
                .contains("import " + DomEvent.class.getName() + ";"));
        Assert.assertTrue("Missing ComponentEvent import", generatedClass
                .contains("import " + ComponentEvent.class.getName() + ";"));
        Assert.assertTrue("Missing ComponentEventListener import",
                generatedClass.contains("import "
                        + ComponentEventListener.class.getName() + ";"));
        Assert.assertFalse("EventData imported even without events",
                generatedClass
                        .contains("import " + EventData.class.getName() + ";"));
    }

    @Test
    public void generateClassWithEventWithEventData_classTypedComponentEventWithEventData() {
        ComponentEventData eventData = new ComponentEventData();
        eventData.setName("change");
        eventData.setDescription("Component change event.");
        componentMetadata.setEvents(Collections.singletonList(eventData));

        ComponentPropertyBaseData property = new ComponentPropertyBaseData();
        property.setName("button");
        property.setType(Collections.singleton(ComponentBasicType.NUMBER));

        eventData.setProperties(Collections.singletonList(property));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        // Using matcher as the formatter may cut the method.
        Pattern pattern = Pattern.compile(
                "public ChangeEvent\\(R source, boolean fromClient,(\\w?)(\\s*?)@EventData\\(\"event\\.button\"\\) double button\\)");
        Matcher matcher = pattern.matcher(generatedClass);
        Assert.assertTrue("Couldn't find constructor with EventData.",
                matcher.find());

        Assert.assertTrue("Couldn't find variable reference",
                generatedClass.contains("private final double button;"));

        Assert.assertTrue("Couldn't find getter for event data",
                generatedClass.contains("public double getButton() {"));

        Assert.assertFalse("Found setter even though one shouldn't exist",
                generatedClass.contains("public void setButton("));

        Assert.assertTrue("Missing EventData import", generatedClass
                .contains("import " + EventData.class.getName() + ";"));
    }

    @Test
    public void generateClassWithEventWithEventDataContainingDotNotation_classTypedComponentEventWithEventData() {
        ComponentEventData eventData = new ComponentEventData();
        eventData.setName("change");
        eventData.setDescription("Component change event.");
        componentMetadata.setEvents(Collections.singletonList(eventData));

        ComponentPropertyBaseData property = new ComponentPropertyBaseData();
        property.setName("details.property");
        property.setType(Collections.singleton(ComponentBasicType.NUMBER));

        eventData.setProperties(Collections.singletonList(property));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        // Using matcher as the formatter may cut the method.
        Pattern pattern = Pattern.compile(
                "public ChangeEvent\\(R source, boolean fromClient,(\\w?)(\\s*?)@EventData\\(\"event\\.details\\.property\"\\) double detailsProperty\\)");
        Matcher matcher = pattern.matcher(generatedClass);
        Assert.assertTrue("Couldn't find constructor with EventData.",
                matcher.find());

        Assert.assertTrue("Couldn't find variable reference", generatedClass
                .contains("private final double detailsProperty;"));

        Assert.assertTrue("Couldn't find getter for event data", generatedClass
                .contains("public double getDetailsProperty() {"));

        Assert.assertFalse("Found setter even though one shouldn't exist",
                generatedClass.contains("public void setDetailsProperty("));

        Assert.assertTrue("Missing EventData import", generatedClass
                .contains("import " + EventData.class.getName() + ";"));
    }

    @Test
    public void generateClassWithStringGetterAndNonFluentSetter_setterSetsEmptyForNullValue() {
        ComponentPropertyData propertyData = new ComponentPropertyData();
        propertyData.setName("name");
        propertyData.setType(Collections.singleton(ComponentBasicType.STRING));
        propertyData
                .setDescription("This is the name property of the component.");
        componentMetadata
                .setProperties(Collections.singletonList(propertyData));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        Assert.assertTrue("No setter found", generatedClass
                .contains("public void setName(java.lang.String name)"));

        Assert.assertTrue("Setter doesn't check for null value",
                generatedClass.contains(propertyData.getName()
                        + " == null ? \"\" : " + propertyData.getName()));
    }

    @Test
    public void generateClassWithBooleanGetterAndNonFluentSetter_setterDoesNotSetEmptyForNullValue() {
        ComponentPropertyData propertyData = new ComponentPropertyData();
        propertyData.setName("required");
        propertyData.setType(Collections.singleton(ComponentBasicType.BOOLEAN));
        propertyData.setDescription("This is a required field.");
        componentMetadata
                .setProperties(Collections.singletonList(propertyData));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        Assert.assertTrue("No setter found", generatedClass
                .contains("public void setRequired(boolean required)"));

        Assert.assertFalse("Setter checks for null value",
                generatedClass.contains(propertyData.getName()
                        + " == null ? \"\" : " + propertyData.getName()));
    }

    @Test
    public void generateClassWithBaseUrl_classContainsBaseUrlInThePackage() {
        componentMetadata.setBaseUrl("some/directory/some-component.html");
        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        Assert.assertTrue(
                "Wrong generated package. It should be com.my.test.some.directory",
                generatedClass
                        .startsWith("package com.my.test.some.directory;"));

        componentMetadata
                .setBaseUrl("\\Some\\Other\\Directory\\some-component.html");
        generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        Assert.assertTrue(
                "Wrong generated package. It should be com.my.test.some.otherdirectory",
                generatedClass.startsWith(
                        "package com.my.test.some.otherdirectory;"));
    }

    @Test
    public void generateClass_implementsHasStyle() {
        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        ComponentGeneratorTestUtils.assertClassImplementsInterface(
                generatedClass, "MyComponent", HasStyle.class);
    }

    @Test
    public void generateClass_implementsComponentSupplier() {
        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        ComponentGeneratorTestUtils.assertClassImplementsInterface(
                generatedClass, "MyComponent", ComponentSupplier.class);
    }

    @Test
    public void generateClassWithClickableBehavior_classImplementsHasClickListeners() {
        componentMetadata.setBehaviors(
                Collections.singletonList("Polymer.GestureEventListeners"));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        ComponentGeneratorTestUtils.assertClassImplementsInterface(
                generatedClass, "MyComponent", HasClickListeners.class);
    }

    @Test
    public void generateButtonClass_classImplementsHasText() {
        componentMetadata.setTag("vaadin-button");
        componentMetadata.setName("VaadinButton");

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        ComponentGeneratorTestUtils.assertClassImplementsInterface(
                generatedClass, "VaadinButton", HasText.class);
    }

    @Test
    public void classContainsGetterAndRelatedChangeEvent_getterContainsSynchronizeAnnotation() {
        ComponentPropertyData property = new ComponentPropertyData();
        property.setName("someproperty");
        property.setType(Collections.singleton(ComponentBasicType.STRING));
        componentMetadata.setProperties(Collections.singletonList(property));

        ComponentEventData event = new ComponentEventData();
        event.setName("someproperty-changed");
        componentMetadata.setEvents(Collections.singletonList(event));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        generatedClass = ComponentGeneratorTestUtils
                .removeIndentation(generatedClass);

        Assert.assertTrue(
                "Wrong getter definition. It should contains @Synchronize(property = \"somepropery\", value = \"someproperty-changed\")",
                generatedClass.contains(
                        "@Synchronize(property = \"someproperty\", value = \"someproperty-changed\") "
                                + "public String getSomeproperty() {"));
    }

    @Test
    public void classContainsDefaultSlot_generatedClassImplementsHasComponents() {
        componentMetadata.setSlots(Collections.singletonList(""));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        ComponentGeneratorTestUtils.assertClassImplementsInterface(
                generatedClass, "MyComponent", HasComponents.class);
        Assert.assertFalse(
                "The generated class shouldn't contain the \"remove\" method",
                generatedClass.contains("public void remove("));
        Assert.assertFalse(
                "The generated class shouldn't contain the \"removeAll\" method",
                generatedClass.contains("public void removeAll("));
    }

    @Test
    public void classContainsOnlyNamedSlots_generatedClassContainsAdders() {
        componentMetadata
                .setSlots(Arrays.asList("named1", "named-2", "named-three"));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        Assert.assertFalse(
                "The generated class shouldn't implement HasComponents",
                generatedClass.contains("HasComponents"));

        Assert.assertTrue(
                "The generated class should contain the \"addToNamed1\" method",
                generatedClass.contains("public R addToNamed1("));
        Assert.assertTrue(
                "The generated class should contain the \"addToNamed2\" method",
                generatedClass.contains("public R addToNamed2("));
        Assert.assertTrue(
                "The generated class should contain the \"addToNamedThree\" method",
                generatedClass.contains("public R addToNamedThree("));
        Assert.assertTrue(
                "The generated class should contain the \"remove\" method",
                generatedClass.contains("public void remove("));
        Assert.assertTrue(
                "The generated class should contain the \"removeAll\" method",
                generatedClass.contains("public void removeAll("));
    }

    @Test
    public void classContainsDefaultSlotAndNamedSlots_generatedClassImplementsHasComponentsAndContainsAdders() {
        componentMetadata.setSlots(
                Arrays.asList("", "named1", "named-2", "named-three"));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        ComponentGeneratorTestUtils.assertClassImplementsInterface(
                generatedClass, "MyComponent", HasComponents.class);

        Assert.assertTrue(
                "The generated class should contain the \"addToNamed1\" method",
                generatedClass.contains("public R addToNamed1("));
        Assert.assertTrue(
                "The generated class should contain the \"addToNamed2\" method",
                generatedClass.contains("public R addToNamed2("));
        Assert.assertTrue(
                "The generated class should contain the \"addToNamedThree\" method",
                generatedClass.contains("public R addToNamedThree("));
        Assert.assertTrue(
                "The generated class should contain the \"remove\" method",
                generatedClass.contains("public void remove("));
        Assert.assertTrue(
                "The generated class should contain the \"removeAll\" method",
                generatedClass.contains("public void removeAll("));
    }

    @Test
    public void classContainsObjectProperty_generatedClassContainsInnerClass() {
        // note: the tests for the nested class are covered by the
        // NestedClassGeneratorTest
        ComponentObjectTypeInnerType stringObjectType = new ComponentObjectTypeInnerType();
        stringObjectType.setName("internalString");
        stringObjectType
                .setType(Collections.singletonList(ComponentBasicType.STRING));

        ComponentObjectType objectType = new ComponentObjectType();
        objectType.setInnerTypes(Collections.singletonList(stringObjectType));

        ComponentPropertyData property = new ComponentPropertyData();
        property.setName("something");
        property.setObjectType(Collections.singletonList(objectType));

        componentMetadata.setProperties(Collections.singletonList(property));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        generatedClass = ComponentGeneratorTestUtils
                .removeIndentation(generatedClass);

        Assert.assertTrue(
                "Generated class should contain the SomethingProperty nested class",
                generatedClass.contains(
                        "public static class SomethingProperty implements JsonSerializable"));

        Assert.assertTrue(
                "Generated class should contain the getSomething method",
                generatedClass
                        .contains("public SomethingProperty getSomething()"));

        Assert.assertTrue(
                "Generated class should contain the setSomething method",
                generatedClass.contains(
                        "public void setSomething(SomethingProperty property)"));
    }

    @Test
    public void classContainsMethodWithObjectParameter_generatedClassContainsInnerClass() {
        // note: the tests for the nested class are covered by the
        // NestedClassGeneratorTest
        ComponentObjectTypeInnerType stringObjectType = new ComponentObjectTypeInnerType();
        stringObjectType.setName("internalString");
        stringObjectType
                .setType(Collections.singletonList(ComponentBasicType.STRING));

        ComponentObjectType objectType = new ComponentObjectType();
        objectType.setInnerTypes(Collections.singletonList(stringObjectType));

        ComponentFunctionParameterData parameter = new ComponentFunctionParameterData();
        parameter.setName("somethingParam");
        parameter.setObjectType(Collections.singletonList(objectType));

        ComponentFunctionData function = new ComponentFunctionData();
        function.setName("callSomething");
        function.setParameters(Collections.singletonList(parameter));

        componentMetadata.setMethods(Collections.singletonList(function));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        generatedClass = ComponentGeneratorTestUtils
                .removeIndentation(generatedClass);

        Assert.assertTrue(
                "Generated class should contain the CallSomethingSomethingParam nested class",
                generatedClass.contains(
                        "public static class CallSomethingSomethingParam implements JsonSerializable"));

        Assert.assertTrue(
                "Generated class should contain the callSomething method",
                generatedClass.contains(
                        "public void callSomething(CallSomethingSomethingParam somethingParam)"));
    }

    @Test
    public void classContainsEventWithObjectParameter_generatedClassContainsInnerClass() {
        // note: the tests for the nested class are covered by the
        // NestedClassGeneratorTest
        ComponentObjectTypeInnerType stringObjectType = new ComponentObjectTypeInnerType();
        stringObjectType.setName("internalString");
        stringObjectType
                .setType(Collections.singletonList(ComponentBasicType.STRING));

        ComponentObjectType objectType = new ComponentObjectType();
        objectType.setInnerTypes(Collections.singletonList(stringObjectType));

        ComponentPropertyBaseData eventData = new ComponentPropertyBaseData();
        eventData.setName("details");
        eventData.setObjectType(Collections.singletonList(objectType));

        ComponentEventData event = new ComponentEventData();
        event.setName("something-changed");
        event.setProperties(Collections.singletonList(eventData));

        componentMetadata.setEvents(Collections.singletonList(event));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        generatedClass = ComponentGeneratorTestUtils
                .removeIndentation(generatedClass);

        Assert.assertTrue(
                "Generated class should contain the SomethingChangeDetails nested class",
                generatedClass.contains(
                        "public static class SomethingChangeDetails implements JsonSerializable"));

        Assert.assertTrue(
                "Generated class should contain the addSomethingChangeListener method",
                generatedClass.contains(
                        "public Registration addSomethingChangeListener( ComponentEventListener<SomethingChangeEvent<R>> listener)"));

        int indexOfEventDeclaration = generatedClass.indexOf(
                "public static class SomethingChangeEvent<R extends MyComponent<R>> extends ComponentEvent<R> {");
        int endIndexOfEventDeclaration = generatedClass.indexOf("} }",
                indexOfEventDeclaration);
        String eventDeclaration = generatedClass.substring(
                indexOfEventDeclaration, endIndexOfEventDeclaration + 3);

        Assert.assertTrue(
                "Generated event should contain the getDetails method",
                eventDeclaration.contains(
                        "public SomethingChangeDetails getDetails() { return new SomethingChangeDetails().readJson(details); } }"));

    }

    @Test
    public void classContainsOverloadedMethodsForMethodsThatAcceptMultipleTypes() {
        ComponentFunctionParameterData firstParameter = new ComponentFunctionParameterData();
        firstParameter.setName("firstParam");
        firstParameter.setType(new LinkedHashSet<>(Arrays.asList(
                ComponentBasicType.STRING, ComponentBasicType.BOOLEAN)));
        ComponentFunctionParameterData secondParameter = new ComponentFunctionParameterData();
        secondParameter.setName("secondParam");
        secondParameter.setType(new LinkedHashSet<>(Arrays.asList(
                ComponentBasicType.STRING, ComponentBasicType.BOOLEAN)));

        ComponentFunctionData function = new ComponentFunctionData();
        function.setName("callSomething");
        function.setParameters(Arrays.asList(firstParameter, secondParameter));

        componentMetadata.setMethods(Collections.singletonList(function));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        generatedClass = ComponentGeneratorTestUtils
                .removeIndentation(generatedClass);

        Assert.assertTrue(generatedClass.contains(
                "public void callSomething(java.lang.String firstParam, java.lang.String secondParam)"));
        Assert.assertTrue(generatedClass.contains(
                "public void callSomething(java.lang.String firstParam, boolean secondParam)"));
        Assert.assertTrue(generatedClass.contains(
                "public void callSomething(boolean firstParam, java.lang.String secondParam)"));
        Assert.assertTrue(generatedClass.contains(
                "public void callSomething(boolean firstParam, boolean secondParam)"));
    }

    @Test
    public void classContainsOverloadedMethodsForMethodsThatAcceptMultipleTypes_withObjectTypes() {
        ComponentObjectTypeInnerType stringObjectTypeInnerType = new ComponentObjectTypeInnerType();
        stringObjectTypeInnerType.setName("internalString");
        stringObjectTypeInnerType
                .setType(Collections.singletonList(ComponentBasicType.STRING));

        ComponentObjectType stringObjectType = new ComponentObjectType();
        stringObjectType.setInnerTypes(
                Collections.singletonList(stringObjectTypeInnerType));

        ComponentFunctionParameterData firstParameter = new ComponentFunctionParameterData();
        firstParameter.setName("firstParam");
        firstParameter
                .setObjectType(Collections.singletonList(stringObjectType));

        ComponentFunctionParameterData secondParameter = new ComponentFunctionParameterData();
        secondParameter.setName("secondParam");
        secondParameter.setType(new LinkedHashSet<>(Arrays.asList(
                ComponentBasicType.STRING, ComponentBasicType.BOOLEAN)));

        ComponentFunctionData function = new ComponentFunctionData();
        function.setName("callSomething");
        function.setParameters(Arrays.asList(firstParameter, secondParameter));

        componentMetadata.setMethods(Collections.singletonList(function));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        generatedClass = ComponentGeneratorTestUtils
                .removeIndentation(generatedClass);

        Assert.assertTrue(generatedClass.contains(
                "public void callSomething(CallSomethingFirstParam firstParam, java.lang.String secondParam)"));
        Assert.assertTrue(generatedClass.contains(
                "public void callSomething(CallSomethingFirstParam firstParam, boolean secondParam)"));
    }

    @Test
    public void componentContainsValueProperty_generatedClassImplementsHasValue() {
        ComponentPropertyData property = new ComponentPropertyData();
        property.setName("value");
        property.setType(Collections.singleton(ComponentBasicType.STRING));
        componentMetadata.setProperties(Collections.singletonList(property));

        ComponentEventData event = new ComponentEventData();
        event.setName("value-changed");
        componentMetadata.setEvents(Collections.singletonList(event));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        generatedClass = ComponentGeneratorTestUtils
                .removeIndentation(generatedClass);

        ComponentGeneratorTestUtils.assertClassImplementsInterface(
                generatedClass, "MyComponent", HasValue.class);
        Assert.assertTrue(
                generatedClass.contains("@Override public String getValue()"));
        Assert.assertTrue(generatedClass.contains(
                "@Override public void setValue(java.lang.String value)"));
    }

    @Test
    public void componentContainsValueProperty_generatedSetValuePreventsSettingTheSameValue() {
        ComponentPropertyData property = new ComponentPropertyData();
        property.setName("value");
        property.setType(Collections.singleton(ComponentBasicType.STRING));
        componentMetadata.setProperties(Collections.singletonList(property));

        ComponentEventData event = new ComponentEventData();
        event.setName("value-changed");
        componentMetadata.setEvents(Collections.singletonList(event));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        generatedClass = ComponentGeneratorTestUtils
                .removeIndentation(generatedClass);

        Assert.assertTrue(generatedClass.contains(
                "@Override public void setValue(java.lang.String value) { if (!Objects.equals(value, getValue())) {"));
    }

    @Test
    public void componentContainsNumberValueProperty_generatedClassImplementsHasValueWithoutPrimitiveTypes() {
        ComponentPropertyData property = new ComponentPropertyData();
        property.setName("value");
        property.setType(Collections.singleton(ComponentBasicType.NUMBER));
        componentMetadata.setProperties(Collections.singletonList(property));

        ComponentEventData event = new ComponentEventData();
        event.setName("value-changed");
        componentMetadata.setEvents(Collections.singletonList(event));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        generatedClass = ComponentGeneratorTestUtils
                .removeIndentation(generatedClass);

        ComponentGeneratorTestUtils.assertClassImplementsInterface(
                generatedClass, "MyComponent", HasValue.class);
        Assert.assertTrue(
                generatedClass.contains("@Override public Double getValue()"));
        Assert.assertTrue(generatedClass.contains(
                "@Override public void setValue(java.lang.Double value)"));
        Assert.assertTrue(generatedClass
                .contains("public void setValue(java.lang.Number value)"));
    }

    @Test
    public void componentContainsNumberValueProperty_generatedSetValuesPreventSettingTheSameValue() {
        ComponentPropertyData property = new ComponentPropertyData();
        property.setName("value");
        property.setType(Collections.singleton(ComponentBasicType.NUMBER));
        componentMetadata.setProperties(Collections.singletonList(property));

        ComponentEventData event = new ComponentEventData();
        event.setName("value-changed");
        componentMetadata.setEvents(Collections.singletonList(event));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        generatedClass = ComponentGeneratorTestUtils
                .removeIndentation(generatedClass);

        Assert.assertTrue(generatedClass.contains(
                "@Override public void setValue(java.lang.Double value) { Objects.requireNonNull(value, \"MyComponent value must not be null\"); if (!Objects.equals(value, getValue())) {"));
        Assert.assertTrue(generatedClass.contains(
                "public void setValue(java.lang.Number value) { Objects.requireNonNull(value, \"MyComponent value must not be null\"); if (!Objects.equals(value, getValue())) {"));
    }

    @Test
    public void componentContainsUnrecognizedPropertyTypes_methodsAreGeneratedAsProtected() {
        ComponentPropertyData objectProperty = new ComponentPropertyData();
        objectProperty.setName("objectProperty");
        objectProperty
                .setType(Collections.singleton(ComponentBasicType.OBJECT));

        ComponentPropertyData arrayProperty = new ComponentPropertyData();
        arrayProperty.setName("arrayProperty");
        arrayProperty.setType(Collections.singleton(ComponentBasicType.ARRAY));

        ComponentPropertyData undefinedProperty = new ComponentPropertyData();
        undefinedProperty.setName("undefinedProperty");
        undefinedProperty
                .setType(Collections.singleton(ComponentBasicType.UNDEFINED));
        componentMetadata.setProperties(Arrays.asList(objectProperty,
                arrayProperty, undefinedProperty));

        ComponentFunctionParameterData objectParameter = new ComponentFunctionParameterData();
        objectParameter.setName("objectParam");
        objectParameter
                .setType(Collections.singleton(ComponentBasicType.OBJECT));

        ComponentFunctionData function = new ComponentFunctionData();
        function.setName("callSomething");
        function.setParameters(Collections.singletonList(objectParameter));

        componentMetadata.setMethods(Collections.singletonList(function));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        generatedClass = ComponentGeneratorTestUtils
                .removeIndentation(generatedClass);

        Assert.assertTrue(generatedClass
                .contains("protected JsonObject protectedGetObjectProperty()"));
        Assert.assertTrue(generatedClass.contains(
                "protected void setObjectProperty(elemental.json.JsonObject objectProperty)"));
        Assert.assertTrue(generatedClass
                .contains("protected JsonArray protectedGetArrayProperty()"));
        Assert.assertTrue(generatedClass.contains(
                "protected void setArrayProperty(elemental.json.JsonArray arrayProperty)"));
        Assert.assertTrue(generatedClass.contains(
                "protected JsonValue protectedGetUndefinedProperty()"));
        Assert.assertTrue(generatedClass.contains(
                "protected void setUndefinedProperty( elemental.json.JsonValue undefinedProperty)"));

        Assert.assertTrue(generatedClass.contains(
                "protected void callSomething(elemental.json.JsonObject objectParam)"));
    }

    @Test
    public void componentContainsFunctionsWithUnregognizedParameterTypes_methodsAreGeneratedAsProtected() {
        ComponentFunctionParameterData objectParameter = new ComponentFunctionParameterData();
        objectParameter.setName("objectParam");
        objectParameter
                .setType(Collections.singleton(ComponentBasicType.OBJECT));

        ComponentFunctionData function1 = new ComponentFunctionData();
        function1.setName("callSomethingWithObject");
        function1.setParameters(Collections.singletonList(objectParameter));

        ComponentFunctionParameterData stringParameter = new ComponentFunctionParameterData();
        stringParameter.setName("stringParam");
        stringParameter
                .setType(Collections.singleton(ComponentBasicType.STRING));

        ComponentFunctionData function2 = new ComponentFunctionData();
        function2.setName("callSomethingWithObjectAndString");
        function2
                .setParameters(Arrays.asList(objectParameter, stringParameter));

        ComponentFunctionParameterData multiParameter = new ComponentFunctionParameterData();
        multiParameter.setName("multiParam");
        multiParameter.setType(new LinkedHashSet<>(Arrays.asList(
                ComponentBasicType.STRING, ComponentBasicType.OBJECT,
                ComponentBasicType.ARRAY, ComponentBasicType.UNDEFINED)));

        ComponentFunctionData function3 = new ComponentFunctionData();
        function3.setName("callSomethingWithMultiTypes");
        function3.setParameters(Collections.singletonList(multiParameter));

        componentMetadata
                .setMethods(Arrays.asList(function1, function2, function3));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        generatedClass = ComponentGeneratorTestUtils
                .removeIndentation(generatedClass);

        Assert.assertTrue(generatedClass.contains(
                "protected void callSomethingWithObject(JsonObject objectParam)"));
        Assert.assertTrue(generatedClass.contains(
                "protected void callSomethingWithObjectAndString( elemental.json.JsonObject objectParam, java.lang.String stringParam)"));
        Assert.assertTrue(generatedClass.contains(
                "public void callSomethingWithMultiTypes(java.lang.String multiParam)"));
        Assert.assertTrue(generatedClass.contains(
                "protected void callSomethingWithMultiTypes( elemental.json.JsonObject multiParam)"));
        Assert.assertTrue(generatedClass.contains(
                "protected void callSomethingWithMultiTypes(JsonArray multiParam)"));
        Assert.assertTrue(generatedClass.contains(
                "protected void callSomethingWithMultiTypes(JsonValue multiParam)"));
    }

    private void assertCallBackMethodIsNotGenerated(String callback) {
        ComponentFunctionData functionData = new ComponentFunctionData();
        functionData.setName(callback);
        componentMetadata.setMethods(Collections.singletonList(functionData));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        Assert.assertFalse("Callback methods are generated",
                generatedClass.contains(callback));
    }
}
