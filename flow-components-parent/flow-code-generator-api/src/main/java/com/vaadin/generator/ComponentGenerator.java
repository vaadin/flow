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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Generated;

import org.apache.commons.lang3.StringUtils;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaDocSource;
import org.jboss.forge.roaster.model.source.MethodSource;
import org.jboss.forge.roaster.model.source.ParameterSource;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.annotations.DomEvent;
import com.vaadin.annotations.EventData;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Synchronize;
import com.vaadin.annotations.Tag;
import com.vaadin.components.NotSupported;
import com.vaadin.flow.event.ComponentEventListener;
import com.vaadin.generator.exception.ComponentGenerationException;
import com.vaadin.generator.metadata.ComponentBasicType;
import com.vaadin.generator.metadata.ComponentEventData;
import com.vaadin.generator.metadata.ComponentFunctionData;
import com.vaadin.generator.metadata.ComponentFunctionParameterData;
import com.vaadin.generator.metadata.ComponentMetadata;
import com.vaadin.generator.metadata.ComponentPropertyBaseData;
import com.vaadin.generator.metadata.ComponentPropertyData;
import com.vaadin.shared.Registration;
import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentEvent;
import com.vaadin.ui.HasStyle;

import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Base class of the component generation process. It takes a
 * {@link ComponentMetadata} as input and generates the corresponding Java class
 * that can interacts with the original webcomponent. The metadata can also be
 * set as a JSON format.
 *
 * @see #generateClass(ComponentMetadata, String, String)
 * @see #generateClass(File, File, String, String)
 */
public class ComponentGenerator {

    private static final String GENERIC_TYPE = "R";
    private static final Logger logger = Logger.getLogger("ComponentGenerator");

    private ObjectMapper mapper;
    private File jsonFile;
    private File targetPath;
    private String basePackage;
    private String licenseNote;
    private String frontendDirectory = "bower_components/";
    private boolean fluentSetters = true;

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
     * Set whether the generator should use fluent setters - setters that return
     * the own object so it's possible to use method chaining.
     * <p>
     * By default, fluentSetters is <code>true</code>.
     * 
     * @param fluentSetters
     *            <code>true</code> to enable fluent setters, <code>false</code>
     *            to disable them.
     * @return this
     */
    public ComponentGenerator withFluentSetters(boolean fluentSetters) {
        this.fluentSetters = fluentSetters;
        return this;
    }

    /**
     * Set the input JSON file.
     *
     * @param jsonFile
     *            The input JSON file.
     * @return this
     */
    public ComponentGenerator withJsonFile(File jsonFile) {
        this.jsonFile = jsonFile;
        return this;
    }

    /**
     * Set the target output directory.
     *
     * @param targetPath
     *            The output base directory for the generated Java file.
     * @return this
     */
    public ComponentGenerator withTargetPath(File targetPath) {
        this.targetPath = targetPath;
        return this;
    }

    /**
     * Set the base package taht will be used.
     *
     * @param basePackage
     *            The base package to be used for the generated Java class. The
     *            final package of the class is basePackage plus the
     *            {@link ComponentMetadata#getBaseUrl()}.
     * @return this
     */
    public ComponentGenerator withBasePackage(String basePackage) {
        this.basePackage = basePackage;
        return this;
    }

    /**
     * Set the license header notice for the file.
     *
     * @param licenseNote
     *            A note to be added on top of the class as a comment. Usually
     *            used for license headers.
     * @return this
     */
    public ComponentGenerator withLicenseNote(String licenseNote) {
        this.licenseNote = licenseNote;
        return this;
    }

    /**
     * Set the import frontend base package. e.g. bower_components
     *
     * @param frontendDirectory
     *            frontend base package
     * @return this
     */
    public ComponentGenerator withFrontendDirectory(String frontendDirectory) {
        if (frontendDirectory == null) {
            return this;
        }
        if (!frontendDirectory.endsWith("/")) {
            this.frontendDirectory = frontendDirectory + "/";
        } else {
            this.frontendDirectory = frontendDirectory;
        }
        return this;
    }

    /**
     * Generate the class according to the set values.
     */
    public void build() {
        generateClass(jsonFile, targetPath, basePackage, licenseNote);
    }

