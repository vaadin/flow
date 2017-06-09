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

import javax.annotation.Generated;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.util.Date;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaDocSource;
import org.jboss.forge.roaster.model.source.MethodSource;

import com.vaadin.annotations.Tag;
import com.vaadin.flow.dom.DomEventListener;
import com.vaadin.generator.exception.ComponentGenerationException;
import com.vaadin.generator.metadata.ComponentEventData;
import com.vaadin.generator.metadata.ComponentFunctionData;
import com.vaadin.generator.metadata.ComponentFunctionParameterData;
import com.vaadin.generator.metadata.ComponentMetadata;
import com.vaadin.generator.metadata.ComponentObjectType;
import com.vaadin.generator.metadata.ComponentPropertyData;
import com.vaadin.shared.Registration;
import com.vaadin.ui.Component;

import elemental.json.JsonArray;
import elemental.json.JsonObject;

/**
 * Base class of the component generation process. It takes a
 * {@link ComponentMetadata} as input and generates the corresponding Java class
 * that can interacts with the original webcomponent. The metadata can also be
 * set as a JSON format.
 * 
 * @see #generateClass(ComponentMetadata, String)
 * @see #generateClass(File, File, String)
 *
 */
public class ComponentGenerator {

    private ObjectMapper mapper;

    /**
     * Converts the JSON file to {@link ComponentMetadata}.
     * 
     * @param jsonFile
     *            The input JSON file.
     * @return the converted ComponentMetadata.
     * @throws ComponentGenerationException
     *             If an error occurs when reading the file.
     */
    protected ComponentMetadata toMetadata(File jsonFile) {
        try {
            return getObjectMapper().readValue(jsonFile,
                    ComponentMetadata.class);
        } catch (IOException e) {
            throw new ComponentGenerationException(
                    "Error reading JSON file \"" + jsonFile + "\"", e);
        }
    }

    private synchronized ObjectMapper getObjectMapper() {
        if (mapper == null) {
            JsonFactory factory = new JsonFactory();
            factory.enable(JsonParser.Feature.ALLOW_COMMENTS);
            mapper = new ObjectMapper(factory);
        }
        return mapper;
    }

    /**
     * Generates the Java class by reading the webcomponent metadata from a JSON
     * file.
     * 
     * @see #toMetadata(File)
     * @see #generateClass(ComponentMetadata, File, String)
     * 
     * @param jsonFile
     *            The input JSON file.
     * @param targetPath
     *            The output base directory for the generated Java file.
     * @param basePackage
     *            The package to be used for the generated Java class.
     * @throws ComponentGenerationException
     *             If an error occurs when generating the class.
     */
    public void generateClass(File jsonFile, File targetPath,
            String basePackage) {

        generateClass(toMetadata(jsonFile), targetPath, basePackage);
    }

    /**
     * Generates and returns the Java class based on the
     * {@link ComponentMetadata}. Doesn't write anything to the disk.
     * 
     * @param metadata
     *            The webcomponent metadata.
     * @param basePackage
     *            The package to be used for the generated Java class.
     * @return The generated Java class in String format.
     * @throws ComponentGenerationException
     *             If an error occurs when generating the class.
     */
    public String generateClass(ComponentMetadata metadata,
            String basePackage) {
        JavaClassSource javaClass = Roaster.create(JavaClassSource.class);
        javaClass.setPackage(basePackage).setPublic()
                .setSuperType(Component.class).setName(ComponentGeneratorUtils
                        .generateValidJavaClassName(metadata.getName()));

        addAnnotation(javaClass, Generated.class,
                ComponentGenerator.class.getName());
        addAnnotation(javaClass, Tag.class, metadata.getTag());

        if (metadata.getProperties() != null) {
            for (ComponentPropertyData property : metadata.getProperties()) {
                generateGetterFor(javaClass, property);

                if (!property.isReadOnly()) {
                    generateSetterFor(javaClass, property);
                }
            }
        }

        if (metadata.getMethods() != null) {
            for (ComponentFunctionData function : metadata.getMethods()) {
                generateFunctionFor(javaClass, function);
            }
        }

        if (metadata.getEvents() != null) {
            for (ComponentEventData event : metadata.getEvents()) {
                generateEventListenerFor(javaClass, event);
            }
        }

        if (StringUtils.isNotEmpty(metadata.getDescription())) {
            addJavaDoc(metadata.getDescription(), javaClass.getJavaDoc());
        }

        return javaClass.toString();
    }

    /**
     * Generates the Java class by using the {@link ComponentMetadata} object.
     * 
     * @param metadata
     *            The webcomponent metadata.
     * @param targetPath
     *            The output base directory for the generated Java file.
     * @param basePackage
     *            The package to be used for the generated Java class.
     * 
     * @throws ComponentGenerationException
     *             If an error occurs when generating the class.
     */
    public void generateClass(ComponentMetadata metadata, File targetPath,
            String basePackage) {

        String source = generateClass(metadata, basePackage);
        String fileName = ComponentGeneratorUtils
                .generateValidJavaClassName(metadata.getName()) + ".java";

        if (!targetPath.isDirectory() && !targetPath.mkdirs()) {
            throw new ComponentGenerationException(
                    "Could not create target directory \"" + targetPath + "\"");
        }
        try {
            Files.write(
                    new File(ComponentGeneratorUtils.convertPackageToDirectory(
                            targetPath, basePackage, true), fileName).toPath(),
                    source.getBytes("UTF-8"));
        } catch (IOException ex) {
            throw new ComponentGenerationException(
                    "Error writing the generated Java source file \"" + fileName
                            + "\" at \"" + targetPath + "\" for component \""
                            + metadata.getName() + "\"",
                    ex);
        }
    }

