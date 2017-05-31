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
package com.vaadin.flow.template;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.annotations.AnnotationReader;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Id;
import com.vaadin.annotations.Tag;
import com.vaadin.flow.StateNode;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ShadowRoot;
import com.vaadin.flow.dom.impl.BasicElementStateProvider;
import com.vaadin.flow.nodefeature.AttachExistingElementFeatureById;
import com.vaadin.flow.nodefeature.ElementPropertyMap;
import com.vaadin.flow.template.model.ListModelType;
import com.vaadin.flow.template.model.ModelDescriptor;
import com.vaadin.flow.template.model.ModelType;
import com.vaadin.flow.template.model.TemplateModel;
import com.vaadin.flow.template.model.TemplateModelProxyHandler;
import com.vaadin.server.CustomElementRegistry;
import com.vaadin.ui.Component;
import com.vaadin.util.ReflectTools;

/**
 * Component for an HTML element declared as a polymer component. The HTML
 * markup should be loaded using the {@link HtmlImport @HtmlImport} annotation
 * and the components should be associated with the web component element using
 * the {@link Tag @Tag} annotation.
 *
 * @param <M>
 *            a model class that will be used for template data propagation
 *
 * @see HtmlImport
 * @see Tag
 *
 * @author Vaadin Ltd
 */
