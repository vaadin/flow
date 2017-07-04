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
import com.vaadin.generator.metadata.ComponentPropertyBaseData;
import com.vaadin.generator.metadata.ComponentPropertyData;
import com.vaadin.ui.HasClickListeners;
import com.vaadin.ui.HasComponents;
import com.vaadin.ui.HasStyle;
import com.vaadin.ui.HasText;

/**
 * Unit tests for the component generator
 */
public class ComponentGeneratorTest {

    ComponentMetadata componentMetadata;
    ComponentGenerator generator;

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
    public void generateClassWithMethod_methodContainsJavaDoc() {
        ComponentFunctionData functionData = new ComponentFunctionData();
        functionData.setName("my-method");
        functionData.setDescription("This is my method documentation.");
        componentMetadata.setMethods(Arrays.asList(functionData));

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
        parameter.setType(Arrays.asList(ComponentBasicType.STRING));

        functionData.setParameters(Arrays.asList(parameter));
        componentMetadata.setMethods(Arrays.asList(functionData));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        Assert.assertTrue("JavaDoc for method parameter text was not found",
                generatedClass.contains("* @param " + parameter.getName()));
    }

    @Test
    public void generateClassWithGetterAndNonFluentSetter_methodContainsJavaDoc() {
        generator.withFluentSetters(false);

        ComponentPropertyData propertyData = new ComponentPropertyData();
        propertyData.setName("name");
        propertyData.setType(Arrays.asList(ComponentBasicType.STRING));
        propertyData
                .setDescription("This is the name property of the component.");
        componentMetadata.setProperties(Arrays.asList(propertyData));

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
    public void generateClassWithGetterAndFluentSetter_methodContainsJavaDoc() {
        ComponentPropertyData propertyData = new ComponentPropertyData();
        propertyData.setName("name");
        propertyData.setType(Arrays.asList(ComponentBasicType.STRING));
        propertyData
                .setDescription("This is the name property of the component.");
        componentMetadata.setProperties(Arrays.asList(propertyData));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        Assert.assertTrue("No getter found",
                generatedClass.contains("public String getName()"));
        Assert.assertTrue("No fluent setter found", generatedClass
                .contains("public R setName(java.lang.String name)"));

        Assert.assertTrue("Method javaDoc was not found",
                generatedClass.contains("* " + propertyData.getDescription()));

        Assert.assertTrue("JavaDoc parameter for fluent setter was not found",
                generatedClass.contains("* @param " + propertyData.getName()));

        Assert.assertTrue("JavaDoc return for fluent setter was not found",
                generatedClass.contains(
                        "* @return This instance, for method chaining."));
    }

    @Test
    public void generateClassWithGetter_methodContainsJavaDoc_noSetter() {
        ComponentPropertyData propertyData = new ComponentPropertyData();
        propertyData.setName("name");
        propertyData.setType(Arrays.asList(ComponentBasicType.STRING));
        propertyData
                .setDescription("This is the name property of the component.");
        propertyData.setReadOnly(true);
        componentMetadata.setProperties(Arrays.asList(propertyData));

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
        propertyData.setType(Arrays.asList(ComponentBasicType.STRING));
        propertyData.setDescription(
                "This is the `<input value=\"name\">` property of the component.");
        propertyData.setReadOnly(true);
        componentMetadata.setProperties(Arrays.asList(propertyData));

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
        propertyData.setType(Arrays.asList(ComponentBasicType.STRING));
        propertyData.setDescription(
                "This is the ```<input value=\"name\">``` property of the component.");
        propertyData.setReadOnly(true);
        componentMetadata.setProperties(Arrays.asList(propertyData));

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
        componentMetadata.setEvents(Arrays.asList(eventData));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        Assert.assertTrue("Custom event class was not found.",
                generatedClass.contains(
                        "public static class ChangeEvent extends ComponentEvent<MyComponent> {"));

        Assert.assertTrue("No DomEvent annotation found",
                generatedClass.contains("@DomEvent(\"change\")"));

        // Using matcher as the formatter may cut the method.
        Pattern pattern = Pattern.compile(
                "addChangeListener\\((\\w?)(\\s*?)ComponentEventListener<ChangeEvent> listener\\)");
        Matcher matcher = pattern.matcher(generatedClass);
        Assert.assertTrue("Couldn't find correct listener for event.",
                matcher.find());

        Assert.assertTrue("Missing DomEvent import", generatedClass
                .contains("import com.vaadin.annotations.DomEvent;"));
        Assert.assertTrue("Missing ComponentEvent import", generatedClass
                .contains("import com.vaadin.ui.ComponentEvent;"));
        Assert.assertTrue("Missing ComponentEventListener import",
                generatedClass.contains(
                        "import com.vaadin.flow.event.ComponentEventListener;"));
        Assert.assertFalse("EventData imported even without events",
                generatedClass
                        .contains("import com.vaadin.annotations.EventData;"));
    }

    @Test
    public void generateClassWithEventWithEventData_classTypedComponentEventWithEventData() {
        ComponentEventData eventData = new ComponentEventData();
        eventData.setName("change");
        eventData.setDescription("Component change event.");
        componentMetadata.setEvents(Arrays.asList(eventData));

        ComponentPropertyBaseData property = new ComponentPropertyBaseData();
        property.setName("button");
        property.setType(Arrays.asList(ComponentBasicType.NUMBER));

        eventData.setProperties(Arrays.asList(property));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        // Using matcher as the formatter may cut the method.
        Pattern pattern = Pattern.compile(
                "public ChangeEvent\\(MyComponent source, boolean fromClient,(\\w?)(\\s*?)@EventData\\(\"event\\.button\"\\) double button\\)");
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
                .contains("import com.vaadin.annotations.EventData;"));
    }

