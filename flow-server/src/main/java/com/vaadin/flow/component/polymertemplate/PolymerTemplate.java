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
package com.vaadin.flow.component.polymertemplate;

import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.flow.component.HtmlImport;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ElementPropertyMap;
import com.vaadin.flow.templatemodel.ListModelType;
import com.vaadin.flow.templatemodel.ModelDescriptor;
import com.vaadin.flow.templatemodel.ModelType;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.templatemodel.TemplateModelProxyHandler;

import elemental.json.Json;
import elemental.json.JsonArray;

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
        new TemplateInitializer(this, parser).initChildElements();

        initModelProperties();
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

    private static boolean checkListType(Class<?> type, ModelType modelType) {
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

    private static ModelType getModelTypeForListModel(Type type,
            ModelType mtype) {
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

    private JsonArray filterUnsetProperties(List<String> properties) {
        JsonArray array = Json.createArray();
        ElementPropertyMap map = getStateNode()
                .getFeature(ElementPropertyMap.class);
        int i = 0;
        for (String property : properties) {
            if (!map.hasProperty(property)) {
                array.set(i, property);
                i++;
            }
        }
        return array;
    }

    private void initModelProperties() {
        // initialize model: fill all model properties with their initial value
        getModel();

        // remove properties whose values are not StateNode from the property
        // map and return their names as a list
        List<String> propertyNames = removeSimpleProperties();

        /*
         * Now populate model properties on the client side. Only explicitly set
         * by the developer properties are in the map at the moment of execution
         * since all simple properties have been removed from the map above.
         * Such properties are excluded from the argument list and won't be
         * populated on the client side.
         *
         * All explicitly set model properties will be sent from the server as
         * usual and will take precedence over the client side values.
         */
        getStateNode().runWhenAttached(ui -> ui.getInternals().getStateTree()
                .beforeClientResponse(getStateNode(),
                        () -> ui.getPage().executeJavaScript(
                                "this.populateModelProperties($0, $1)",
                                getElement(),
                                filterUnsetProperties(propertyNames))));
    }

    private List<String> removeSimpleProperties() {
        ElementPropertyMap map = getStateNode()
                .getFeature(ElementPropertyMap.class);
        List<String> props = map.getPropertyNames()
                .filter(name -> !(map.getProperty(name) instanceof StateNode))
                .collect(Collectors.toList());
        props.forEach(map::removeProperty);
        return props;
    }

}
