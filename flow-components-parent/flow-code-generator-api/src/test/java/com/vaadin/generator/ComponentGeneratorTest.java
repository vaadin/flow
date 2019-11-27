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

import static org.hamcrest.CoreMatchers.containsString;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.HasText;
import com.vaadin.generator.metadata.ComponentBasicType;
import com.vaadin.generator.metadata.ComponentEventData;
import com.vaadin.generator.metadata.ComponentFunctionData;
import com.vaadin.generator.metadata.ComponentFunctionParameterData;
import com.vaadin.generator.metadata.ComponentMetadata;
import com.vaadin.generator.metadata.ComponentObjectType;
import com.vaadin.generator.metadata.ComponentObjectType.ComponentObjectTypeInnerType;
import com.vaadin.generator.metadata.ComponentPropertyBaseData;
import com.vaadin.generator.metadata.ComponentPropertyData;

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
        Assert.assertTrue("No setter found",
                generatedClass.contains("public void setName(String name)"));

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
        Assert.assertTrue("No setter found",
                generatedClass.contains("public void setName(String name)"));

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
    public void generateClassWithPropertyChangeEvent_propertyChangeListenerUsedInsteadOfDomEvent() {
        ComponentEventData eventData = new ComponentEventData();
        eventData.setName("some-property-changed");
        eventData.setDescription("Property change event.");
        componentMetadata.setEvents(Collections.singletonList(eventData));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        generatedClass = ComponentGeneratorTestUtils
                .removeIndentation(generatedClass);

        Assert.assertThat("Custom event class was not found.", generatedClass,
                containsString(
                        "public static class SomePropertyChangeEvent<R extends MyComponent<R>> extends ComponentEvent<R> {"));

        Assert.assertFalse("DomEvent should not be used for property changes",
                generatedClass.contains("@DomEvent"));

        // Using matcher as the formatter may cut the method.
        Pattern pattern = Pattern.compile(
                "addSomePropertyChangeListener\\((\\w?)(\\s*?)ComponentEventListener<SomePropertyChangeEvent<R>> listener\\)");
        Matcher matcher = pattern.matcher(generatedClass);
        Assert.assertTrue("Couldn't find correct listener for event.",
                matcher.find());

        Assert.assertThat(
                "Event should be propagated to a property change listener",
                StringUtils.deleteWhitespace(generatedClass), containsString(
                        "getElement().addPropertyChangeListener(\"someProperty\","));

        Assert.assertFalse("DomEvent imported even without dom-events",
                generatedClass
                        .contains("import " + DomEvent.class.getName() + ";"));
        Assert.assertThat("Missing ComponentEvent import", generatedClass,
                containsString(
                        "import " + ComponentEvent.class.getName() + ";"));
        Assert.assertThat("Missing ComponentEventListener import",
                generatedClass, containsString("import "
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

        Assert.assertTrue("No setter found",
                generatedClass.contains("public void setName(String name)"));

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
    public void generateClass_withoutHasStyle() {
        componentMetadata.setTag("vaadin-dialog");
        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        Assert.assertFalse(
                "Generated dialog should not have HasStyle interface",
                generatedClass.contains("implements HasStyle"));
    }

    @Test
    public void componentContainsNotifiedProperty_generatedListenerUsesComponentAsEventSource() {
        generator.withProtectedMethods(true);

        ComponentPropertyData property = new ComponentPropertyData();
        property.setName("something");
        property.setType(Collections.singleton(ComponentBasicType.STRING));
        property.setNotify(true);

        componentMetadata.setProperties(Collections.singletonList(property));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        generatedClass = ComponentGeneratorTestUtils
                .removeIndentation(generatedClass);

        Assert.assertThat(
                "Generated listener should use component as event source",
                generatedClass, containsString(
                        "ChangeEvent<R>( (R) this, event.isUserOriginated())));"));
    }

    @Test
    public void generateClassWithClickableBehavior_classImplementsHasClickListeners() {
        componentMetadata.setBehaviors(
                Collections.singletonList("Polymer.GestureEventListeners"));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        ComponentGeneratorTestUtils.assertClassImplementsInterface(
                generatedClass, "MyComponent", ClickNotifier.class);
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
        property.setName("someProperty");
        property.setType(Collections.singleton(ComponentBasicType.STRING));
        componentMetadata.setProperties(Collections.singletonList(property));

        ComponentEventData event = new ComponentEventData();
        event.setName("some-property-changed");
        componentMetadata.setEvents(Collections.singletonList(event));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        generatedClass = ComponentGeneratorTestUtils
                .removeIndentation(generatedClass);

        Assert.assertTrue(
                "Wrong getter definition. It should contains @Synchronize(property = \"somePropery\", value = \"some-property-changed\")",
                generatedClass.contains(
                        "@Synchronize(property = \"someProperty\", value = \"some-property-changed\") "
                                + "public String getSomeProperty() {"));
    }

    @Test
    public void classContainsGetterAndRelatedChangeEvent_eventContainsPropertyAndGetter() {
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

        Assert.assertThat(
                "Event should save the property value from the source component",
                generatedClass,
                containsString("someproperty = source.getSomeproperty();"));

        Assert.assertThat("Event should have getter for the property",
                generatedClass, containsString(
                        "public String getSomeproperty() { return someproperty; }"));
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
                generatedClass.contains("public void removeAll"));
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
                generatedClass.contains("public void addToNamed1("));
        Assert.assertTrue(
                "The generated class should contain the \"addToNamed2\" method",
                generatedClass.contains("public void addToNamed2("));
        Assert.assertTrue(
                "The generated class should contain the \"addToNamedThree\" method",
                generatedClass.contains("public void addToNamedThree("));
        Assert.assertTrue(
                "The generated class should contain the \"remove\" method",
                generatedClass.contains("public void remove("));
        Assert.assertTrue(
                "The generated class should contain the \"removeAll\" method",
                generatedClass.contains("public void removeAll"));
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
                generatedClass.contains("public void addToNamed1("));
        Assert.assertTrue(
                "The generated class should contain the \"addToNamed2\" method",
                generatedClass.contains("public void addToNamed2("));
        Assert.assertTrue(
                "The generated class should contain the \"addToNamedThree\" method",
                generatedClass.contains("public void addToNamedThree("));
        Assert.assertTrue(
                "The generated class should contain the \"remove\" method",
                generatedClass.contains("public void remove("));
        Assert.assertTrue(
                "The generated class should contain the \"removeAll\" method",
                generatedClass.contains("public void removeAll"));
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
        event.setName("something-happened");
        event.setProperties(Collections.singletonList(eventData));

        componentMetadata.setEvents(Collections.singletonList(event));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        generatedClass = ComponentGeneratorTestUtils
                .removeIndentation(generatedClass);

        Assert.assertTrue(
                "Generated class should contain the SomethingHappenedDetails nested class",
                generatedClass.contains(
                        "public static class SomethingHappenedDetails implements JsonSerializable"));

        Assert.assertTrue(
                "Generated class should contain the addSomethingChangHappenedListener method",
                generatedClass.contains(
                        "public Registration addSomethingHappenedListener( ComponentEventListener<SomethingHappenedEvent<R>> listener)"));

        int indexOfEventDeclaration = generatedClass.indexOf(
                "public static class SomethingHappenedEvent<R extends MyComponent<R>> extends ComponentEvent<R> {");
        int endIndexOfEventDeclaration = generatedClass.indexOf("} }",
                indexOfEventDeclaration);
        String eventDeclaration = generatedClass.substring(
                indexOfEventDeclaration, endIndexOfEventDeclaration + 3);

        Assert.assertTrue(
                "Generated event should contain the getDetails method",
                eventDeclaration.contains(
                        "public SomethingHappenedDetails getDetails() { return new SomethingHappenedDetails().readJson(details); } }"));

    }

    @Test
    public void classContainsOverloadedMethodsForMethodsThatAcceptMultipleTypes() {
        ComponentFunctionParameterData firstParameter = new ComponentFunctionParameterData();
        firstParameter.setName("firstParam");
        firstParameter
                .setType(new HashSet<>(Arrays.asList(ComponentBasicType.STRING,
                        ComponentBasicType.BOOLEAN)));
        ComponentFunctionParameterData secondParameter = new ComponentFunctionParameterData();
        secondParameter.setName("secondParam");
        secondParameter
                .setType(new HashSet<>(Arrays.asList(ComponentBasicType.STRING,
                        ComponentBasicType.BOOLEAN)));

        ComponentFunctionData function = new ComponentFunctionData();
        function.setName("callSomething");
        function.setParameters(Arrays.asList(firstParameter, secondParameter));

        componentMetadata.setMethods(Collections.singletonList(function));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        generatedClass = ComponentGeneratorTestUtils
                .removeIndentation(generatedClass);

        Assert.assertTrue(generatedClass.contains(
                "public void callSomething(String firstParam, String secondParam)"));
        Assert.assertTrue(generatedClass.contains(
                "public void callSomething(String firstParam, boolean secondParam)"));
        Assert.assertTrue(generatedClass.contains(
                "public void callSomething(boolean firstParam, String secondParam)"));
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
        secondParameter
                .setType(new HashSet<>(Arrays.asList(ComponentBasicType.STRING,
                        ComponentBasicType.BOOLEAN)));

        ComponentFunctionData function = new ComponentFunctionData();
        function.setName("callSomething");
        function.setParameters(Arrays.asList(firstParameter, secondParameter));

        componentMetadata.setMethods(Collections.singletonList(function));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        generatedClass = ComponentGeneratorTestUtils
                .removeIndentation(generatedClass);

        Assert.assertTrue(generatedClass.contains(
                "public void callSomething(CallSomethingFirstParam firstParam, String secondParam)"));
        Assert.assertTrue(generatedClass.contains(
                "public void callSomething(CallSomethingFirstParam firstParam, boolean secondParam)"));
    }

    @Test
    public void componentContainsValuePropertyWithNotify_generatedClassExtendsAbstractSinglePropertyField() {
        ComponentPropertyData property = new ComponentPropertyData();
        property.setName("value");
        property.setType(Collections.singleton(ComponentBasicType.STRING));
        property.setNotify(true);
        componentMetadata.setProperties(Collections.singletonList(property));

        String generatedClass = generator.generateClass(componentMetadata, "com.my.test", null);
        generatedClass = ComponentGeneratorTestUtils.removeIndentation(generatedClass);

        Assert.assertThat(generatedClass, CoreMatchers.not(CoreMatchers
                .containsString("getValue()")));
        Assert.assertThat(generatedClass, CoreMatchers
                .containsString("AbstractSinglePropertyField<R, T>"));
    }

    @Test
    public void componentDoesntContainsValueProperty_generatedClassDoesntExtendsAbstractSinglePropertyField() {
        String generatedClass = generator.generateClass(componentMetadata, "com.my.test", null);
        generatedClass = ComponentGeneratorTestUtils.removeIndentation(generatedClass);

        Assert.assertThat(generatedClass, CoreMatchers.not(CoreMatchers
                .containsString("AbstractSinglePropertyField<R, T>")));
    }

    @Test
    public void valuedComponents_HaveAppropriateConstructors() {
        componentMetadata = new ComponentMetadata();
        componentMetadata.setTag("vaadin-date-picker");
        componentMetadata.setName("VaadinDatePicker");
        componentMetadata.setBaseUrl("vaadin-date-picker/vaadin-date-picker.html");
        componentMetadata.setVersion("0.0.1");
        componentMetadata
                .setDescription("Test java doc creation for class file");

        ComponentPropertyData prop1 = new ComponentPropertyData();
        prop1.setName("value");
        prop1.setType(Collections.singleton(ComponentBasicType.STRING));
        prop1.setNotify(true);

        ComponentPropertyData prop2 = new ComponentPropertyData();
        prop2.setName("invalid");
        prop2.setType(Collections.singleton(ComponentBasicType.BOOLEAN));
        prop2.setNotify(true);
        componentMetadata.setProperties(Arrays.asList(prop1, prop2));

        componentMetadata.setBehaviors(Arrays.asList("Vaadin.ControlStateMixin"));

        String generated = generator.withClassNamePrefix("Generated")
                .generateClass(componentMetadata, "com.vaadin.flow.component.datepicker", null);

        generated = ComponentGeneratorTestUtils
                .removeIndentation(generated);

        Assert.assertThat(generated, CoreMatchers.containsString(
                "GeneratedVaadinDatePicker<R extends GeneratedVaadinDatePicker<R, T>, T>"));

        Assert.assertThat(generated, CoreMatchers.containsString(
                "super(\"value\","));

        Assert.assertThat(generated, CoreMatchers.containsString(
                "AbstractSinglePropertyField<R, T>"));

        Assert.assertThat(generated, CoreMatchers.containsString(
                "public GeneratedVaadinDatePicker()"));

        Assert.assertThat(generated, CoreMatchers.containsString(
                "public <P> GeneratedVaadinDatePicker("));

        Assert.assertThat(generated, CoreMatchers.containsString(
                "public GeneratedVaadinDatePicker(T initialValue, T defaultValue, boolean acceptNullValues)"));

        Assert.assertThat(generated, CoreMatchers.containsString(
                "public <P> GeneratedVaadinDatePicker(T initialValue, T defaultValue, Class<P> elementPropertyType,"
                + " SerializableFunction<P, T> presentationToModel, SerializableFunction<T, P> modelToPresentation)"));

        Assert.assertThat(generated, CoreMatchers.containsString(
                "Focusable<R>"));
        
        Assert.assertThat(generated, CoreMatchers.containsString(
                "InvalidChangeEvent<R extends GeneratedVaadinDatePicker<R, ?>>"));
    }

    @Test
    public void checkedRemapedValueComponents_HaveAppropriateConstructors() {
        componentMetadata = new ComponentMetadata();
        componentMetadata.setTag("vaadin-checkbox");
        componentMetadata.setName("VaadinCheckBox");
        componentMetadata.setBaseUrl("vaadin-checkbox/vaadin-checkbox.html");
        componentMetadata.setVersion("0.0.1");
        componentMetadata
                .setDescription("Test java doc creation for class file");

        ComponentPropertyData prop1 = new ComponentPropertyData();
        prop1.setName("checked");
        prop1.setType(Collections.singleton(ComponentBasicType.BOOLEAN));
        prop1.setNotify(true);
        
        ComponentPropertyData prop2 = new ComponentPropertyData();
        prop2.setName("value");
        prop2.setType(Collections.singleton(ComponentBasicType.STRING));
        prop2.setNotify(false);

        componentMetadata.setProperties(Arrays.asList(prop2, prop1));

        String generated = generator.withClassNamePrefix("Generated")
                .generateClass(componentMetadata, "com.vaadin.flow.component.checkbox", null);

        generated = ComponentGeneratorTestUtils.removeIndentation(generated);

        Assert.assertThat(generated, CoreMatchers.containsString(
                "GeneratedVaadinCheckbox<R extends GeneratedVaadinCheckbox<R, T>, T>"));
        Assert.assertThat(generated,
                CoreMatchers.containsString("super(\"checked\","));
        Assert.assertThat(generated,
                CoreMatchers.not(CoreMatchers.containsString("getValue")));
        Assert.assertThat(generated,
                CoreMatchers.not(CoreMatchers.containsString("setValue")));
        Assert.assertThat(generated,
                CoreMatchers.not(CoreMatchers.containsString("getChecked")));
        Assert.assertThat(generated,
                CoreMatchers.not(CoreMatchers.containsString("getChecked")));

        Assert.assertThat(generated, CoreMatchers.containsString(
                "public <P> GeneratedVaadinCheckbox(T initialValue, T defaultValue,"));
        Assert.assertThat(generated, CoreMatchers.containsString(
                "Class<P> elementPropertyType,"));
        Assert.assertThat(generated, CoreMatchers.containsString(
                "SerializableBiFunction<R, P, T> presentationToModel,"));
        Assert.assertThat(generated, CoreMatchers.containsString(
                "SerializableBiFunction<R, T, P> modelToPresentation) {"));
        Assert.assertThat(generated, CoreMatchers.containsString(
                "super(\"checked\", defaultValue, elementPropertyType, presentationToModel,"));
        Assert.assertThat(generated, CoreMatchers.containsString(
                "modelToPresentation);"));
        Assert.assertThat(generated, CoreMatchers.containsString(
                "if (initialValue != null) {"));
        Assert.assertThat(generated, CoreMatchers.containsString(
                "setModelValue(initialValue, false);"));
        Assert.assertThat(generated, CoreMatchers.containsString(
                "setPresentationValue(initialValue);"));
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

        Assert.assertThat(generatedClass, CoreMatchers.containsString(
                "protected JsonObject getObjectPropertyJsonObject()"));
        Assert.assertThat(generatedClass, CoreMatchers.containsString(
                "protected void setObjectProperty(JsonObject objectProperty)"));
        Assert.assertThat(generatedClass, CoreMatchers.containsString(
                "protected JsonArray getArrayPropertyJsonArray()"));
        Assert.assertThat(generatedClass, CoreMatchers.containsString(
                "protected void setArrayProperty(JsonArray arrayProperty)"));
        Assert.assertThat(generatedClass, CoreMatchers.containsString(
                "protected JsonValue getUndefinedPropertyJsonValue()"));
        Assert.assertThat(generatedClass, CoreMatchers.containsString(
                "protected void setUndefinedProperty(JsonValue undefinedProperty)"));

        Assert.assertThat(generatedClass, CoreMatchers.containsString(
                "protected void callSomething(JsonObject objectParam)"));
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
        multiParameter
                .setType(new HashSet<>(Arrays.asList(ComponentBasicType.STRING,
                        ComponentBasicType.OBJECT, ComponentBasicType.ARRAY,
                        ComponentBasicType.UNDEFINED)));

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
                "protected void callSomethingWithObjectAndString(JsonObject objectParam, String stringParam)"));
        Assert.assertTrue(generatedClass.contains(
                "public void callSomethingWithMultiTypes(String multiParam)"));
        Assert.assertTrue(generatedClass.contains(
                "protected void callSomethingWithMultiTypes(JsonObject multiParam)"));
        Assert.assertTrue(generatedClass.contains(
                "protected void callSomethingWithMultiTypes(JsonArray multiParam)"));
        Assert.assertTrue(generatedClass.contains(
                "protected void callSomethingWithMultiTypes(JsonValue multiParam)"));
    }

    @Test
    public void propertyContainsNotify_eventIsGenerated() {
        ComponentPropertyData property = new ComponentPropertyData();
        property.setName("something");
        property.setType(Collections.singleton(ComponentBasicType.STRING));
        property.setNotify(true);
        componentMetadata.setProperties(Collections.singletonList(property));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        generatedClass = ComponentGeneratorTestUtils
                .removeIndentation(generatedClass);

        Assert.assertThat(generatedClass, CoreMatchers.containsString(
                "public void setSomething(String something) {"));
        Assert.assertThat(generatedClass, CoreMatchers.containsString(
                "public Registration addSomethingChangeListener( ComponentEventListener<SomethingChangeEvent<R>> listener) {"));
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

    @Test
    public void htmlJavaDoc() {
        ComponentFunctionData functionData = new ComponentFunctionData();
        functionData.setName("my-method");
        // @formatter:off
        functionData.setDescription(""
                + "This is my method documentation,"
                + "```html\n"
                + "<my-component\n"
                + "label=\"myLabel\">\n"
                + "</my-component>\n"
                + "```");
        // @formatter:on

        componentMetadata.setMethods(Collections.singletonList(functionData));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        Assert.assertTrue("JavaDoc must have escaped HTML",
                generatedClass.contains("&lt;my-component"));
        Assert.assertTrue("JavaDoc must have escaped HTML",
                generatedClass.contains("label=&quot;myLabel&quot;&gt;"));
        Assert.assertFalse("JavaDoc must not have @code",
                generatedClass.contains("@code"));
    }

    @Test
    public void withAbstractClass() {
        generator.withAbstractClass(true);

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        Assert.assertThat(generatedClass, CoreMatchers
                .containsString("public abstract class MyComponent"));
    }

    @Test
    public void withProtectedMethods() {
        generator.withProtectedMethods(true);

        ComponentPropertyData property = new ComponentPropertyData();
        property.setName("something");
        property.setType(Collections.singleton(ComponentBasicType.STRING));
        property.setNotify(true);

        ComponentPropertyData value = new ComponentPropertyData();
        value.setName("value");
        value.setType(Collections.singleton(ComponentBasicType.NUMBER));
        value.setNotify(true);
        componentMetadata.setProperties(Arrays.asList(property, value));

        ComponentFunctionData function = new ComponentFunctionData();
        function.setName("function");
        componentMetadata.setMethods(Collections.singletonList(function));

        componentMetadata.setSlots(Arrays.asList("", "named"));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        Assert.assertThat(generatedClass,
                CoreMatchers.not(CoreMatchers.containsString("HasComponents")));
        Assert.assertThat(generatedClass,
                CoreMatchers.not(CoreMatchers.containsString("HasValue")));

        Assert.assertThat(generatedClass, CoreMatchers.containsString(
                "protected void setSomething(String something)"));
        Assert.assertThat(generatedClass, CoreMatchers
                .containsString("protected String getSomethingString()"));
        Assert.assertThat(generatedClass, CoreMatchers.containsString(
                "protected Registration addSomethingChangeListener("));

        Assert.assertThat(generatedClass,
                CoreMatchers.containsString("protected void function()"));

        Assert.assertThat(generatedClass, CoreMatchers.containsString(
                "protected void addToNamed(Component... components)"));
        Assert.assertThat(generatedClass, CoreMatchers.containsString(
                "protected void remove(Component... components)"));
        Assert.assertThat(generatedClass,
                CoreMatchers.containsString("protected void removeAll"));
    }

}