    @Test
    public void generateClassWithEventWithEventDataContainingDotNotation_classTypedComponentEventWithEventData() {
        ComponentEventData eventData = new ComponentEventData();
        eventData.setName("change");
        eventData.setDescription("Component change event.");
        componentMetadata.setEvents(Arrays.asList(eventData));

        ComponentPropertyBaseData property = new ComponentPropertyBaseData();
        property.setName("details.property");
        property.setType(Arrays.asList(ComponentBasicType.NUMBER));

        eventData.setProperties(Arrays.asList(property));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        // Using matcher as the formatter may cut the method.
        Pattern pattern = Pattern.compile(
                "public ChangeEvent\\(MyComponent source, boolean fromClient,(\\w?)(\\s*?)@EventData\\(\"event\\.details\\.property\"\\) double detailsProperty\\)");
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
                .contains("import com.vaadin.annotations.EventData;"));
    }

    @Test
    public void generateClassWithStringGetterAndNonFluentSetter_setterSetsEmptyForNullValue() {
        generator.withFluentSetters(false);

        ComponentPropertyData propertyData = new ComponentPropertyData();
        propertyData.setName("name");
        propertyData.setType(Arrays.asList(ComponentBasicType.STRING));
        propertyData
                .setDescription("This is the name property of the component.");
        componentMetadata.setProperties(Arrays.asList(propertyData));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        Assert.assertTrue("No setter found", generatedClass
                .contains("public void setName(java.lang.String name)"));

        Assert.assertTrue("Setter doesn't check for null value",
                generatedClass.contains(propertyData.getName()
                        + " == null ? \"\" : " + propertyData.getName()));
    }

    @Test
    public void generateClassWithStringGetterAndFluentSetter_setterSetsEmptyForNullValue() {
        ComponentPropertyData propertyData = new ComponentPropertyData();
        propertyData.setName("name");
        propertyData.setType(Arrays.asList(ComponentBasicType.STRING));
        propertyData
                .setDescription("This is the name property of the component.");
        componentMetadata.setProperties(Arrays.asList(propertyData));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        Assert.assertTrue("No fluent setter found", generatedClass
                .contains("public R setName(java.lang.String name)"));

        Assert.assertTrue("Fluent setter doesn't check for null value",
                generatedClass.contains(propertyData.getName()
                        + " == null ? \"\" : " + propertyData.getName()));
    }

    @Test
    public void generateClassWithBooleanGetterAndNonFluentSetter_setterDoesNotSetEmptyForNullValue() {
        generator.withFluentSetters(false);

        ComponentPropertyData propertyData = new ComponentPropertyData();
        propertyData.setName("required");
        propertyData.setType(Arrays.asList(ComponentBasicType.BOOLEAN));
        propertyData.setDescription("This is a required field.");
        componentMetadata.setProperties(Arrays.asList(propertyData));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        Assert.assertTrue("No setter found", generatedClass
                .contains("public void setRequired(boolean required)"));

        Assert.assertFalse("Setter checks for null value",
                generatedClass.contains(propertyData.getName()
                        + " == null ? \"\" : " + propertyData.getName()));
    }

    @Test
    public void generateClassWithBooleanGetterAndFluentSetter_setterDoesNotSetEmptyForNullValue() {
        ComponentPropertyData propertyData = new ComponentPropertyData();
        propertyData.setName("required");
        propertyData.setType(Arrays.asList(ComponentBasicType.BOOLEAN));
        propertyData.setDescription("This is a required field.");
        componentMetadata.setProperties(Arrays.asList(propertyData));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        Assert.assertTrue("No fluent setter found", generatedClass
                .contains("public R setRequired(boolean required)"));

        Assert.assertFalse("Fluent setter checks for null value",
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
                "Wrong generated package. It should be com.my.test.some.other.directory",
                generatedClass.startsWith(
                        "package com.my.test.some.other.directory;"));
    }

    @Test
    public void generateClassWithFluentSetters_classContainsGenericTypeAndGetSelf() {
        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        Assert.assertTrue(
                "Wrong class definition. It should be MyComponent<R extends MyComponent<R>>",
                generatedClass.contains(
                        "public class MyComponent<R extends MyComponent<R>>"));

        Assert.assertTrue("The method getSelf() wasn't found",
                generatedClass.contains("protected R getSelf()"));
    }

    @Test
    public void generateClass_implementsHasStyle() {
        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        assertClassImplementsInterface(generatedClass, "MyComponent",
                HasStyle.class);
    }

    @Test
    public void generateClassWithClickableBehavior_classImplementsHasClickListeners() {
        componentMetadata
                .setBehaviors(Arrays.asList("Polymer.GestureEventListeners"));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        assertClassImplementsInterface(generatedClass, "MyComponent",
                HasClickListeners.class);
    }

    @Test
    public void generateButtonClass_classImplementsHasText() {
        componentMetadata.setTag("vaadin-button");
        componentMetadata.setName("VaadinButton");

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        assertClassImplementsInterface(generatedClass, "VaadinButton",
                HasText.class);
    }

    private void assertClassImplementsInterface(String generatedClass,
            String className, Class<?> interfaceToBeImplemented) {
        Pattern pattern = Pattern.compile("\\s*public\\s+class\\s+" + className
                + ".*\\s+extends\\s+Component\\s+implements\\s+([^\\{]+)\\{");
        Matcher matcher = pattern.matcher(generatedClass);
        Assert.assertTrue("Wrong class declaration", matcher.find());

        String interfaces = matcher.group(1);
        Assert.assertTrue(interfaceToBeImplemented.getSimpleName()
                + " interface not found in the class definition: " + interfaces,
                interfaces.contains(interfaceToBeImplemented.getSimpleName()));
    }

    @Test
    public void classContainsGetterAndRelatedChangeEvent_getterContainsSynchronizeAnnotation() {
        ComponentPropertyData property = new ComponentPropertyData();
        property.setName("someproperty");
        property.setType(Arrays.asList(ComponentBasicType.STRING));
        componentMetadata.setProperties(Arrays.asList(property));

        ComponentEventData event = new ComponentEventData();
        event.setName("someproperty-changed");
        componentMetadata.setEvents(Arrays.asList(event));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        // remove indentation
        generatedClass = generatedClass.replaceAll("\\s\\s+", " ");

        Assert.assertTrue(
                "Wrong getter definition. It should contains @Synchronize(property = \"somepropery\", value = \"someproperty-changed\")",
                generatedClass.contains(
                        "@Synchronize(property = \"someproperty\", value = \"someproperty-changed\") "
                                + "public String getSomeproperty() {"));
    }

    @Test
    public void classContainsDefaultSlot_generatedClassImplementsHasComponents() {
        componentMetadata.setSlots(Arrays.asList(""));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        assertClassImplementsInterface(generatedClass, "MyComponent",
                HasComponents.class);
    }

    @Test
    public void classContainsNamedSlots_generatedClassContainsAdders() {
        componentMetadata
                .setSlots(Arrays.asList("named1", "named-2", "named-three"));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        Assert.assertFalse(
                "The generated class shouldn't implement HasComponents",
                generatedClass.contains("HasComponents"));

        Assert.assertTrue(
                "The generated class should contain the \"addToNamed1\" method",
                generatedClass.contains("public void addToNamed1("));
        Assert.assertTrue(
                "The generated class should contain the \"addToNamed2\" method",
                generatedClass.contains("public void addToNamed2("));
        Assert.assertTrue(
                "The generated class should contain the \"addToNamedThree\" method",
                generatedClass.contains("public void addToNamedThree("));
    }

    @Test
    public void classContainsDefaultSlotAndNamedSlots_generatedClassImplementsHasComponentsAndContainsAdders() {
        componentMetadata.setSlots(
                Arrays.asList("", "named1", "named-2", "named-three"));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        assertClassImplementsInterface(generatedClass, "MyComponent",
                HasComponents.class);

        Assert.assertTrue(
                "The generated class should contain the \"addToNamed1\" method",
                generatedClass.contains("public void addToNamed1("));
        Assert.assertTrue(
                "The generated class should contain the \"addToNamed2\" method",
                generatedClass.contains("public void addToNamed2("));
        Assert.assertTrue(
                "The generated class should contain the \"addToNamedThree\" method",
                generatedClass.contains("public void addToNamedThree("));
    }
}
