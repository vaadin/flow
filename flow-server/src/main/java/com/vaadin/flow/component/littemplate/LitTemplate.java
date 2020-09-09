/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.littemplate.LitTemplateParser.LitTemplateParserFactory;
import com.vaadin.flow.component.polymertemplate.Id;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.server.VaadinService;

/**
 * Component which renders a LitElement template.
 * <p>
 * A LitElement template is defined in a JavaScript module which should be
 * placed inside the {@literal frontend} folder and loaded using
 * {@link JsModule @JsModule}. The tag name defined for the Lit template must be
 * defined using {@link Tag @Tag} on this class.
 * <p>
 * By annotating a field using {@link Id @Id} you can map a
 * {@link Component @Component} instance to an element in the template, marked
 * with an {@code id} attribute which matches the field name or the optionally
 * given value to the annotation.
 * <p>
 * Note that injected components will have the same limitations as with
 * {@link PolymerTemplate}.
 * <p>
 * For more information about the LitElement project, see
 * https://lit-element.polymer-project.org/
 *
 * @see JsModule
 * @see Tag
 * @see Id
 *
 * @author Vaadin Ltd
 * @since
 */
public abstract class LitTemplate extends Component {

    static {
        UsageStatistics.markAsUsed("flow/LitTemplate", null);
    }

    /**
     * Creates the component mapped to a LitElement.
     * <p>
     * The call is delegated to
     * {@link #LitTemplate(LitTemplateParser, VaadinService)} via
     * {@code VaadinService.getCurrent()} as a service and parser created via
     * {@link LitTemplateParserFactory} retrieved from {@link Instantiator}.
     * 
     * @see #LitTemplate(LitTemplateParser, VaadinService)
     * @see VaadinService
     * @see LitTemplateParserFactory
     * @see Instantiator
     * @see Instantiator#getOrCreate(Class)
     */
    protected LitTemplate() {
        this(getParser(VaadinService.getCurrent()), VaadinService.getCurrent());
    }

    /**
     * Creates the component component mapped to a LitElement using the provided
     * {@code parser} and {@code service}.
     *
     * @param parser
     *            a template parser
     * @param service
     *            the related service instance
     */
    protected LitTemplate(LitTemplateParser parser, VaadinService service) {
        LitTemplateInitializer templateInitializer = new LitTemplateInitializer(
                this, VaadinService.getCurrent());
        templateInitializer.initChildElements();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Please note that components defined using {@link Id @Id} are not child
     * components. Only components explicitly added through methods such as
     * {@link HasComponents#add} or {@link Element#appendChild(Element...)} are
     * returned by this method.
     */
    @Override
    public Stream<Component> getChildren() {
        return super.getChildren();
    }

    static LitTemplateParser getParser(VaadinService service) {
        LitTemplateParserFactory factory = service.getInstantiator()
                .getOrCreate(LitTemplateParserFactory.class);
        return factory.createParser();
    }

}
