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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        Stream<String> propertyNames = ModelDescriptor.get(getModelType())
                .getPropertyNames();
        Set<Class> modelClassCollection = propertyNames.map(this::getJavaClass)
                .flatMap(Set::stream).collect(Collectors.toSet());
        return Collections.unmodifiableSet(modelClassCollection);
    }

    private Set<Class> getJavaClass(String type) {
        Type javaType = getModelType(type).getJavaType();
        return getSubType(javaType);
    }

    private Set<Class> getSubType(Type javaType) {
        Set<Class> subClasses = new HashSet<>();
        Class aClass = GenericTypeReflector.erase(javaType);
        if (List.class.isAssignableFrom(aClass)) {
            Type argumentType = javaType;
            do {
                argumentType = ((ParameterizedType) GenericTypeReflector
                        .capture(argumentType)).getActualTypeArguments()[0];
                aClass = GenericTypeReflector.erase(argumentType);
                subClasses.add(aClass);
            } while (List.class.isAssignableFrom(aClass));
        } else {
            subClasses.add(aClass);
        }
        return subClasses;
    }

    private ModelType getModelType(String type) {
        return ModelDescriptor.get(getModelType()).getPropertyType(type);
    }

    /**
     * Get the {@code ModelType} for given class.
     * 
     * @param modelClass
     *            Class to find ModelType for
     * @return ModelType of modelClass
     */
    public ModelType getModelType(Class<?> modelClass) {
        return ModelDescriptor.get(getModelType()).getPropertyNames()
                .map(this::getModelType)
                .filter(modelType -> typeClassEqualsClass(modelClass,
                        modelType))
                .findFirst().orElse(null);
    }

    private boolean typeClassEqualsClass(Class<?> modelClass,
            ModelType modelType) {
        Set<Class> subType = getSubType(modelType.getJavaType());
        return subType.stream().anyMatch(
                type -> GenericTypeReflector.erase(type).equals(modelClass));
    }
}
