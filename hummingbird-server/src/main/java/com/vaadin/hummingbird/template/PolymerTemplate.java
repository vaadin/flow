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

import com.vaadin.annotations.AnnotationReader;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Tag;
import com.vaadin.hummingbird.template.angular.ElementTemplateBuilder;
import com.vaadin.hummingbird.template.angular.ModelValueBindingProvider;
import com.vaadin.hummingbird.template.model.ModelDescriptor;
import com.vaadin.hummingbird.template.model.TemplateModel;

/**
 * Component for an HTML element declared as a polymer component. The HTML
 * markup should be loaded using the {@link HtmlImport @HtmlImport} annotation
 * and the components should be associated with the web component element using
 * the {@link Tag @Tag} annotation.
 *
 * @see HtmlImport
 * @see Tag
 *
 * @author Vaadin Ltd
 */
public abstract class PolymerTemplate<M extends TemplateModel>
        extends AbstractTemplate<M> {
    public PolymerTemplate() {
        super(null);

        String tagName = AnnotationReader
                .getAnnotationFor(getClass(), Tag.class).map(Tag::value)
                .orElseThrow(() -> new IllegalStateException(
                        "No tag annotation found"));

        ElementTemplateBuilder elementTemplateBuilder = new ElementTemplateBuilder(
                tagName);
        ModelDescriptor<? extends M> modelDescriptor = ModelDescriptor
                .get(getModelType());
        modelDescriptor.getPropertyNames()
                .forEach(propertyName -> elementTemplateBuilder.setProperty(
                        propertyName, new ModelValueBindingProvider(
                                propertyName)));

        setTemplateRoot(elementTemplateBuilder.build(null).iterator().next());
    }
}
