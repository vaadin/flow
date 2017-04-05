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
package com.vaadin.hummingbird.template;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.googlecode.gentyref.GenericTypeReflector;

import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Tag;
import com.vaadin.hummingbird.nodefeature.ModelMap;
import com.vaadin.hummingbird.template.model.ModelDescriptor;
import com.vaadin.hummingbird.template.model.ModelType;
import com.vaadin.hummingbird.template.model.TemplateModel;

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

    private Set<Class> modelClasses;
    private Map<Class, ModelType> modelTypes;

    /**
     * Creates the component that is responsible for Polymer template
     * functionality.
     */
    public PolymerTemplate() {
        // This a workaround to propagate model to a Polymer template.
        // Correct implementation will follow in
        // https://github.com/vaadin/hummingbird/issues/1371

        ModelMap modelMap = getStateNode().getFeature(ModelMap.class);
        ModelDescriptor.get(getModelType()).getPropertyNames()
                .forEach(propertyName -> modelMap.setValue(propertyName, null));
    }

    /**
     * Collects all {@code Class}es used in the TemplateModel.
     * 
     * @return Set with classes used in model
     */
    public Set<Class> getModelClasses() {
        if (modelClasses == null) {
            modelTypes = new HashMap<>();
            modelClasses = Collections
                    .unmodifiableSet(ModelDescriptor.get(getModelType())
                            .getPropertyNames().map(type -> getJavaClass(type))
                            .collect(Collectors.toSet()));
        }
        return modelClasses;
    }

    private Class getJavaClass(String type) {
        Type javaType = ModelDescriptor.get(getModelType())
                .getPropertyType(type).getJavaType();
        Class aClass = GenericTypeReflector.erase(javaType);
        if (List.class.isAssignableFrom(aClass)) {
            do {
                Type argumentType = ((ParameterizedType) GenericTypeReflector
                        .capture(javaType)).getActualTypeArguments()[0];
                aClass = GenericTypeReflector.erase(argumentType);
            } while (List.class.isAssignableFrom(aClass));
        }
        modelTypes.put(aClass,
                ModelDescriptor.get(getModelType()).getPropertyType(type));
        return aClass;
    }

    /**
     * Get the {@code ModelType} for given class.
     * 
     * @param modelClass
     *            Class to find ModelType for
     * @return ModelType of modelClass
     */
    public ModelType getModelType(Class<?> modelClass) {
        return modelTypes.get(modelClass);
    }
}
