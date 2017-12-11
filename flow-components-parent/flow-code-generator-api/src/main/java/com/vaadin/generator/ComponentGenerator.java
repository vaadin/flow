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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.Visibility;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaDocSource;
import org.jboss.forge.roaster.model.source.MethodSource;
import org.jboss.forge.roaster.model.source.ParameterSource;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.dom.Element;
import com.vaadin.generator.exception.ComponentGenerationException;
import com.vaadin.generator.metadata.ComponentBasicType;
import com.vaadin.generator.metadata.ComponentEventData;
import com.vaadin.generator.metadata.ComponentFunctionData;
import com.vaadin.generator.metadata.ComponentMetadata;
import com.vaadin.generator.metadata.ComponentObjectType;
import com.vaadin.generator.metadata.ComponentPropertyBaseData;
import com.vaadin.generator.metadata.ComponentPropertyData;
import com.vaadin.generator.metadata.ComponentType;
import com.vaadin.generator.registry.BehaviorRegistry;
import com.vaadin.generator.registry.ExclusionRegistry;
import com.vaadin.generator.registry.PropertyNameRemapRegistry;
import com.vaadin.shared.Registration;
import com.vaadin.ui.Component;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.ComponentSupplier;
import com.vaadin.ui.common.HasComponents;
import com.vaadin.ui.common.HasStyle;
import com.vaadin.ui.common.HasText;
import com.vaadin.ui.common.HasValue;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.common.JsonSerializable;
import com.vaadin.ui.common.NotSupported;
import com.vaadin.ui.event.ComponentEvent;
import com.vaadin.ui.event.ComponentEventListener;
import com.vaadin.ui.event.DomEvent;
import com.vaadin.ui.event.EventData;
import com.vaadin.ui.event.Synchronize;

import elemental.json.JsonObject;

