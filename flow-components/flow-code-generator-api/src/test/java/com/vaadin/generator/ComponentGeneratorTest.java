package com.vaadin.generator;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.generator.metadata.ComponentFunctionData;
import com.vaadin.generator.metadata.ComponentFunctionParameterData;
import com.vaadin.generator.metadata.ComponentMetadata;
import com.vaadin.generator.metadata.ComponentObjectType;
import com.vaadin.generator.metadata.ComponentPropertyData;

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
        parameter.setType(ComponentObjectType.STRING);

        functionData.setParameters(Arrays.asList(parameter));
        componentMetadata.setMethods(Arrays.asList(functionData));

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", null);

        Assert.assertTrue("JavaDoc for method parameter text was not found",
                generatedClass.contains("* @param " + parameter.getName()));
    }

    @Test
    public void generateClassWithGetterAndSetter_methodContainsJavaDoc() {
        ComponentPropertyData propertyData = new ComponentPropertyData();
        propertyData.setName("name");
        propertyData.setType(ComponentObjectType.STRING);
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
    public void generateClassWithGetter_methodContainsJavaDoc_noSetter() {
        ComponentPropertyData propertyData = new ComponentPropertyData();
        propertyData.setName("name");
        propertyData.setType(ComponentObjectType.STRING);
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
    public void generateClassWithLicenseNote_classContainsLicenseHeader() {
        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test", "some license header");

        Assert.assertTrue("No license header found", generatedClass.startsWith(
                "/*\n * some license header\n */\npackage com.my.test"));
    }

    @Test
    public void generateClassWithStringGetterAndSetter_setterSetsEmptyForNullValue() {
        ComponentPropertyData propertyData = new ComponentPropertyData();
        propertyData.setName("name");
        propertyData.setType(ComponentObjectType.STRING);
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
    public void generateClassWithBooleanGetterAndSetter_setterDoesNotSetEmptyForNullValue() {
        ComponentPropertyData propertyData = new ComponentPropertyData();
        propertyData.setName("required");
        propertyData.setType(ComponentObjectType.BOOLEAN);
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
}
