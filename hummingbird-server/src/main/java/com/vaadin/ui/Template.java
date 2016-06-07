/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.ui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.stream.Stream;

import com.vaadin.annotations.AnnotationReader;
import com.vaadin.annotations.HtmlTemplate;
import com.vaadin.annotations.Id;
import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.impl.TemplateElementStateProvider;
import com.vaadin.hummingbird.nodefeature.TemplateMap;
import com.vaadin.hummingbird.router.HasChildView;
import com.vaadin.hummingbird.router.RouterConfiguration;
import com.vaadin.hummingbird.router.View;
import com.vaadin.hummingbird.template.RelativeFileResolver;
import com.vaadin.hummingbird.template.TemplateNode;
import com.vaadin.hummingbird.template.TemplateParseException;
import com.vaadin.hummingbird.template.model.TemplateModel;
import com.vaadin.hummingbird.template.model.TemplateModelProxyHandler;
import com.vaadin.hummingbird.template.model.TemplateModelTypeParser;
import com.vaadin.hummingbird.template.parser.TemplateParser;
import com.vaadin.hummingbird.template.parser.TemplateResolver;
import com.vaadin.util.ReflectTools;

/**
 * Component for declaratively defined element structures. The structure of a
 * template is loaded from an .html file on the classpath.
 * <p>
 * There are two options to specify the HTML file:
 * <ul>
 * <li>By default the HTML file should be in the same package of the class, and
 * the name should be the same as the class name, but with the
 * <code>.html</code> file extension.
 * <li>Annotate your subclass with {@link HtmlTemplate} annotation with the HTML
 * file path as a value. The path can be either relative (without leading "/")
 * or absolute. In the first case the path is considered as a relative for the
 * class package.
 * </ul>
 * <p>
 * A template can be used as a {@link HasChildView} in
 * {@link RouterConfiguration} if the template file contains a
 * <code>@child@</code> slot.
 *
 * @see HtmlTemplate
 *
 * @author Vaadin Ltd
 */
public abstract class Template extends Component implements HasChildView {
    private final StateNode stateNode = TemplateElementStateProvider
            .createRootNode();

    private TemplateModel model;

    /**
     * Creates a new template.
     */
    public Template() {
        // Will set element later
        super(null);

        HtmlTemplate annotation = getClass().getAnnotation(HtmlTemplate.class);
        if (annotation == null) {
            setTemplateElement(getClass().getSimpleName() + ".html");
        } else {
            setTemplateElement(annotation.value());
        }

    }

    /**
     * Creates a new template using {@code templateFileName} as the template
     * file name.
     *
     * @param templateFileName
     *            the template file name, not {@code null}
     */
    protected Template(String templateFileName) {
        // Will set element later
        super(null);

        setTemplateElement(templateFileName);
    }

    /**
     * Creates a new template using {@code inputStream} as a template content.
     *
     * @param inputStream
     *            the HTML snippet input stream
     *
     */
    protected Template(InputStream inputStream) {
        // Will set element later
        super(null);

        // No support for @include@ when using this constructor right now
        setTemplateElement(inputStream, relativeFilename -> {
            throw new IOException("No template resolver defined");
        });
    }

    private void setTemplateElement(String templateFileNameAndPath) {
        if (templateFileNameAndPath == null) {
            throw new IllegalArgumentException(
                    "HTML template file name cannot be null");
        }
        RelativeFileResolver templateResolver = new RelativeFileResolver(
                getClass(), templateFileNameAndPath);

        String templateFileName = new File(templateFileNameAndPath).getName();
        InputStream templateContentStream = templateResolver
                .resolve(templateFileName);
        setTemplateElement(templateContentStream, templateResolver);
    }

    private void setTemplateElement(InputStream inputStream,
            TemplateResolver templateResolver) {
        try (InputStream templateContentStream = inputStream) {

            TemplateNode templateRoot = TemplateParser
                    .parse(templateContentStream, templateResolver);

            stateNode.getFeature(TemplateMap.class)
                    .setRootTemplate(templateRoot);

            Element rootElement = Element.get(stateNode);

            setElement(this, rootElement);
        } catch (IOException e) {
            throw new TemplateParseException("Error reading template", e);
        }

        mapComponents(getClass());
    }

    private void mapComponents(Class<?> cls) {
        if (cls.getSuperclass() != Template.class) {
            // Parent fields
            mapComponents(cls.getSuperclass());
        }

        Stream<Field> annotatedComponentFields = Stream
                .of(cls.getDeclaredFields())
                .filter(field -> !field.isSynthetic());

        annotatedComponentFields.forEach(this::maybeMapComponentField);
    }

    private void maybeMapComponentField(Field field) {
        Optional<Id> idAnnotation = AnnotationReader.getAnnotationFor(field,
                Id.class);
        if (!idAnnotation.isPresent()) {
            return;
        }
        String id = idAnnotation.get().value();

        if (!Component.class.isAssignableFrom(field.getType())) {
            throw new IllegalArgumentException("The field '" + field.getName()
                    + "' in " + getClass().getName() + " has an @"
                    + Id.class.getSimpleName()
                    + " annotation but the field type '"
                    + field.getType().getName() + "' does not extend "
                    + Component.class.getSimpleName());
        }

        String fieldName = field.getName();
        @SuppressWarnings("unchecked")
        Class<? extends Component> componentType = (Class<? extends Component>) field
                .getType();

        Optional<Element> element = getElementById(id);
        if (!element.isPresent()) {
            throw new IllegalArgumentException("No element with id '" + id
                    + "' found while binding field '" + fieldName + "' in "
                    + getClass().getName());
        }

        if (element.get().equals(getElement())) {
            throw new IllegalArgumentException(
                    "Cannot map the root element of the template. This is always mapped to the template instance itself ("
                            + getClass().getName() + ")");
        }
        Component c = Component.from(element.get(), componentType);
        ReflectTools.setJavaFieldValue(this, field, c);
    }

    /**
     * Finds an element with the given id inside this template.
     *
     * @param id
     *            the id to look for
     * @return an optional element with the id, or an empty Optional if no
     *         element with the given id was found
     */
    private Optional<Element> getElementById(String id) {
        return stateNode.getFeature(TemplateMap.class).getRootTemplate()
                .findElement(stateNode, id);
    }

    @Override
    public void setChildView(View childView) {
        TemplateMap templateMap = stateNode.getFeature(TemplateMap.class);
        if (childView == null) {
            templateMap.setChild(null);
        } else {
            templateMap.setChild(childView.getElement().getNode());
        }
    }

    /**
     * Returns the {@link TemplateModel model} of this template.
     * <p>
     * The type of the model will be the type that this method returns in the
     * instance it is invoked on - meaning that you should override this method
     * and return your own model type that extends {@link TemplateModel}.
     *
     * @return the model of this template
     * @see TemplateModel
     */
    protected TemplateModel getModel() {
        if (model == null) {
            model = createTemplateModelInstance();
        }
        return model;
    }

    private TemplateModel createTemplateModelInstance() {
        Class<? extends TemplateModel> modelType = getModelType();

        return TemplateModelProxyHandler.createModelProxy(stateNode, modelType);
    }

    /**
     * Gets the type of the template model to use with with this template.
     *
     * @return the model type, not <code>null</code>
     */
    protected Class<? extends TemplateModel> getModelType() {
        return TemplateModelTypeParser.getType(getClass());
    }
}
