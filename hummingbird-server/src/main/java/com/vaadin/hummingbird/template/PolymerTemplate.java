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

import java.util.function.Function;
import java.util.stream.Collectors;

import com.vaadin.annotations.AnnotationReader;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Tag;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.nodefeature.PolymerTemplateMap;
import com.vaadin.hummingbird.template.angular.ModelValueBindingProvider;
import com.vaadin.hummingbird.template.model.ModelDescriptor;
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
     * Creates a new template.
     */
    public PolymerTemplate() {
        String tagName = AnnotationReader
                .getAnnotationFor(getClass(), Tag.class).map(Tag::value)
                .orElseThrow(() -> new IllegalStateException(
                        "No tag annotation found"));

        Element element = new Element(tagName);

        ModelDescriptor<? extends M> modelDescriptor = ModelDescriptor
                .get(getModelType());

        element.getNode().getFeature(PolymerTemplateMap.class)
                .setModelBindings(modelDescriptor.getPropertyNames()
                        .collect(Collectors.toMap(Function.identity(),
                                ModelValueBindingProvider::new)));
        setElement(this, element);
        stateNode = element.getNode();
    }

    @Override
    protected ModelDescriptor<?> getModelDescriptor() {
        return stateNode.getFeature(PolymerTemplateMap.class)
                .getModelDescriptor();
    }

    @Override
    protected void setModelDescriptor(ModelDescriptor<?> descriptor) {
        stateNode.getFeature(PolymerTemplateMap.class)
                .setModelDescriptor(descriptor);
    }
}