    private void addAnnotation(JavaClassSource javaClass,
            Class<? extends Annotation> annotation, String literalValue) {
        javaClass.addAnnotation(annotation)
                .setLiteralValue("\"" + literalValue + "\"");
    }

    private void generateGetterFor(JavaClassSource javaClass,
            ComponentPropertyData property) {

        MethodSource<JavaClassSource> method = javaClass.addMethod().setPublic()
                .setReturnType(toJavaType(property.getType()));

        if (property.getType() == ComponentObjectType.BOOLEAN) {
            method.setName(ComponentGeneratorUtils
                    .generateMethodNameForProperty("is", property.getName()));
        } else {
            method.setName(ComponentGeneratorUtils
                    .generateMethodNameForProperty("get", property.getName()));
        }
        switch (property.getType()) {
        case STRING:
            method.setBody(
                    String.format("return getElement().getProperty(\"%s\");",
                            property.getName()));
            break;
        case BOOLEAN:
            method.setBody(String.format(
                    "return getElement().getProperty(\"%s\", false);",
                    property.getName()));
            break;
        case NUMBER:
            method.setBody(String.format(
                    "return getElement().getProperty(\"%s\", 0.0);",
                    property.getName()));
            break;
        case DATE:
            method.setBody(
                    String.format("return getElement().getProperty(\"%s\");",
                            property.getName()));
            break;
        case ARRAY:
            method.setBody(String.format(
                    "return (JsonArray) getElement().getPropertyRaw(\"%s\");",
                    property.getName()));
            break;
        case OBJECT:
            method.setBody(String.format(
                    "return (JsonObject) getElement().getPropertyRaw(\"%s\");",
                    property.getName()));
            break;
        }

        if (StringUtils.isNotEmpty(property.getDescription())) {
            addJavaDoc(property.getDescription(), method.getJavaDoc());
        }
    }

    private void addJavaDoc(String documentation, JavaDocSource<?> javaDoc) {
        String nl = System.getProperty("line.separator");
        String text = String.format("%s%s%s%s",
                "Description copied from corresponding location in WebComponent:",
                nl, nl, documentation);
        javaDoc.setFullText(text);
    }

    private void generateSetterFor(JavaClassSource javaClass,
            ComponentPropertyData property) {

        MethodSource<JavaClassSource> method = javaClass.addMethod()
                .setName(ComponentGeneratorUtils.generateMethodNameForProperty(
                        "set", property.getName()))
                .setPublic().setReturnTypeVoid();

        method.addParameter(toJavaType(property.getType()), property.getName());

        switch (property.getType()) {
        case ARRAY:
        case OBJECT:
            method.setBody(
                    String.format("getElement().setPropertyJson(\"%s\", %s);",
                            property.getName(), property.getName()));
            break;
        default:
            method.setBody(
                    String.format("getElement().setProperty(\"%s\", %s);",
                            property.getName(), property.getName()));
            break;
        }

        if (StringUtils.isNotEmpty(property.getDescription())) {
            addJavaDoc(property.getDescription(), method.getJavaDoc());
        }

        method.getJavaDoc().addTagValue("@param", property.getName());
    }

    private void generateFunctionFor(JavaClassSource javaClass,
            ComponentFunctionData function) {

        MethodSource<JavaClassSource> method = javaClass.addMethod()
                .setName(StringUtils.uncapitalize(ComponentGeneratorUtils
                        .formatStringToValidJavaIdentifier(function.getName())))
                .setPublic().setReturnTypeVoid();

        if (StringUtils.isNotEmpty(function.getDescription())) {
            addJavaDoc(function.getDescription(), method.getJavaDoc());
        }

        StringBuilder params = new StringBuilder();
        if (function.getParameters() != null
                && !function.getParameters().isEmpty()) {

            for (ComponentFunctionParameterData param : function
                    .getParameters()) {
                String formattedName = StringUtils
                        .uncapitalize(ComponentGeneratorUtils
                                .formatStringToValidJavaIdentifier(
                                        param.getName()));
                method.addParameter(toJavaType(param.getType()), formattedName);
                params.append(", ").append(formattedName);

                method.getJavaDoc().addTagValue("@param", param.getName());
            }
        }

        method.setBody(String.format("getElement().callFunction(\"%s\"%s);",
                function.getName(), params.toString()));
    }

    private void generateEventListenerFor(JavaClassSource javaClass,
            ComponentEventData event) {

        MethodSource<JavaClassSource> method = javaClass.addMethod()
                .setName("add" + StringUtils.capitalize(ComponentGeneratorUtils
                        .formatStringToValidJavaIdentifier(event.getName())
                        + "Listener"))
                .setPublic().setReturnType(Registration.class);
        method.addParameter(DomEventListener.class, "listener");

        method.setBody(String.format(
                "return getElement().addEventListener(\"%s\", listener);",
                event.getName()));
    }

    private Class<?> toJavaType(ComponentObjectType type) {
        switch (type) {
        case STRING:
            return String.class;
        case NUMBER:
            return double.class;
        case BOOLEAN:
            return boolean.class;
        case ARRAY:
            return JsonArray.class;
        case DATE:
            return Date.class;
        case OBJECT:
            return JsonObject.class;
        default:
            throw new ComponentGenerationException(
                    "Not a supported type: " + type);
        }
    }
}