import static java.nio.charset.StandardCharsets.UTF_8;

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
    private static final String JAVADOC_THROWS = "@throws";
    private static final String JAVADOC_SEE = "@see";
    private static final String JAVADOC_PARAM = "@param";
    private static final String JAVADOC_RETURN = "@return";
    private static final String GENERIC_TYPE = "R";
    private static final String GENERIC_TYPE_DECLARATION = '<' + GENERIC_TYPE
            + '>';
    private static final String PROPERTY_CHANGE_EVENT_POSTFIX = "-changed";

    private ObjectMapper mapper;
    private File jsonFile;
    private File targetPath;
    private String basePackage;
    private String classNamePrefix;
    private String licenseNote;
    private String frontendDirectory = "bower_components/";
    // https://github.com/vaadin/flow/issues/2370
    private boolean fluentSetters;

    private final JavaDocFormatter javaDocFormatter = new JavaDocFormatter();

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
     * Set the base package that will be used.
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
     * Set a prefix for the name of all generated classes. e.g. "Generated"
     *
     * @param classNamePrefix
     *            the class name prefix
     * @return this
     */
    public ComponentGenerator withClassNamePrefix(String classNamePrefix) {
        this.classNamePrefix = classNamePrefix;
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

    /**
     * Generates and returns the Java class based on the JSON file provide.
     * Doesn't write anything to the disk.
     *
     * @param jsonFile
     *            The webcomponent JSON metadata.
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
     * @see #generateClass(ComponentMetadata, String, String)
     */
    public String generateClass(File jsonFile, String basePackage,
            String licenseNote) {
        return generateClass(toMetadata(jsonFile), basePackage, licenseNote);
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

                int firstDot = subPackage.indexOf('.');
                if (firstDot > 0) {
                    String firstSegment = subPackage.substring(0, firstDot);
                    String lastSegment = subPackage.substring(firstDot + 1);
                    subPackage = lastSegment.replace(".", "");
                    if (!"vaadin".equals(firstSegment)) {
                        subPackage = firstSegment + "." + subPackage;
                    }
                }

                targetPackage += "." + subPackage;
            }
        }

        JavaClassSource javaClass = Roaster.create(JavaClassSource.class);
        javaClass.setPackage(targetPackage).setPublic()
                .setName(getGeneratedClassName(metadata.getTag()));

        if (metadata.getParentTagName() != null) {
            javaClass.setSuperType(
                    getGeneratedClassName(metadata.getParentTagName())
                            + GENERIC_TYPE_DECLARATION);
        } else {
            javaClass.setSuperType(Component.class);
            addInterfaces(metadata, javaClass);
        }

        javaClass.addTypeVariable().setName(GENERIC_TYPE)
                .setBounds(javaClass.getName() + GENERIC_TYPE_DECLARATION);

        addClassAnnotations(metadata, javaClass);

        if (metadata.getProperties() != null) {
            generateEventsForPropertiesWithNotify(metadata);
            generateGettersAndSetters(metadata, javaClass);
        }

        if (metadata.getMethods() != null) {
            metadata.getMethods().stream()
                    .filter(function -> !ExclusionRegistry.isMethodExcluded(
                            metadata.getTag(), function.getName()))
                    .forEach(
                            function -> generateMethodFor(javaClass, function));
        }

        if (metadata.getEvents() != null) {
            metadata.getEvents().stream()
                    .filter(event -> !ExclusionRegistry.isEventExcluded(
                            metadata.getTag(), event.getName()))
                    .forEach(event -> generateEventListenerFor(javaClass,
                            metadata, event));
        }

        if (metadata.getSlots() != null && !metadata.getSlots().isEmpty()) {
            generateAdders(metadata, javaClass);
        }

        if (StringUtils.isNotEmpty(metadata.getDescription())) {
            addMarkdownJavaDoc(metadata.getDescription(),
                    javaClass.getJavaDoc());
        }

        generateConstructors(javaClass);

        return javaClass;
    }

    private void generateEventsForPropertiesWithNotify(
            ComponentMetadata metadata) {
        metadata.getProperties().stream()
                .filter(property -> !ExclusionRegistry.isPropertyExcluded(
                        metadata.getTag(), property.getName()))
                .filter(ComponentPropertyData::isNotify).forEachOrdered(
                        property -> generateEventForPropertyWithNotify(metadata,
                                property));
    }

    private void generateEventForPropertyWithNotify(ComponentMetadata metadata,
            ComponentPropertyData property) {
        String eventName = ComponentGeneratorUtils
                .convertCamelCaseToHyphens(property.getName()) + "-changed";
        List<ComponentEventData> events = metadata.getEvents();
        if (events == null) {
            events = new ArrayList<>();
            metadata.setEvents(events);
        }
        if (events.stream()
                .anyMatch(event -> event.getName().equals(eventName))) {
            return;
        }
        ComponentEventData event = new ComponentEventData();
        event.setName(eventName);
        event.setDescription(String.format(
                "Event fired every time the `%s` property is changed.",
                property.getName()));
        event.setProperties(Collections.singletonList(property));
        events.add(event);
    }

    private String getGeneratedClassName(String tagName) {
        return ComponentGeneratorUtils.generateValidJavaClassName(
                (classNamePrefix == null ? "" : classNamePrefix + "-")
                        + tagName);
    }

    private void generateConstructors(JavaClassSource javaClass) {
        boolean generateDefaultConstructor = false;
        if (javaClass.hasInterface(HasText.class)) {
            generateDefaultConstructor = true;
            MethodSource<JavaClassSource> constructor = javaClass.addMethod()
                    .setConstructor(true).setPublic().setBody("setText(text);");
            constructor.addParameter(String.class.getSimpleName(), "text");
            constructor.getJavaDoc().setText(
                    "Sets the given string as the content of this component.")
                    .addTagValue(JAVADOC_PARAM, "text the text content to set")
                    .addTagValue(JAVADOC_SEE, "HasText#setText(String)");

        } else if (javaClass.hasInterface(HasComponents.class)) {
            generateDefaultConstructor = true;
            MethodSource<JavaClassSource> constructor = javaClass.addMethod()
                    .setConstructor(true).setPublic()
                    .setBody("add(components);");
            ComponentGeneratorUtils.addMethodParameter(javaClass, constructor,
                    Component.class, "components").setVarArgs(true);
            constructor.getJavaDoc().setText(
                    "Adds the given components as children of this component.")
                    .addTagValue(JAVADOC_PARAM,
                            "components the components to add")
                    .addTagValue(JAVADOC_SEE,
                            "HasComponents#add(Component...)");
        }

        if (generateDefaultConstructor) {
            javaClass.addMethod().setConstructor(true).setPublic().setBody("")
                    .getJavaDoc().setText("Default constructor.");
        }
    }

    private void addInterfaces(ComponentMetadata metadata,
            JavaClassSource javaClass) {

        // all components have styles
        javaClass.addInterface(HasStyle.class);

        List<String> classBehaviorsAndMixins = new ArrayList<>();
        classBehaviorsAndMixins.add(metadata.getTag());

        if (metadata.getBehaviors() != null) {
            classBehaviorsAndMixins.addAll(metadata.getBehaviors());
        }

        if (metadata.getMixins() != null) {
            classBehaviorsAndMixins.addAll(metadata.getMixins());
        }

        Set<Class<?>> interfaces = BehaviorRegistry
                .getClassesForBehaviors(classBehaviorsAndMixins);

        interfaces.forEach(clazz -> {
            if (clazz.getTypeParameters().length > 0) {
                javaClass.addInterface(
                        clazz.getName() + GENERIC_TYPE_DECLARATION);
            } else {
                javaClass.addInterface(clazz);
            }
        });

        boolean componentSupplierAdded = interfaces.stream()
                .filter(ComponentSupplier.class::isAssignableFrom).count() > 0;

        if (!componentSupplierAdded) {
            javaClass.addInterface(ComponentSupplier.class.getName()
                    + GENERIC_TYPE_DECLARATION);
        }
    }

    private void generateGettersAndSetters(ComponentMetadata metadata,
            JavaClassSource javaClass) {

        metadata.getProperties().stream()
                .filter(property -> !ExclusionRegistry.isPropertyExcluded(
                        metadata.getTag(), property.getName()))
                .forEachOrdered(property -> {
                    generateGetterFor(javaClass, metadata, property,
                            metadata.getEvents());

                    if (!property.isReadOnly()) {
                        generateSetterFor(javaClass, metadata, property);
                    }
                });
    }

    private void generateAdders(ComponentMetadata metadata,
            JavaClassSource javaClass) {

        boolean hasDefaultSlot = false;
        boolean hasNamedSlot = false;

        for (String slot : metadata.getSlots()) {
            if (StringUtils.isEmpty(slot)) {
                hasDefaultSlot = true;
            } else {
                hasNamedSlot = true;
                generateAdder(slot, javaClass);
            }
        }

        if (hasDefaultSlot) {
            javaClass.addInterface(HasComponents.class);
        }

        if (hasNamedSlot) {
            generateRemovers(javaClass, hasDefaultSlot);
        }
    }

    private void generateAdder(String slot, JavaClassSource javaClass) {
        String methodName = ComponentGeneratorUtils
                .generateMethodNameForProperty("addTo", slot);
        MethodSource<JavaClassSource> method = javaClass.addMethod().setPublic()
                .setName(methodName);
        ComponentGeneratorUtils.addMethodParameter(javaClass, method,
                Component.class, "components").setVarArgs(true);
        method.setBody(String.format(
                "for (Component component : components) {%n component.getElement().setAttribute(\"slot\", \"%s\");%n getElement().appendChild(component.getElement());%n }",
                slot));

        method.getJavaDoc().setText(String.format(
                "Adds the given components as children of this component at the slot '%s'.",
                slot))
                .addTagValue(JAVADOC_PARAM, "components The components to add.")
                .addTagValue(JAVADOC_SEE,
                        "<a href=\"https://developer.mozilla.org/en-US/docs/Web/HTML/Element/slot\">MDN page about slots</a>")
                .addTagValue(JAVADOC_SEE,
                        "<a href=\"https://html.spec.whatwg.org/multipage/scripting.html#the-slot-element\">Spec website about slots</a>");
        addFluentReturnToMethod(method);
    }

    private void generateRemovers(JavaClassSource javaClass,
            boolean useOverrideAnnotation) {

        MethodSource<JavaClassSource> removeMethod = javaClass.addMethod()
                .setPublic().setReturnTypeVoid().setName("remove");
        ComponentGeneratorUtils.addMethodParameter(javaClass, removeMethod,
                Component.class, "components").setVarArgs(true);
        removeMethod.setBody(
                String.format("for (Component component : components) {%n"
                        + "if (getElement().equals(component.getElement().getParent())) {%n"
                        + "component.getElement().removeAttribute(\"slot\");%n"
                        + "getElement().removeChild(component.getElement());%n "
                        + "}%n" + "else {%n"
                        + "throw new IllegalArgumentException(\"The given component (\" + component + \") is not a child of this component\");%n"
                        + "}%n }"));

        if (useOverrideAnnotation) {
            removeMethod.addAnnotation(Override.class);
        } else {
            removeMethod.getJavaDoc().setText(String.format(
                    "Removes the given child components from this component."))
                    .addTagValue(JAVADOC_PARAM,
                            "components The components to remove.")
                    .addTagValue(JAVADOC_THROWS,
                            "IllegalArgumentException if any of the components is not a child of this component.");
        }

        MethodSource<JavaClassSource> removeAllMethod = javaClass.addMethod()
                .setPublic().setReturnTypeVoid().setName("removeAll");
        removeAllMethod.setBody(String.format(
                "getElement().getChildren().forEach(child -> child.removeAttribute(\"slot\"));%n"
                        + "getElement().removeAllChildren();"));
        if (useOverrideAnnotation) {
            removeAllMethod.addAnnotation(Override.class);
        } else {
            javaClass.addImport(Element.class);
            removeAllMethod.getJavaDoc().setText(String.format(
                    "Removes all contents from this component, this includes child components, "
                            + "text content as well as child elements that have been added directly to "
                            + "this component using the {@link Element} API."));
        }
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

        javaClass.addAnnotation(Tag.class).setStringValue(metadata.getTag());

        String importPath = metadata.getBaseUrl().replace("\\", "/");
        if (importPath.startsWith("/")) {
            importPath = importPath.substring(1);
        }
        String htmlImport = String.format("frontend://%s%s", frontendDirectory,
                importPath);
        javaClass.addAnnotation(HtmlImport.class).setStringValue(htmlImport);
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
                    source.getBytes(UTF_8));
        } catch (IOException ex) {
            throw new ComponentGenerationException(
                    "Error writing the generated Java source file \"" + fileName
                            + "\" at \"" + targetPath + "\" for component \""
                            + metadata.getName() + "\"",
                    ex);
        }
    }

    private void generateGetterFor(JavaClassSource javaClass,
            ComponentMetadata metadata, ComponentPropertyData property,
            List<ComponentEventData> events) {

        String propertyJavaName = getJavaNameForProperty(metadata,
                property.getName());

        if (containsObjectType(property)) {
            JavaClassSource nestedClass = generateNestedPojo(javaClass,
                    property.getObjectType().get(0),
                    propertyJavaName + "-property",
                    String.format(
                            "Class that encapsulates the data of the '%s' property in the {@link %s} component.",
                            property.getName(), javaClass.getName()));

            MethodSource<JavaClassSource> method = javaClass.addMethod()
                    .setPublic().setReturnType(nestedClass);
            method.setName(ComponentGeneratorUtils
                    .generateMethodNameForProperty("get", propertyJavaName));
            method.setBody(String.format(
                    "return new %s().readJson((JsonObject) getElement().getPropertyRaw(\"%s\"));",
                    nestedClass.getName(), property.getName()));

            addSynchronizeAnnotationAndJavadocToGetter(method, property,
                    events);

            if ("value".equals(propertyJavaName)
                    && shouldImplementHasValue(metadata)) {
                javaClass.addInterface(HasValue.class.getName() + "<"
                        + GENERIC_TYPE + ", " + nestedClass.getName() + ">");
                method.addAnnotation(Override.class);

                method.setBody(String.format(
                        "JsonObject _obj = (JsonObject) getElement().getPropertyRaw(\"%s\");"
                                + "return _obj == null ? getEmptyValue() : new %s().readJson(_obj);",
                        property.getName(), nestedClass.getName()));
            } else {
                method.setBody(String.format(
                        "return new %s().readJson((JsonObject) getElement().getPropertyRaw(\"%s\"));",
                        nestedClass.getName(), property.getName()));
            }

        } else {
            boolean postfixWithVariableType = property.getType().size() > 1;
            if (postfixWithVariableType) {
                property.setType(new TreeSet<>(property.getType()));
            }
            for (ComponentBasicType basicType : property.getType()) {
                Class<?> javaType = ComponentGeneratorUtils
                        .toJavaType(basicType);
                MethodSource<JavaClassSource> method = javaClass.addMethod()
                        .setReturnType(javaType);

                setMethodVisibility(method, basicType);

                if (basicType == ComponentBasicType.BOOLEAN) {
                    if (!propertyJavaName.startsWith("is")
                            && !propertyJavaName.startsWith("has")
                            && !propertyJavaName.startsWith("have")) {

                        method.setName(ComponentGeneratorUtils
                                .generateMethodNameForProperty("is",
                                        propertyJavaName));
                    } else {
                        method.setName(ComponentGeneratorUtils
                                .formatStringToValidJavaIdentifier(
                                        propertyJavaName));
                    }
                } else {
                    method.setName(ComponentGeneratorUtils
                            .generateMethodNameForProperty("get",
                                    propertyJavaName)
                            + (postfixWithVariableType
                                    ? StringUtils.capitalize(
                                            basicType.name().toLowerCase())
                                    : ""));
                }

                if (method.getVisibility() == Visibility.PROTECTED) {
                    method.setName("protected"
                            + StringUtils.capitalize(method.getName()));
                }


                addSynchronizeAnnotationAndJavadocToGetter(method, property,
                        events);

                if ("value".equals(propertyJavaName)
                        && shouldImplementHasValue(metadata)) {
                    method.setName("getValue");
                    if (javaType.isPrimitive()) {
                        javaType = ClassUtils.primitiveToWrapper(javaType);
                        method.setReturnType(javaType);
                    }
                    javaClass.addInterface(HasValue.class.getName() + "<"
                            + GENERIC_TYPE + ", " + javaType.getName() + ">");
                    javaClass.removeImport(ComponentSupplier.class);
                    javaClass.removeInterface(ComponentSupplier.class);
                    method.addAnnotation(Override.class);

                    method.setBody(ComponentGeneratorUtils
                            .generateElementApiValueGetterForType(
                            basicType, property.getName()));

                    addGetEmptyValueIfString(javaClass, javaType);
                } else {
                    method.setBody(ComponentGeneratorUtils
                            .generateElementApiGetterForType(basicType,
                                    property.getName()));
                }
            }
        }
    }

    private void addGetEmptyValueIfString(JavaClassSource javaClass,
            Class<?> javaType) {
        if (javaType == String.class) {
            MethodSource<JavaClassSource> method = javaClass.addMethod()
                    .setPublic().setReturnType(String.class.getSimpleName());
            method.setName("getEmptyValue");
            method.setBody("return \"\";");
            method.addAnnotation(Override.class);
        }
    }

    /**
     * Sets the method visibility, taking account whether is the type is
     * supported or not by the Java API.
     *
     * @param method
     *            the method which visibility should be set
     * @param type
     *            the type of objects used by in the method signature
     * @see #isSupportedObjectType(ComponentType)
     */
    private void setMethodVisibility(MethodSource<JavaClassSource> method,
            ComponentType type) {
        setMethodVisibility(method, Collections.singleton(type));
    }

    /**
     * Sets the method visibility, taking account whether is the types are
     * supported or not by the Java API.
     *
     * @param method
     *            the method which visibility should be set
     * @param types
     *            the types of objects used by in the method signature
     * @see #isSupportedObjectType(ComponentType)
     */
    private void setMethodVisibility(MethodSource<JavaClassSource> method,
            Collection<? extends ComponentType> types) {

        if (types.stream().allMatch(this::isSupportedObjectType)) {
            method.setPublic();
        } else {
            method.setProtected();
        }
    }

    /**
     * Gets whether the type is undefined in Java terms. Methods with undefined
     * returns or parameters are created as protected.
     */
    private boolean isSupportedObjectType(ComponentType type) {
        if (!type.isBasicType()) {
            return true;
        }

        ComponentBasicType basicType = (ComponentBasicType) type;

        switch (basicType) {
        case NUMBER:
        case STRING:
        case BOOLEAN:
        case DATE:
            return true;
        }

        return false;
    }

    private void addSynchronizeAnnotationAndJavadocToGetter(
            MethodSource<JavaClassSource> method,
            ComponentPropertyData property, List<ComponentEventData> events) {
        // verifies whether the getter needs a @Synchronize annotation by
        // inspecting the event list
        String synchronizationDescription = "";

        if (containsChangedEventForProperty(property.getName(), events)) {
            method.addAnnotation(Synchronize.class)
                    .setStringValue("property", property.getName())
                    .setStringValue(property.getName() + "-changed");

            synchronizationDescription = "This property is synchronized automatically from client side when a '"
                    + property.getName() + "-changed' event happens.";
        } else {
            synchronizationDescription = "This property is not synchronized automatically from the client side, so the returned value may not be the same as in client side.";
        }

        if (StringUtils.isNotEmpty(property.getDescription())) {
            addMarkdownJavaDoc(property.getDescription() + "<p>"
                    + synchronizationDescription, method.getJavaDoc());
        } else {
            method.getJavaDoc().setFullText(synchronizationDescription);
        }

        method.getJavaDoc().addTagValue(JAVADOC_RETURN, "the {@code "
                + property.getName() + "} property from the webcomponent");
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

    /**
     * Verifies whether a component should implement the {@link HasValue}
     * interface.
     * <p>
     * To be able to implement the interface, the component must have a
     * non-read-only property called "value", and publish "value-changed"
     * events.
     * <p>
     * The "value" also cannot be multi-typed.
     */
    private boolean shouldImplementHasValue(ComponentMetadata metadata) {
        if (metadata.getProperties() == null || metadata.getEvents() == null) {
            return false;
        }

        if (metadata.getProperties().stream()
                .anyMatch(property -> "value"
                        .equals(getJavaNameForProperty(metadata,
                                property.getName()))
                        && !property.isReadOnly()
                        && (containsObjectType(property)
                                || property.getType().size() == 1))) {

            return metadata.getEvents().stream()
                    .anyMatch(event -> "value-changed"
                            .equals(getJavaNameForPropertyChangeEvent(metadata,
                                    event.getName())));
        }
        return false;
    }

    private void addMarkdownJavaDoc(String documentation,
            JavaDocSource<?> javaDoc) {
        javaDoc.setFullText(javaDocFormatter.formatJavaDoc(documentation));
    }

    private void generateSetterFor(JavaClassSource javaClass,
            ComponentMetadata metadata, ComponentPropertyData property) {

        String propertyJavaName = getJavaNameForProperty(metadata,
                property.getName());

        if (containsObjectType(property)) {
            // the getter already created the nested pojo, so here we just need
            // to get the name
            String nestedClassName = ComponentGeneratorUtils
                    .generateValidJavaClassName(propertyJavaName + "-property");

            MethodSource<JavaClassSource> method = javaClass.addMethod()
                    .setName(ComponentGeneratorUtils
                            .generateMethodNameForProperty("set",
                                    propertyJavaName))
                    .setPublic();

            method.addParameter(nestedClassName, "property");

            method.setBody(String.format(
                    "getElement().setPropertyJson(\"%s\", property.toJson());",
                    property.getName()));

            if (StringUtils.isNotEmpty(property.getDescription())) {
                addMarkdownJavaDoc(property.getDescription(),
                        method.getJavaDoc());
            }

            method.getJavaDoc().addTagValue(JAVADOC_PARAM,
                    "property the property to set");

            if (fluentSetters) {
                addFluentReturnToMethod(method);
            }

            if ("value".equals(propertyJavaName)
                    && shouldImplementHasValue(metadata)) {
                method.addAnnotation(Override.class);
                preventSettingTheSameValue(javaClass, "property", method);
            }

        } else {

            for (ComponentBasicType basicType : property.getType()) {
                MethodSource<JavaClassSource> method = javaClass.addMethod()
                        .setName(ComponentGeneratorUtils
                                .generateMethodNameForProperty("set",
                                        propertyJavaName));

                setMethodVisibility(method, basicType);

                Class<?> setterType = ComponentGeneratorUtils
                        .toJavaType(basicType);

                String parameterName = ComponentGeneratorUtils
                        .formatStringToValidJavaIdentifier(propertyJavaName);
                ComponentGeneratorUtils.addMethodParameter(javaClass, method,
                        setterType, parameterName);

                boolean nullable = !"value".equals(propertyJavaName)
                        || !shouldImplementHasValue(metadata)
                        || String.class != setterType;

                method.setBody(
                        ComponentGeneratorUtils.generateElementApiSetterForType(
                                basicType, property.getName(), parameterName, nullable));

                if (StringUtils.isNotEmpty(property.getDescription())) {
                    addMarkdownJavaDoc(property.getDescription(),
                            method.getJavaDoc());
                }

                method.getJavaDoc().addTagValue(JAVADOC_PARAM,
                        String.format("%s the %s value to set", parameterName,
                                setterType.getSimpleName()));

                if (fluentSetters) {
                    addFluentReturnToMethod(method);
                }
                if ("value".equals(propertyJavaName)
                        && shouldImplementHasValue(metadata)) {

                    method.addAnnotation(Override.class);
                    preventSettingTheSameValue(javaClass, parameterName,
                            method);
                    if (setterType.isPrimitive()) {
                        implementHasValueSetterWithPimitiveType(javaClass,
                                property, method, setterType, parameterName);
                    } else if (!nullable) {
                        method.setBody(String.format(
                                "Objects.requireNonNull(%s, \"%s cannot be null\");",
                                parameterName, parameterName)
                                + method.getBody());
                    }
                }
            }
        }
    }

    /**
     * HasValue interface use a generic type for the value, and generics can't
     * be used with primitive types. This method converts any boolean or double
     * parameters to {@link Boolean} and {@link Double} respectively.
     * <p>
     * Note that for double, an overload setter with {@link Number} is also
     * created, to allow the developer to call the setValue method using int.
     */
    private void implementHasValueSetterWithPimitiveType(
            JavaClassSource javaClass, ComponentPropertyData property,
            MethodSource<JavaClassSource> method, Class<?> setterType,
            String parameterName) {
        method.removeParameter(setterType, parameterName);
        setterType = ClassUtils.primitiveToWrapper(setterType);
        ComponentGeneratorUtils.addMethodParameter(javaClass, method,
                setterType, parameterName);
        preventNullArgument(javaClass, parameterName, method);

        if (setterType.equals(Double.class)) {
            MethodSource<JavaClassSource> overloadMethod = javaClass.addMethod()
                    .setName(method.getName()).setPublic();
            ComponentGeneratorUtils.addMethodParameter(javaClass,
                    overloadMethod, Number.class, parameterName);
            overloadMethod.setBody(String.format("setValue(%s.doubleValue());",
                    parameterName));

            if (StringUtils.isNotEmpty(property.getDescription())) {
                addMarkdownJavaDoc(property.getDescription(),
                        overloadMethod.getJavaDoc());
            }

            overloadMethod.getJavaDoc().addTagValue(JAVADOC_PARAM,
                    String.format("%s the %s value to set", parameterName,
                            Number.class.getSimpleName()));
            overloadMethod.getJavaDoc().addTagValue(JAVADOC_SEE,
                    "#setValue(Double)");

            preventSettingTheSameValue(javaClass, parameterName,
                    overloadMethod);
            preventNullArgument(javaClass, parameterName, overloadMethod);

            if (fluentSetters) {
                addFluentReturnToMethod(overloadMethod);
            }
        }
    }

    private void preventSettingTheSameValue(JavaClassSource javaClass,
            String parameterName, MethodSource<JavaClassSource> method) {
        javaClass.addImport(Objects.class);
        method.setBody(String.format("if (!Objects.equals(%s, getValue())) {",
                parameterName) + method.getBody() + "}");
    }

    private void preventNullArgument(JavaClassSource javaClass,
            String parameterName, MethodSource<JavaClassSource> method) {
        javaClass.addImport(Objects.class);
        method.setBody(String.format("Objects.requireNonNull(%s, \"%s\");",
                parameterName, javaClass.getName() + " value must not be null")
                + method.getBody());
    }

    private void addFluentReturnToMethod(MethodSource<JavaClassSource> method) {
        method.setReturnType(GENERIC_TYPE);
        method.setBody(method.getBody() + "return get();");
        method.getJavaDoc().addTagValue(JAVADOC_RETURN,
                "this instance, for method chaining");
    }

    private void generateMethodFor(JavaClassSource javaClass,
            ComponentFunctionData function) {
        Set<List<ComponentType>> typeVariants = FunctionParameterVariantCombinator
                .generateVariants(function);
        Map<ComponentObjectType, JavaClassSource> nestedClassesMap = new HashMap<>();
        for (List<ComponentType> typeVariant : typeVariants) {
            MethodSource<JavaClassSource> method = javaClass.addMethod()
                    .setName(StringUtils.uncapitalize(ComponentGeneratorUtils
                            .formatStringToValidJavaIdentifier(
                                    function.getName())))
                    .setReturnTypeVoid();

            if (StringUtils.isNotEmpty(function.getDescription())) {
                addMarkdownJavaDoc(function.getDescription(),
                        method.getJavaDoc());
            }

            String parameterString = generateMethodParameters(javaClass, method,
                    function, typeVariant, nestedClassesMap);

            // methods with return values are currently not supported
            if (function.getReturns() != null
                    && function.getReturns() != ComponentBasicType.UNDEFINED) {
                method.setProtected();
                method.addAnnotation(NotSupported.class);

                method.getJavaDoc().setText(method.getJavaDoc().getText()
                        + "<p>This function is not supported by Flow because it returns a <code>"
                        + ComponentGeneratorUtils
                                .toJavaType(function.getReturns()).getName()
                        + "</code>. Functions with return types different than void are not supported at this moment.");
                method.setBody("");
            } else {
                setMethodVisibility(method, typeVariant);

                method.setBody(
                        String.format("getElement().callFunction(\"%s\"%s);",
                                function.getName(), parameterString));
            }
        }
    }

    /**
     * Adds the parameters and javadocs to the given method and generates nested
     * classes for complex object parameters if needed.
     *
     * @param javaClass
     *            the main class file
     * @param method
     *            the method to add parameters to
     * @param function
     *            the function data
     * @param typeVariant
     *            the list of types to use for each added parameter
     * @param nestedClassesMap
     *            map for memorizing already generated nested classes
     * @return a string of the parameters of the function, or an empty string if
     *         no parameters
     */
    private String generateMethodParameters(JavaClassSource javaClass,
            MethodSource<JavaClassSource> method,
            ComponentFunctionData function, List<ComponentType> typeVariant,
            Map<ComponentObjectType, JavaClassSource> nestedClassesMap) {
        int paramIndex = 0;
        StringBuilder sb = new StringBuilder();
        for (ComponentType paramType : typeVariant) {
            String paramName = function.getParameters().get(paramIndex)
                    .getName();
            String paramDescription = function.getParameters().get(paramIndex)
                    .getDescription();
            String formattedName = StringUtils.uncapitalize(
                    ComponentGeneratorUtils.formatStringToValidJavaIdentifier(
                            function.getParameters().get(paramIndex)
                                    .getName()));
            paramIndex++;

            if (paramType.isBasicType()) {
                ComponentBasicType bt = (ComponentBasicType) paramType;
                ComponentGeneratorUtils.addMethodParameter(javaClass, method,
                        ComponentGeneratorUtils.toJavaType(bt), formattedName);
                sb.append(", ").append(formattedName);
            } else {
                ComponentObjectType ot = (ComponentObjectType) paramType;
                String nameHint = function.getName() + "-" + paramName;
                JavaClassSource nestedClass = nestedClassesMap.computeIfAbsent(
                        ot,
                        objectType -> generateNestedPojo(javaClass, objectType,
                                nameHint,
                                String.format(
                                        "Class that encapsulates the data to be sent to the {@link %s#%s(%s)} method.",
                                        javaClass.getName(), method.getName(),
                                        ComponentGeneratorUtils
                                                .generateValidJavaClassName(
                                                        nameHint))));
                sb.append(", ").append(formattedName).append(".toJson()");
                method.getJavaDoc().addTagValue(JAVADOC_SEE,
                        nestedClass.getName());
                method.addParameter(nestedClass, formattedName);
            }

            method.getJavaDoc().addTagValue(JAVADOC_PARAM,
                    String.format("%s %s", formattedName, paramDescription));
        }
        return sb.toString();
    }

    private void generateEventListenerFor(JavaClassSource javaClass,
            ComponentMetadata metadata, ComponentEventData event) {
        String eventJavaApiName = getJavaNameForPropertyChangeEvent(metadata,
                event.getName());

        // verify whether the HasValue interface is implemented.
        if ("value-changed".equals(eventJavaApiName)
                && shouldImplementHasValue(metadata)) {
            if (!eventJavaApiName.equals(event.getName())) {
                overrideHasValueGetClientValuePropertyName(javaClass, event
                        .getName().replace(PROPERTY_CHANGE_EVENT_POSTFIX, ""));
            }
            return;
        }

        eventJavaApiName = ComponentGeneratorUtils
                .formatStringToValidJavaIdentifier(eventJavaApiName);

        if (eventJavaApiName.endsWith("Changed")) {
            // removes the "d" in the end, to create addSomethingChangeListener
            // and SomethingChangeEvent
            eventJavaApiName = eventJavaApiName.substring(0,
                    eventJavaApiName.length() - 1);
        }

        JavaClassSource eventListener = createEventListenerEventClass(javaClass,
                event, eventJavaApiName);

        javaClass.addNestedType(eventListener);
        MethodSource<JavaClassSource> method = javaClass.addMethod()
                .setName("add"
                        + StringUtils.capitalize(eventJavaApiName + "Listener"))
                .setPublic().setReturnType(Registration.class);
        method.addParameter("ComponentEventListener<" + eventListener.getName()
                + GENERIC_TYPE_DECLARATION + ">", "listener");

        method.getJavaDoc().setText(String.format(
                "Adds a listener for {@code %s} events fired by the webcomponent.",
                event.getName()))
                .addTagValue(JAVADOC_PARAM, "listener the listener")
                .addTagValue(JAVADOC_RETURN,
                        "a {@link Registration} for removing the event listener");

        method.addAnnotation(SuppressWarnings.class)
                .setStringArrayValue(new String[] { "rawtypes", "unchecked" });

        method.setBody(String.format(
                "return addListener(%s.class, (ComponentEventListener) listener);",
                eventListener.getName()));
    }

    private void overrideHasValueGetClientValuePropertyName(
            JavaClassSource javaClass, String propertyName) {
        MethodSource<JavaClassSource> method = javaClass.addMethod()
                .setName("getClientValuePropertyName").setPublic()
                .setReturnType(String.class);
        method.addAnnotation(Override.class);
        method.setBody(String.format("return \"%s\";", propertyName));
    }

    private JavaClassSource createEventListenerEventClass(
            JavaClassSource javaClass, ComponentEventData event,
            String javaEventName) {
        String eventClassName = StringUtils.capitalize(javaEventName);
        String eventListenerString = String.format(
                "public static class %sEvent<%s extends %s<%s>> extends ComponentEvent<%s> {}",
                eventClassName, GENERIC_TYPE, javaClass.getName(), GENERIC_TYPE,
                GENERIC_TYPE);

        JavaClassSource eventListener = Roaster.parse(JavaClassSource.class,
                eventListenerString);

        MethodSource<JavaClassSource> eventConstructor = eventListener
                .addMethod().setConstructor(true).setPublic()
                .setBody("super(source, fromClient);");
        eventConstructor.addParameter(GENERIC_TYPE, "source");
        eventConstructor.addParameter("boolean", "fromClient");

        for (ComponentPropertyBaseData property : event.getProperties()) {
            // Add new parameter to constructor
            final String propertyName = property.getName();
            String normalizedProperty = ComponentGeneratorUtils
                    .formatStringToValidJavaIdentifier(propertyName);
            Class<?> propertyJavaType;

            if (containsObjectType(property)) {
                JavaClassSource nestedClass = generateNestedPojo(javaClass,
                        property.getObjectType().get(0),
                        eventClassName + "-" + propertyName,
                        String.format(
                                "Class that encapsulates the data received on the '%s' property of @{link %s} events, from the @{link %s} component.",
                                propertyName, eventListener.getName(),
                                javaClass.getName()));

                propertyJavaType = JsonObject.class;

                eventListener.addField().setType(propertyJavaType).setPrivate()
                        .setFinal(true).setName(normalizedProperty);

                eventListener.addMethod().setName(ComponentGeneratorUtils
                        .generateMethodNameForProperty("get", propertyName))
                        .setPublic().setReturnType(nestedClass)
                        .setBody(String.format("return new %s().readJson(%s);",
                                nestedClass.getName(), normalizedProperty));
            } else {
                if (!property.getType().isEmpty()) {
                    // for varying types, using the first type declared in the
                    // JSDoc
                    // it is anyway very rare to have varying property type
                    propertyJavaType = ComponentGeneratorUtils
                            .toJavaType(property.getType().iterator().next());
                } else { // object property
                    propertyJavaType = JsonObject.class;
                }

                // Create private field
                eventListener.addProperty(propertyJavaType, normalizedProperty)
                        .setAccessible(true).setMutable(false);
            }

            ParameterSource<JavaClassSource> parameter = ComponentGeneratorUtils
                    .addMethodParameter(javaClass, eventConstructor,
                            propertyJavaType,
                            normalizedProperty);
            parameter.addAnnotation(EventData.class)
                    .setStringValue(String.format("event.%s", propertyName));

            // Set value to private field
            eventConstructor.setBody(String.format("%s%nthis.%s = %s;",
                    eventConstructor.getBody(), normalizedProperty,
                    normalizedProperty));
            // Add the EventData as a import
            javaClass.addImport(EventData.class);
        }

        eventListener.addAnnotation(DomEvent.class)
                .setStringValue(event.getName());

        // Add event imports.
        javaClass.addImport(DomEvent.class);
        javaClass.addImport(ComponentEvent.class);
        javaClass.addImport(ComponentEventListener.class);

        return eventListener;
    }

    private boolean containsObjectType(ComponentPropertyBaseData property) {
        return property.getObjectType() != null
                && !property.getObjectType().isEmpty();
    }

    private JavaClassSource generateNestedPojo(JavaClassSource javaClass,
            ComponentObjectType type, String nameHint, String description) {
        JavaClassSource nestedClass = new NestedClassGenerator().withType(type)
                .withFluentSetters(fluentSetters).withNameHint(nameHint)
                .build();

        if (javaClass.getNestedType(nestedClass.getName()) != null) {
            throw new ComponentGenerationException("Duplicated nested class: \""
                    + nestedClass.getName()
                    + "\". Please make sure your webcomponent definition contains unique properties, events and method names.");
        }

        nestedClass.getJavaDoc().setText(description);
        javaClass.addNestedType(nestedClass);
        javaClass.addImport(JsonObject.class);
        javaClass.addImport(JsonSerializable.class);
        return nestedClass;
    }

    private Properties getProperties(String fileName) {
        Properties config = new Properties();

        // Get properties resource with version information.
        try (InputStream resourceAsStream = this.getClass()
                .getResourceAsStream("/" + fileName)) {
            config.load(resourceAsStream);
            return config;
        } catch (IOException e) {
            LoggerFactory.getLogger(getClass().getSimpleName()).warn(
                    "Failed to load properties file '{}'", fileName, e);
        }

        return config;
    }

    private String getJavaNameForProperty(ComponentMetadata metadata,
            String propertyName) {
        String javaApiName = propertyName;
        Optional<String> nameRemapping = PropertyNameRemapRegistry
                .getOptionalMappingFor(metadata.getTag(), propertyName);
        if (nameRemapping.isPresent()) {
            javaApiName = nameRemapping.get();
        }
        return javaApiName;
    }

    private String getJavaNameForPropertyChangeEvent(ComponentMetadata metadata,
            String propertyChangeEventName) {
        if (!propertyChangeEventName.endsWith(PROPERTY_CHANGE_EVENT_POSTFIX)) {
            // not a property change event, just pass original value through
            return propertyChangeEventName;
        }
        return getJavaNameForProperty(metadata, propertyChangeEventName
                .replace(PROPERTY_CHANGE_EVENT_POSTFIX, ""))
                + PROPERTY_CHANGE_EVENT_POSTFIX;
    }

}