    /**
     * Generates the Java class by reading the webcomponent metadata from a JSON
     * file.
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
     * @see #toMetadata(File)
     * @see #generateClass(ComponentMetadata, File, String, String)
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
                        .generateValidJavaClassName(metadata.getTag()));

        // all components have styles
        javaClass.addInterface(HasStyle.class);

        List<String> classBehaviorsAndMixins = new ArrayList<>();
        classBehaviorsAndMixins.add(javaClass.getName());

        if (metadata.getBehaviors() != null) {
            classBehaviorsAndMixins.addAll(metadata.getBehaviors());
        }

        Set<Class<?>> interfaces = BehaviorRegistry
                .getClassesForBehaviors(classBehaviorsAndMixins);
        interfaces.forEach(javaClass::addInterface);

        addClassAnnotations(metadata, javaClass);

        if (metadata.getProperties() != null) {
            metadata.getProperties().forEach(property -> {
                generateGetterFor(javaClass, property, metadata.getEvents());

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

        if (fluentSetters) {
            javaClass.addTypeVariable().setName(GENERIC_TYPE)
                    .setBounds(javaClass.getName() + "<" + GENERIC_TYPE + ">");
            generateGetSelf(javaClass);
        }

        return javaClass;
    }

    private void generateGetSelf(JavaClassSource javaClass) {
        MethodSource<JavaClassSource> method = javaClass.addMethod()
                .setName("getSelf").setProtected().setReturnType(GENERIC_TYPE);

        method.getJavaDoc()
                .setText(
                        "Gets the narrow typed reference to this object. Subclasses should override this method to support method chaining using the inherited type.")
                .addTagValue("@return", "This object casted to its type.");

        method.setBody("return (" + GENERIC_TYPE + ") this;");
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

    private void addClassAnnotations(ComponentMetadata metadata,
            JavaClassSource javaClass) {

        Properties properties = getProperties("version.prop");
        String generator = String.format("Generator: %s#%s",
                ComponentGenerator.class.getName(),
                properties.getProperty("generator.version"));
        String webComponent = String.format("WebComponent: %s#%s",
                metadata.getName(), metadata.getVersion());

        String flow = String.format("Flow#%s",
                properties.getProperty("flow.version"));

        String[] generatedValue = new String[] { generator, webComponent,
                flow };

        javaClass.addAnnotation(Generated.class)
                .setStringArrayValue(generatedValue);

        addAnnotation(javaClass, Tag.class, metadata.getTag());

        String importPath = metadata.getBaseUrl().replace("\\", "/");
        if (importPath.startsWith("/")) {
            importPath = importPath.substring(1);
        }
        String htmlImport = String.format("frontend://%s%s", frontendDirectory,
                importPath);
        addAnnotation(javaClass, HtmlImport.class, htmlImport);
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
            ComponentPropertyData property, List<ComponentEventData> events) {
        boolean postfixWithVariableType = property.getType().size() > 1;
        for (ComponentBasicType basicType : property.getType()) {
            MethodSource<JavaClassSource> method = javaClass.addMethod()
                    .setPublic().setReturnType(toJavaType(basicType));

            if (basicType == ComponentBasicType.BOOLEAN) {
                method.setName(
                        ComponentGeneratorUtils.generateMethodNameForProperty(
                                "is", property.getName()));
            } else {
                method.setName(ComponentGeneratorUtils
                        .generateMethodNameForProperty("get",
                                property.getName())
                        + (postfixWithVariableType ? StringUtils.capitalize(
                                basicType.name().toLowerCase()) : ""));
            }
            switch (basicType) {
            case STRING:
                method.setBody(String.format(
                        "return getElement().getProperty(\"%s\");",
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
                method.setBody(String.format(
                        "return getElement().getProperty(\"%s\");",
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
            case UNDEFINED:
                method.setBody(String.format(
                        "return (JsonValue) getElement().getPropertyRaw(\"%s\");",
                        property.getName()));
                break;
            }

            // verifies whether the getter needs a @Synchronize annotation by
            // inspecting the event list
            String synchronizationDescription = "";

            if (containsChangedEventForProperty(property.getName(), events)) {
                method.addAnnotation(Synchronize.class)
                        .setStringValue("property", property.getName())
                        .setStringValue(property.getName() + "-changed");

                synchronizationDescription = "This property is synchronized automatically from client side when a \""
                        + property.getName() + "-changed\" event happens.";
            } else {
                synchronizationDescription = "This property is not synchronized automatically from the client side, so the returned value may not be the same as in client side.";
            }

            if (StringUtils.isNotEmpty(property.getDescription())) {
                addJavaDoc(
                        property.getDescription() + "<p>"
                                + synchronizationDescription,
                        method.getJavaDoc());
            } else {
                method.getJavaDoc().setFullText(synchronizationDescription);
            }
        }
    }

    private boolean containsChangedEventForProperty(String property,
            List<ComponentEventData> events) {
        if (events == null) {
            return false;
        }
        String eventName = property + "-changed";
        return events.stream().map(ComponentEventData::getName)
                .anyMatch(name -> name.equals(eventName));
    }

    private void addJavaDoc(String documentation, JavaDocSource<?> javaDoc) {
        String nl = System.getProperty("line.separator");
        String text = String.format("%s%s%s%s",
                "Description copied from corresponding location in WebComponent:",
                nl, nl, documentation.replaceAll("```(.*?)```", "{@code $1}")
                        .replaceAll("`(.*?)`", "{@code $1}"));
        try {
            javaDoc.setFullText(text);
        } catch (IllegalArgumentException ile) {
            logger.log(Level.WARNING,
                    "Javadoc exception for file " + jsonFile.getName(), ile);
            logger.warning("Failed to set javadoc: " + text);
        }
    }

    private void generateSetterFor(JavaClassSource javaClass,
            ComponentPropertyData property) {

        for (ComponentBasicType basicType : property.getType()) {
            MethodSource<JavaClassSource> method = javaClass.addMethod()
                    .setName(ComponentGeneratorUtils
                            .generateMethodNameForProperty("set",
                                    property.getName()))
                    .setPublic();

            Class<?> setterType = toJavaType(basicType);
            method.addParameter(setterType, property.getName());

            switch (basicType) {
            case ARRAY:
            case UNDEFINED:
            case OBJECT:
                method.setBody(String.format(
                        "getElement().setPropertyJson(\"%s\", %s);",
                        property.getName(), property.getName()));
                break;
            default:
                method.setBody(String.format(
                        "getElement().setProperty(\"%s\", %s);",
                        property.getName(),
                        getSetterValue(property.getName(), setterType)));
                break;
            }

            if (StringUtils.isNotEmpty(property.getDescription())) {
                addJavaDoc(property.getDescription(), method.getJavaDoc());
            }

            method.getJavaDoc().addTagValue("@param", property.getName());

            if (fluentSetters) {
                method.setReturnType(GENERIC_TYPE);
                method.setBody(method.getBody() + "\n" + "return getSelf();");
                method.getJavaDoc().addTagValue("@return",
                        "This instance, for method chaining.");
            }
        }
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
                .setReturnTypeVoid();

        if (StringUtils.isNotEmpty(function.getDescription())) {
            addJavaDoc(function.getDescription(), method.getJavaDoc());
        }

        // methods with return values are not supported
        if (function.getReturns() != null
                && function.getReturns() != ComponentBasicType.UNDEFINED) {
            method.setProtected();
            method.addAnnotation(NotSupported.class);
            generateFunctionParameters(function, method);
            method.getJavaDoc().addTagValue("@return",
                    "It would return a " + toJavaType(function.getReturns()));
            method.setBody("");
        } else {
            method.setPublic();
            method.setBody(String.format("getElement().callFunction(\"%s\"%s);",
                    function.getName(),
                    generateFunctionParameters(function, method)));
        }

    }

    /**
     * Adds the function parameters in a string separated with commas.
     * <p>
     * Also adds the parameters to the to the javadocs for the given method.
     *
     * @param function
     *            the function data
     * @param method
     *            the method to add javadocs to, not {@code null}
     * @return a string of the parameters of the function, or an empty string if
     *         no parameters
     */
    private String generateFunctionParameters(ComponentFunctionData function,
            MethodSource<JavaClassSource> method) {
        if (function.getParameters() == null
                || function.getParameters().isEmpty()) {
            return "";
        }

        StringBuilder params = new StringBuilder();
        for (ComponentFunctionParameterData param : function.getParameters()) {
            List<ComponentBasicType> typeVariants = param.getType();
            boolean useTypePostfixForVariableName = typeVariants.size() > 1;

            // there might be multiple types accepted for same parameter,
            // for now different types are allowed and parameter name is
            // postfixed with type
            for (ComponentBasicType basicType : typeVariants) {

                String formattedName = StringUtils
                        .uncapitalize(ComponentGeneratorUtils
                                .formatStringToValidJavaIdentifier(
                                        param.getName()));
                if (useTypePostfixForVariableName) {
                    formattedName = formattedName + StringUtils
                            .capitalize(basicType.name().toLowerCase());
                }

                method.addParameter(toJavaType(basicType), formattedName);
                params.append(", ").append(formattedName);

                method.getJavaDoc().addTagValue("@param",
                        param.getName() + (useTypePostfixForVariableName
                                ? " can be <code>null</code>" : ""));
            }
        }
        return params.toString();
    }

