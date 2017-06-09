package com.vaadin.generator;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test component generator utility methods
 */
public class ComponentGeneratorUtilsTest {

    @Test
    public void formatStringToValidJavaIdentifier_ReturnsCamelCase() {
        String name = "test_method-for_component";
        String convertedName = ComponentGeneratorUtils
                .formatStringToValidJavaIdentifier(name);

        Assert.assertEquals(
                "Name was not converted to camelCase in the correct way",
                "testMethodForComponent", convertedName);
    }

    @Test
    public void formatStringWithNoConnectorPunctuation_returnsSameString() {
        String name = "testMethod$name";
        String convertedName = ComponentGeneratorUtils
                .formatStringToValidJavaIdentifier(name);

        Assert.assertEquals(
                "Name was not converted to camelCase in the correct way",
                name, convertedName);
    }

    @Test
    public void generateValidJavaClassName_ReturnsCamelCase() {
        String name = "test_method-for_component";
        String convertedName = ComponentGeneratorUtils
                .generateValidJavaClassName(name);

        Assert.assertEquals(
                "Name was not converted to camelCase in the correct way",
                "TestMethodForComponent", convertedName);
    }

    @Test
    public void gettersAndSettersAreGeneratedAsCamelCase() {
        String property = "my_property";

        String formattedProperty = ComponentGeneratorUtils.generateMethodNameForProperty("get", property);

        Assert.assertEquals("Getter was converted wrong.", "getMyProperty", formattedProperty);

        formattedProperty = ComponentGeneratorUtils.generateMethodNameForProperty("set", property);

        Assert.assertEquals("Setter was converted wrong.", "setMyProperty", formattedProperty);
    }

    @Test(expected = AssertionError.class)
    public void propertyFormatterFailsAssertionForNullPrefix() {
        String property = "my_property";

        String formatted =ComponentGeneratorUtils.generateMethodNameForProperty(null, property);

        Assert.assertEquals("", "MyProperty", formatted);
    }
}
