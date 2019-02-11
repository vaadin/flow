package com.vaadin.flow.server.webcomponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponent;
import com.vaadin.flow.component.webcomponent.WebComponentMethod;
import com.vaadin.flow.component.webcomponent.WebComponentProperty;
import com.vaadin.flow.component.webcomponent.WebComponentWrapperTest;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.MockInstantiator;

public class WebComponentGeneratorTest {

    @Test
    public void generatorShouldGenerateAllPropertiesAndMethods() {
        List<PropertyData> propertyDataSet = new ArrayList<>(
                WebComponentGenerator.getPropertyData(MyComponent.class,
                        new MockInstantiator()));

        PropertyData response = new PropertyData("response", String.class,
                "hello");
        PropertyData integer = new PropertyData("integerValue", Integer.class,
                null);
        PropertyData message = new PropertyData("message", String.class, "");

        Assert.assertTrue("All three properties should have been found",
                propertyDataSet
                        .containsAll(Stream.of(response, integer, message)
                                .collect(Collectors.toSet())));

        Assert.assertEquals("Response initial value should have been 'hello'",
                response.getInitialValue(),
                propertyDataSet.get(propertyDataSet.indexOf(response))
                        .getInitialValue());
        Assert.assertEquals("IntegerValue shouldn't have a initial value",
                integer.getInitialValue(),
                propertyDataSet.get(propertyDataSet.indexOf(integer))
                        .getInitialValue());
        Assert.assertEquals(
                "Method property 'message' init value should be default from the annotation",
                message.getInitialValue(),
                propertyDataSet.get(propertyDataSet.indexOf(message))
                        .getInitialValue());

    }

    @Test
    public void extendingPropertiesShouldOverrideInGeneratorPropertiesAndMethods() {
        List<PropertyData> propertyDataSet = new ArrayList<>(
                WebComponentGenerator.getPropertyData(MyExtension.class,
                        new MockInstantiator()));

        PropertyData response = new PropertyData("response", String.class,
                "Hi");
        PropertyData integer = new PropertyData("integerValue", Integer.class,
                null);
        PropertyData message = new PropertyData("message", String.class,
                "extend");

        Assert.assertTrue("All three properties should have been found",
                propertyDataSet
                        .containsAll(Stream.of(response, integer, message)
                                .collect(Collectors.toSet())));

        Assert.assertEquals("Response initial value should have been 'Hi'",
                response.getInitialValue(),
                propertyDataSet.get(propertyDataSet.indexOf(response))
                        .getInitialValue());
        Assert.assertEquals("IntegerValue shouldn't have a initial value",
                integer.getInitialValue(),
                propertyDataSet.get(propertyDataSet.indexOf(integer))
                        .getInitialValue());
        Assert.assertEquals(
                "Method property 'message' init value should be 'extend'",
                message.getInitialValue(),
                propertyDataSet.get(propertyDataSet.indexOf(message))
                        .getInitialValue());
    }

    @Test
    public void generatedReplacementMapContainsExpectedEntries() {
        Set<PropertyData> propertyData = WebComponentGenerator
                .getPropertyData(MyComponent.class, new MockInstantiator());

        Map<String, String> replacementsMap = WebComponentGenerator
                .getReplacementsMap("document.body", "my-component",
                        propertyData, "/foo");

        Assert.assertTrue("Missing dashed tag name",
                replacementsMap.containsKey("TagDash"));
        Assert.assertTrue("Missing camel cased tag name",
                replacementsMap.containsKey("TagCamel"));
        Assert.assertTrue("Missing 'PropertyMethods'",
                replacementsMap.containsKey("PropertyMethods"));
        Assert.assertTrue("Missing 'Properties'",
                replacementsMap.containsKey("Properties"));
        Assert.assertTrue("No 'RootElement' specified",
                replacementsMap.containsKey("RootElement"));
        Assert.assertTrue("Missing servlet context path",
                replacementsMap.containsKey("servlet_context"));

        Assert.assertEquals("my-component", replacementsMap.get("TagDash"));
        Assert.assertEquals("MyComponent", replacementsMap.get("TagCamel"));

        Assert.assertEquals("document.body",
                replacementsMap.get("RootElement"));

        Assert.assertEquals("/foo", replacementsMap.get("servlet_context"));

        String propertyMethods = replacementsMap.get("PropertyMethods");
        Assert.assertTrue(propertyMethods.contains("_sync_message"));
        Assert.assertTrue(propertyMethods.contains("_sync_integerValue"));
        Assert.assertTrue(propertyMethods.contains("_sync_response"));

        String properties = replacementsMap.get("Properties");
        Assert.assertTrue(properties
                .contains("\"message\":{\"type\":\"String\",\"value\":\"\""));
        Assert.assertTrue(properties.contains(
                "\"integerValue\":{\"type\":\"Integer\",\"observer\""));
        Assert.assertTrue(properties.contains(
                "\"response\":{\"type\":\"String\",\"value\":\"hello\""));

    }

    @WebComponent("my-component")
    public static class MyComponent extends Component {

        protected String message;
        protected WebComponentProperty<String> response = new WebComponentProperty<>(
                "hello", String.class);
        protected WebComponentProperty<Integer> integerValue = new WebComponentProperty<>(
                Integer.class);

        public MyComponent() {
            super(new Element("div"));
        }

        @WebComponentMethod("message")
        public void setMessage(String message) {
            this.message = message;
        }

    }

    @WebComponent("my-extension")
    public static class MyExtension
            extends WebComponentWrapperTest.MyComponent {

        protected WebComponentProperty<String> response = new WebComponentProperty<>(
                "Hi", String.class);

        @WebComponentMethod(value = "message", initialValue = "extend")
        public void setMyFancyMessage(String extendedMessage) {
            message = extendedMessage + "!";
        }
    }
}
