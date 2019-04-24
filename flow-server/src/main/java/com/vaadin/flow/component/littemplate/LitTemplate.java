/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.component.littemplate;

import java.util.Collections;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.polymertemplate.AbstractTemplate;
import com.vaadin.flow.component.polymertemplate.Id;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.templatemodel.TemplateModel;

/**
 * Component which renders a Lit Element template.
 * <p>
 * A Lit Element template is defined in a JavaScript module which should be
 * placed inside the {@literal frontend} folder and loaded using
 * {@link JSModule @JsModule}. The tag name defined for the Lit template must be
 * defined using {@link Tag @Tag} on this class.
 * <p>
 * By annotating a field using {@link Id @Id} you can map a
 * {@link Component @Component} instance to an element in the template, marked
 * with an {@code id} attribute which matches the field name or the optionally
 * given value to the annotation.
 * <p>
 * Note that injected components will have the same limitations as with
 * {@link PolymerTemplate}.
 *
 * @param <M>
 *            a model class that will be used for template data propagation
 *
 * @see JSModule
 * @see Tag
 * @see Id
 * @see https://lit-element.polymer-project.org/
 *
 * @author Vaadin Ltd
 * @since 2.0
 */
@NpmPackage(value = "lit-element", version = "2.1.0")
public abstract class LitTemplate<M extends TemplateModel>
        extends AbstractTemplate<M> {

    static {
        UsageStatistics.markAsUsed("flow/LitTemplate", null);
    }

    /**
     * Creates the component mapped to a Lit Element.
     */
    protected LitTemplate() {
        LitTemplateInitializer templateInitializer = new LitTemplateInitializer(
                this, VaadinService.getCurrent());
        templateInitializer.mapComponents();

        initModel(Collections.emptySet());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Please note that components defined using {@link Id @Id} are not child
     * components since they are attached inside the Shadow DOM. Only components
     * explicitly added through methods such as {@link HasComponents#add} or
     * {@link Element#appendChild(Element...)} are returned by this method.
     */
    @Override
    public Stream<Component> getChildren() {
        return super.getChildren();
    }

}
