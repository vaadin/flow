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
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

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
 * @see #generateClass(ComponentMetadata, String, String)
 * @see #generateClass(File, File, String, String)
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
     * @see #generateClass(ComponentMetadata, File, String, String)
     * 
     * @param jsonFile
     *            The input JSON file.
     * @param targetPath
     *            The output base directory for the generated Java file.
     * @param basePackage
     *            The base package to be used for the generated Java class. The
     *            final package of the class is basePackage plus the
     *            {@link ComponentMetadata#getBaseUrl()}.
     * @param licenseNote
     *            A note to be added on top of the class as a comment. Usually
     *            used for license headers.
     * @throws ComponentGenerationException
     *             If an error occurs when generating the class.
     */
    public void generateClass(File jsonFile, File targetPath,
            String basePackage, String licenseNote) {

        generateClass(toMetadata(jsonFile), targetPath, basePackage,
                licenseNote);
    }

    /**
     * Generates and returns the Java class based on the
     * {@link ComponentMetadata}. Doesn't write anything to the disk.
     * 
     * @param metadata
     *            The webcomponent metadata.
     * @param basePackage
     *            The base package to be used for the generated Java class. The
     *            final package of the class is basePackage plus the
     *            {@link ComponentMetadata#getBaseUrl()}.
     * @param licenseNote
     *            A note to be added on top of the class as a comment. Usually
     *            used for license headers.
     * @return The generated Java class in String format.
     * @throws ComponentGenerationException
     *             If an error occurs when generating the class.
     */
    public String generateClass(ComponentMetadata metadata, String basePackage,
            String licenseNote) {

        JavaClassSource javaClass = generateClassSource(metadata, basePackage);
        return addLicenseHeaderIfAvailable(javaClass.toString(), licenseNote);
    }

    /*
     * Gets the JavaClassSource object (note, the license is added externally to
     * the source, since JavaClassSource doesn't support adding a comment to the
     * beginning of the file).
     */
    private JavaClassSource generateClassSource(ComponentMetadata metadata,
            String basePackage) {

        String targetPackage = basePackage;
        if (StringUtils.isNotBlank(metadata.getBaseUrl())) {
            String subPackage = ComponentGeneratorUtils
                    .convertFilePathToPackage(metadata.getBaseUrl());
            if (StringUtils.isNotBlank(subPackage)) {
                targetPackage += "." + subPackage;
            }
        }

        JavaClassSource javaClass = Roaster.create(JavaClassSource.class);
        javaClass.setPackage(targetPackage).setPublic()
                .setSuperType(Component.class).setName(ComponentGeneratorUtils
                        .generateValidJavaClassName(metadata.getName()));

        addGeneratedAnnotation(metadata, javaClass);
        addAnnotation(javaClass, Tag.class, metadata.getTag());

        if (metadata.getProperties() != null) {
            metadata.getProperties().forEach(property -> {
                generateGetterFor(javaClass, property);

                if (!property.isReadOnly()) {
                    generateSetterFor(javaClass, property);
                }
            });
        }

        if (metadata.getMethods() != null) {
            metadata.getMethods().forEach(
                    function -> generateFunctionFor(javaClass, function));
        }

        if (metadata.getEvents() != null) {
            metadata.getEvents().forEach(
                    event -> generateEventListenerFor(javaClass, event));
        }

        if (StringUtils.isNotEmpty(metadata.getDescription())) {
            addJavaDoc(metadata.getDescription(), javaClass.getJavaDoc());
        }

        return javaClass;
    }

    /*
     * Adds the license header to the source, if available. If the license is
     * empty, just returns the original source.
     */
    private String addLicenseHeaderIfAvailable(String source,
            String licenseNote) {

        if (StringUtils.isBlank(licenseNote)) {
            return source;
        }

        return ComponentGeneratorUtils.formatStringToJavaComment(licenseNote)
                + source;
    }

    private void addGeneratedAnnotation(ComponentMetadata metadata,
            JavaClassSource javaClass) {
        Properties apiVersionProperties = getAPIVersionProperties();
        String generator = String.format("Generator: %s#%s",
                ComponentGenerator.class.getName(),
                apiVersionProperties.getProperty("generator.version"));
        String webcomponent = String.format("WebComponent: %s/%s#%s",
                metadata.getBaseUrl(), metadata.getTag(),
                metadata.getVersion());

        String flow = String.format("Flow#%s",
                apiVersionProperties.getProperty("flow.version"));

        String[] generatedValue = new String[] { generator, webcomponent,
                flow };

        javaClass.addAnnotation(Generated.class)
                .setStringArrayValue(generatedValue);
    }

    /**
     * Generates the Java class by using the {@link ComponentMetadata} object.
     * 
     * @param metadata
     *            The webcomponent metadata.
     * @param targetPath
     *            The output base directory for the generated Java file.
     * @param basePackage
     *            The base package to be used for the generated Java class. The
     *            final package of the class is basePackage plus the
     *            {@link ComponentMetadata#getBaseUrl()}.
     * @param licenseNote
     *            A note to be added on top of the class as a comment. Usually
     *            used for license headers.
     * 
     * @throws ComponentGenerationException
     *             If an error occurs when generating the class.
     */
    public void generateClass(ComponentMetadata metadata, File targetPath,
            String basePackage, String licenseNote) {

        JavaClassSource javaClass = generateClassSource(metadata, basePackage);
        String source = addLicenseHeaderIfAvailable(javaClass.toString(),
                licenseNote);

        String fileName = ComponentGeneratorUtils
                .generateValidJavaClassName(javaClass.getName()) + ".java";

        if (!targetPath.isDirectory() && !targetPath.mkdirs()) {
            throw new ComponentGenerationException(
                    "Could not create target directory \"" + targetPath + "\"");
        }
        try {
            Files.write(
                    new File(
                            ComponentGeneratorUtils.convertPackageToDirectory(
                                    targetPath, javaClass.getPackage(), true),
                            fileName).toPath(),
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

        Class<?> setterType = toJavaType(property.getType());
        method.addParameter(setterType, property.getName());

        switch (property.getType()) {
        case ARRAY:
        case OBJECT:
            method.setBody(
                    String.format("getElement().setPropertyJson(\"%s\", %s);",
                            property.getName(), property.getName()));
            break;
        default:
            method.setBody(String.format(
                    "getElement().setProperty(\"%s\", %s);", property.getName(),
                    getSetterValue(property.getName(), setterType)));
            break;
        }

        if (StringUtils.isNotEmpty(property.getDescription())) {
            addJavaDoc(property.getDescription(), method.getJavaDoc());
        }

        method.getJavaDoc().addTagValue("@param", property.getName());
    }

    private String getSetterValue(String propertyName, Class<?> setterType) {
        String value = propertyName;
        // Don't insert null as property value. Insert empty String instead.
        if (String.class.equals(setterType)) {
            value = String.format("%s == null ? \"\" : %s", propertyName,
                    propertyName);
        }
        return value;
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

    public Properties getAPIVersionProperties() {
        // Get properties resource with version information.
        InputStream resourceAsStream = this.getClass()
                .getResourceAsStream("/version.prop");

        Properties config = new Properties();
        try {
            config.load(resourceAsStream);
            return config;
        } catch (IOException e) {
            Logger.getLogger(getClass().getSimpleName()).log(Level.WARNING,
                    "Failed to load properties file 'version.prop'", e);
        }

        return config;
    }
}
