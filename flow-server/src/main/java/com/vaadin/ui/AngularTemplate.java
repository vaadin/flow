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
package com.vaadin.ui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.stream.Stream;

import com.vaadin.flow.StateNode;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.impl.TemplateElementStateProvider;
import com.vaadin.flow.nodefeature.TemplateMap;
import com.vaadin.flow.polymertemplate.AbstractTemplate;
import com.vaadin.flow.polymertemplate.Id;
import com.vaadin.flow.router.legacy.HasChildView;
import com.vaadin.flow.router.legacy.RouterConfiguration;
import com.vaadin.flow.router.legacy.View;
import com.vaadin.flow.template.angular.HtmlTemplate;
import com.vaadin.flow.template.angular.RelativeFileResolver;
import com.vaadin.flow.template.angular.TemplateNode;
import com.vaadin.flow.template.angular.TemplateParseException;
import com.vaadin.flow.template.angular.model.ModelDescriptor;
import com.vaadin.flow.template.angular.model.TemplateModel;
import com.vaadin.flow.template.angular.model.TemplateModelProxyHandler;
import com.vaadin.flow.template.angular.model.TemplateModelTypeParser;
import com.vaadin.flow.template.angular.parser.TemplateParser;
import com.vaadin.flow.template.angular.parser.TemplateResolver;
import com.vaadin.flow.util.AnnotationReader;
import com.vaadin.flow.util.ReflectTools;
import com.vaadin.ui.event.AttachEvent;

/**
 * Component for dec .AttachEvent; import com.vaadin.flow.telaratively defined
 * element structures. The structure of a template is loaded from an .html file
 * on the classpath.
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
 * @deprecated do not use! feature is to be removed in the near future
 */
@Deprecated
public abstract class AngularTemplate extends AbstractTemplate<TemplateModel>
        implements HasChildView {

    private transient TemplateModel model;

    /**
     * Creates a new template.
     */
    public AngularTemplate() {
        // Will set element later
        super(createTemplateStateNode());

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
    public AngularTemplate(String templateFileName) {
        // Will set element later
        super(createTemplateStateNode());

        setTemplateElement(templateFileName);
    }

    /**
     * Creates a new template using {@code inputStream} as a template content.
     *
     * @param inputStream
     *            the HTML snippet input stream
     *
     */
    public AngularTemplate(InputStream inputStream) {
        // Will set element later
        super(createTemplateStateNode());

        // No support for @include@ when using this constructor right now
        setTemplateElement(inputStream, relativeFilename -> {
            throw new IOException("No template resolver defined");
        });
    }

    @Override
    public void setChildView(View childView) {
        TemplateMap templateMap = getStateNode().getFeature(TemplateMap.class);
        if (childView == null) {
            templateMap.setChild(null);
        } else {
            templateMap.setChild(childView.getElement().getNode());
        }
    }

    private static StateNode createTemplateStateNode() {
        return TemplateElementStateProvider.createRootNode();
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
            setTemplateRoot(templateRoot);
        } catch (IOException e) {
            throw new TemplateParseException("Error reading template", e);
        }

        mapComponents(getClass());
    }

    private void mapComponents(Class<?> cls) {
        if (cls.getSuperclass() != AbstractTemplate.class) {
            // Parent fields
            mapComponents(cls.getSuperclass());
        }

        Stream<Field> annotatedComponentFields = Stream
                .of(cls.getDeclaredFields())
                .filter(field -> !field.isSynthetic());

        annotatedComponentFields.forEach(this::tryMapComponentOrElement);
    }

    @SuppressWarnings("unchecked")
    private void tryMapComponentOrElement(Field field) {
        Optional<Id> idAnnotation = AnnotationReader.getAnnotationFor(field,
                Id.class);
        if (!idAnnotation.isPresent()) {
            return;
        }
        String id = idAnnotation.get().value();

        Class<?> fieldType = field.getType();
        String fieldName = field.getName();
        Element element = getElementById(id)
                .orElseThrow(() -> new IllegalArgumentException(String.format(
                        "No element with id '%s' found while binding field '%s' in '%s'",
                        id, fieldName, getClass().getName())));
        if (element.equals(getElement())) {
            throw new IllegalArgumentException(
                    "Cannot map the root element of the template. "
                            + "This is always mapped to the template instance itself ("
                            + getClass().getName() + ")");
        }

        if (Component.class.isAssignableFrom(fieldType)) {
            Class<? extends Component> componentType = (Class<? extends Component>) fieldType;
            Component c = Component.from(element, componentType);
            ReflectTools.setJavaFieldValue(this, field, c);
        } else if (Element.class.isAssignableFrom(fieldType)) {
            ReflectTools.setJavaFieldValue(this, field, element);
        } else {
            throw new IllegalArgumentException(String.format(
                    "The field '%s' in '%s' has an @'%s' "
                            + "annotation but the field type '%s' "
                            + "does not extend neither '%s' nor '%s'",
                    fieldName, getClass().getName(), Id.class.getSimpleName(),
                    fieldType.getName(), Component.class.getSimpleName(),
                    Element.class.getSimpleName()));
        }
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
        return getStateNode().getFeature(TemplateMap.class).getRootTemplate()
                .findElement(getStateNode(), id);
    }

    /**
     * Sets root of the template.
     *
     * @param templateRoot
     *            template root to set
     */
    private void setTemplateRoot(TemplateNode templateRoot) {
        getStateNode().getFeature(TemplateMap.class)
                .setRootTemplate(templateRoot);
        Element rootElement = Element.get(getStateNode());
        setElement(this, rootElement);
    }

    @Override
    protected Class<? extends TemplateModel> getModelType() {
        return TemplateModelTypeParser.getType(getClass());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        // initialize the model so that all properties are available in the
        // underlying node's ModelMap
        getModel();
    }

    @Override
    protected TemplateModel getModel() {
        if (model == null) {
            model = createTemplateModelInstance();
        }
        return model;
    }

    private TemplateModel createTemplateModelInstance() {
        ModelDescriptor<? extends TemplateModel> descriptor = ModelDescriptor
                .get(getModelType());
        updateModelDescriptor(descriptor);
        return TemplateModelProxyHandler.createModelProxy(getStateNode(),
                descriptor);
    }

    protected void updateModelDescriptor(
            ModelDescriptor<? extends TemplateModel> currentDescriptor) {
        ModelDescriptor<?> oldDescriptor = getStateNode()
                .getFeature(TemplateMap.class).getModelDescriptor();
        if (oldDescriptor == null) {
            getStateNode().getFeature(TemplateMap.class)
                    .setModelDescriptor(currentDescriptor);
        } else {
            /*
             * Can have an existing descriptor if createTemplateModelInstance
             * has been run previously but the transient model field has been
             * cleared. Let's just verify that we're still seeing the same
             * definition.
             */
            assert oldDescriptor.toJson().toJson()
                    .equals(currentDescriptor.toJson().toJson());
        }
    }
}