    private void generateEventListenerFor(JavaClassSource javaClass,
            ComponentEventData event) {
        String eventName = ComponentGeneratorUtils
                .formatStringToValidJavaIdentifier(event.getName());

        JavaClassSource eventListener = createEventListenerEventClass(javaClass,
                event);

        javaClass.addNestedType(eventListener);
        MethodSource<JavaClassSource> method = javaClass.addMethod()
                .setName("add" + StringUtils.capitalize(eventName + "Listener"))
                .setPublic().setReturnType(Registration.class);
        method.addParameter(
                "ComponentEventListener<" + eventListener.getName() + ">",
                "listener");

        method.setBody(String.format("return addListener(%s.class, listener);",
                eventListener.getName()));
    }

    private JavaClassSource createEventListenerEventClass(
            JavaClassSource javaClass, ComponentEventData event) {
        String eventName = ComponentGeneratorUtils
                .formatStringToValidJavaIdentifier(event.getName());
        String eventClassName = StringUtils.capitalize(eventName);
        String eventListenerString = String.format(
                "public static class %sEvent extends ComponentEvent<%s> {}",
                eventClassName, javaClass.getName());

        JavaClassSource eventListener = Roaster.parse(JavaClassSource.class,
                eventListenerString);

        MethodSource<JavaClassSource> eventConstructor = eventListener
                .addMethod().setConstructor(true).setPublic()
                .setBody("super(source, fromClient);");
        eventConstructor.addParameter(javaClass.getName(), "source");
        eventConstructor.addParameter("boolean", "fromClient");

        for (ComponentPropertyBaseData property : event.getProperties()) {
            // Add new parameter to constructor
            final String propertyName = property.getName();
            Class<?> propertyJavaType;

            if (!property.getType().isEmpty()) {
                // for varying types, using the first type declared in the JSDoc
                // it is anyway very rare to have varying property type
                propertyJavaType = toJavaType(property.getType().get(0));
            } else { // object property
                propertyJavaType = JsonObject.class;
            }
            String normalizedProperty = ComponentGeneratorUtils
                    .formatStringToValidJavaIdentifier(propertyName);
            ParameterSource<JavaClassSource> parameter = eventConstructor
                    .addParameter(propertyJavaType, normalizedProperty);
            parameter.addAnnotation(EventData.class).setLiteralValue(
                    String.format("\"event.%s\"", propertyName));
            // Create private field
            eventListener.addProperty(propertyJavaType, normalizedProperty)
                    .setAccessible(true).setMutable(false);

            // Set value to private field
            eventConstructor.setBody(String.format("%s%nthis.%s = %s;",
                    eventConstructor.getBody(), normalizedProperty,
                    normalizedProperty));
            // Add the EventData as a import
            javaClass.addImport(EventData.class);
        }

        eventListener.addAnnotation(DomEvent.class)
                .setLiteralValue("\"" + event.getName() + "\"");

        // Add event imports.
        javaClass.addImport(DomEvent.class);
        javaClass.addImport(ComponentEvent.class);
        javaClass.addImport(ComponentEventListener.class);

        return eventListener;
    }

    private Class<?> toJavaType(ComponentBasicType type) {
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
        case UNDEFINED:
            return JsonValue.class;
        default:
            throw new ComponentGenerationException(
                    "Not a supported type: " + type);
        }
    }

    private Properties getProperties(String fileName) {
        Properties config = new Properties();

        // Get properties resource with version information.
        try (InputStream resourceAsStream = this.getClass()
                .getResourceAsStream("/" + fileName)) {
            config.load(resourceAsStream);
            return config;
        } catch (IOException e) {
            Logger.getLogger(getClass().getSimpleName()).log(Level.WARNING,
                    "Failed to load properties file '" + fileName + "'", e);
        }

        return config;
    }
}
