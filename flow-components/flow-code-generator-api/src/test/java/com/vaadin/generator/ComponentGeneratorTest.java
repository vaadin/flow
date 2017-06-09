package com.vaadin.generator;

import java.util.ArrayList;

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
        componentMetadata.setVersion("0.0.1");
        componentMetadata
                .setDocumentation("Test java doc creation for class file");
    }

    @Test
    public void generateClass_containsClassJavaDoc() {
        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test");

        Assert.assertTrue("Generated class didn't contain class JavaDoc",
                generatedClass
                        .contains("* " + componentMetadata.getDocumentation()));
    }

    @Test
    public void generateClass_generatedTagIsSameAsTag() {
        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test");

        Assert.assertTrue("Generated class had wrong generated tag",
                generatedClass.contains(
                        "@Generated(\"" + componentMetadata.getTag() + "\")"));
    }

    @Test
    public void generateClass_classNameIsCamelCase() {
        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test");

        Assert.assertTrue("Generated class name was faulty",
                generatedClass.contains("public class MyComponent"));
    }

    @Test
    public void generateClassWithMethod_methodContainsJavaDoc() {
        ComponentFunctionData functionData = new ComponentFunctionData();
        functionData.setName("my-method");
        functionData.setDocumentation("This is my method documentation.");
        componentMetadata.setFunctions(new ArrayList<ComponentFunctionData>() {
            {
                add(functionData);
            }
        });

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test");

        Assert.assertTrue("Method javaDoc was not found", generatedClass
                .contains("* " + functionData.getDocumentation()));
    }

    @Test
    public void generateClassWithMethodWithParameters_methodContainsParamInJavaDoc() {
        ComponentFunctionData functionData = new ComponentFunctionData();
        functionData.setName("my-method");
        functionData.setDocumentation("This is my method documentation.");

        ComponentFunctionParameterData parameter = new ComponentFunctionParameterData();
        parameter.setName("text");
        parameter.setType(ComponentObjectType.STRING);

        functionData
                .setParameters(new ArrayList<ComponentFunctionParameterData>() {
                    {
                        add(parameter);
                    }
                });
        componentMetadata.setFunctions(new ArrayList<ComponentFunctionData>() {
            {
                add(functionData);
            }
        });

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test");

        Assert.assertTrue("JavaDoc for method parameter text was not found", generatedClass
                .contains("* @param " + parameter.getName()));
    }

    @Test
    public void generateClassWithGetterAndSetter_methodContainsJavaDoc() {
        ComponentPropertyData propertyData = new ComponentPropertyData();
        propertyData.setName("name");
        propertyData.setType(ComponentObjectType.STRING);
        propertyData.setDocumentation("This is the name property of the component.");
        componentMetadata.setProperties(new ArrayList<ComponentPropertyData>() {
            {
                add(propertyData);
            }
        });

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test");

        Assert.assertTrue("No getter found", generatedClass.contains("public String getName()"));
        Assert.assertTrue("No setter found", generatedClass.contains("public void setName(java.lang.String name)"));

        Assert.assertTrue("Method javaDoc was not found", generatedClass
                .contains("* " + propertyData.getDocumentation()));

        Assert.assertTrue("JavaDoc parameter for setter was not found", generatedClass
                .contains("* @param " + propertyData.getName()));
    }

    @Test
    public void generateClassWithGetter_methodContainsJavaDoc_noSetter() {
        ComponentPropertyData propertyData = new ComponentPropertyData();
        propertyData.setName("name");
        propertyData.setType(ComponentObjectType.STRING);
        propertyData.setDocumentation("This is the name property of the component.");
        propertyData.setReadOnly(true);
        componentMetadata.setProperties(new ArrayList<ComponentPropertyData>() {
            {
                add(propertyData);
            }
        });

        String generatedClass = generator.generateClass(componentMetadata,
                "com.my.test");

        Assert.assertTrue("No getter found", generatedClass.contains("public String getName()"));
        Assert.assertFalse("Found setter even if it shouldn't exist", generatedClass.contains("setName"));

        Assert.assertTrue("Method javaDoc was not found", generatedClass
                .contains("* " + propertyData.getDocumentation()));
    }
}