public abstract class PolymerTemplate<M extends TemplateModel>
        extends AbstractTemplate<M> {

    private transient M model;

    /**
     * Creates the component that is responsible for Polymer template
     * functionality using the provided {@code parser}.
     * 
     * @param parser
     *            a template parser
     */
    public PolymerTemplate(TemplateParser parser) {
        Predicate<String> idVerifier = parseTemplate(
                parser.getTemplateContent(getClass(), getElement().getTag()));
        // This a workaround to propagate model to a Polymer template.
        // Correct implementation will follow in
        // https://github.com/vaadin/flow/issues/1371

        ElementPropertyMap modelMap = getStateNode()
                .getFeature(ElementPropertyMap.class);
        ModelDescriptor.get(getModelType()).getPropertyNames().forEach(
                propertyName -> modelMap.setProperty(propertyName, null));

        mapComponents(getClass(), idVerifier);
    }

    /**
     * Creates the component that is responsible for Polymer template
     * functionality.
     */
    public PolymerTemplate() {
        this(new DefaultTemplateParser());
    }

    /**
     * Check if the given Class {@code type} is found in the Model.
     *
     * @param type
     *            Class to check support for
     * @return True if supported by this PolymerTemplate
     */
    public boolean isSupportedClass(Class<?> type) {
        List<ModelType> modelTypes = ModelDescriptor.get(getModelType())
                .getPropertyNames().map(this::getModelType)
                .collect(Collectors.toList());

        boolean result = false;
        for (ModelType modelType : modelTypes) {
            if (type.equals(modelType.getJavaType())) {
                result = true;
            } else if (modelType instanceof ListModelType) {
                result = checkListType(type, modelType);
            }
            if (result) {
                break;
            }
        }
        return result;
    }

    private boolean checkListType(Class<?> type, ModelType modelType) {
        if (type.isAssignableFrom(List.class)) {
            return true;
        }
        ModelType model = modelType;
        while (model instanceof ListModelType) {
            model = ((ListModelType<?>) model).getItemType();
        }
        return type.equals(model.getJavaType());
    }

    private ModelType getModelType(String type) {
        return ModelDescriptor.get(getModelType()).getPropertyType(type);
    }

    /**
     * Get the {@code ModelType} for given class.
     *
     * @param type
     *            Type to get the ModelType for
     * @return ModelType for given Type
     */
    public ModelType getModelType(Type type) {
        List<ModelType> modelTypes = ModelDescriptor.get(getModelType())
                .getPropertyNames().map(this::getModelType)
                .collect(Collectors.toList());

        for (ModelType mtype : modelTypes) {
            if (type.equals(mtype.getJavaType())) {
                return mtype;
            } else if (mtype instanceof ListModelType) {
                ModelType modelType = getModelTypeForListModel(type, mtype);
                if (modelType != null) {
                    return modelType;
                }
            }
        }
        String msg = String.format(
                "Couldn't find ModelType for requested class %s",
                type.getTypeName());
        throw new IllegalArgumentException(msg);
    }

    @Override
    protected M getModel() {
        if (model == null) {
            model = createTemplateModelInstance();
        }
        return model;
    }

    private M createTemplateModelInstance() {
        ModelDescriptor<? extends M> descriptor = ModelDescriptor
                .get(getModelType());
        return TemplateModelProxyHandler.createModelProxy(getStateNode(),
                descriptor);
    }

    private ModelType getModelTypeForListModel(Type type, ModelType mtype) {
        ModelType modelType = mtype;
        while (modelType instanceof ListModelType) {
            if (type.equals(modelType.getJavaType())) {
                return modelType;
            }
            modelType = ((ListModelType<?>) modelType).getItemType();
        }
        // If type was not a list type then check the bean for List if it
        // matches the type
        if (type.equals(modelType.getJavaType())) {
            return modelType;
        }
        return null;
    }

    /* Map declared fields marked @Id */

    private void mapComponents(Class<?> cls, Predicate<String> idVerifier) {
        if (!AbstractTemplate.class.equals(cls.getSuperclass())) {
            // Parent fields
            mapComponents(cls.getSuperclass(), idVerifier);
        }

        Stream<Field> annotatedComponentFields = Stream
                .of(cls.getDeclaredFields())
                .filter(field -> !field.isSynthetic());

        annotatedComponentFields
                .forEach(filed -> tryMapComponentOrElement(filed, idVerifier));
    }

    @SuppressWarnings("unchecked")
    private void tryMapComponentOrElement(Field field,
            Predicate<String> idVeridier) {
        Optional<Id> idAnnotation = AnnotationReader.getAnnotationFor(field,
                Id.class);
        if (!idAnnotation.isPresent()) {
            return;
        }
        String id = idAnnotation.get().value();
        if (!idVeridier.test(id)) {
            throw new IllegalStateException(String.format(
                    "There is no element with "
                            + "id='%s' in the template file. Cannot map it using @%s",
                    id, Id.class.getSimpleName()));
        }

        Class<?> fieldType = field.getType();

        Element element = getElementById(id).orElse(null);

        if (element != null && getElement().equals(element)) {
            throw new IllegalArgumentException(
                    "Cannot map the root element of the template. "
                            + "This is always mapped to the template instance itself ("
                            + getClass().getName() + ")");
        } else if (!fieldType.isAnnotationPresent(Tag.class)) {
            String msg = String.format(
                    "Cannot instantiate element without tagName for '%s' defined for field '%s' found in class '%s'",
                    fieldType.getName(), field.getName(), getClass().getName());
            throw new IllegalArgumentException(msg);
        }

        Tag tag = fieldType.getAnnotation(Tag.class);

        attachExistingElementById(tag.value(), id, field);
    }

    private Optional<Element> getElementById(String id) {
        Optional<ShadowRoot> shadowRootOptional = getElement().getShadowRoot();

        ShadowRoot shadowRoot;

        if (shadowRootOptional.isPresent()) {
            shadowRoot = shadowRootOptional.get();
        } else {
            shadowRoot = getElement().attachShadow();
        }

        return shadowRoot.getChildren().flatMap(this::flattenChildren)
                .filter(element -> id.equals(element.getAttribute("id")))
                .findFirst();
    }

    private Stream<Element> flattenChildren(Element node) {
        if (node.getChildCount() > 0) {
            return node.getChildren().flatMap(this::flattenChildren);
        }
        return Stream.of(node);
    }

    /**
     * Attaches a child element with the given {@code tagName} and {@code id} to
     * an existing dom element on the client side with matching data.
     * 
     * @param tagName
     *            tag name of element, not {@code null}
     * @param id
     *            id of element to attach to
     * @param field
     *            field to attach {@code Element} or {@code Component} to
     */
    private void attachExistingElementById(String tagName, String id,
            Field field) {
        if (tagName == null) {
            throw new IllegalArgumentException(
                    "Tag name parameter cannot be null");
        }
        /*
         * create a node that should represent the client-side element. This
         * node won't be available anywhere and will be removed if there is no
         * appropriate element on the client-side. This node will be used after
         * client-side roundtrip for the appropriate element.
         */
        StateNode proposedNode = BasicElementStateProvider
                .createStateNode(tagName);
        Element element = Element.get(proposedNode);
        handleAttach(element, field);

        StateNode node = getElement().getNode();
        node.runWhenAttached(ui -> {
            node.getFeature(AttachExistingElementFeatureById.class)
                    .register(getElement(), proposedNode);
            ui.getPage().executeJavaScript(
                    "this.attachExistingElementById($0, $1, $2, $3);",
                    getElement(), tagName, proposedNode.getId(), id);
        });
    }

    private void handleAttach(Element element, Field field) {
        Class<?> fieldType = field.getType();
        if (Component.class.isAssignableFrom(fieldType)) {
            CustomElementRegistry.getInstance().wrapElementIfNeeded(element);
            Component component;

            Optional<Component> wrappedComponent = element.getComponent();
            if (wrappedComponent.isPresent())
                component = wrappedComponent.get();
            else {
                Class<? extends Component> componentType = (Class<? extends Component>) fieldType;
                component = Component.from(element, componentType);
            }

            ReflectTools.setJavaFieldValue(this, field, component);
        } else if (Element.class.isAssignableFrom(fieldType)) {
            ReflectTools.setJavaFieldValue(this, field, element);
        } else {
            String msg = String.format(
                    "The field '%s' in '%s' has an @'%s' "
                            + "annotation but the field type '%s' "
                            + "does not extend neither '%s' nor '%s'",
                    field.getName(), getClass().getName(),
                    Id.class.getSimpleName(), fieldType.getName(),
                    Component.class.getSimpleName(),
                    Element.class.getSimpleName());

            throw new IllegalArgumentException(msg);
        }
    }

    private Predicate<String> parseTemplate(
            com.vaadin.external.jsoup.nodes.Element element) {
        return id -> element.getElementById(id) != null;
    }

}
