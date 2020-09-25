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
package com.vaadin.flow.component.polymertemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.littemplate.InjectableLitElementInitializer;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.component.template.internal.AbstractInjectableElementInitializer;
import com.vaadin.flow.dom.Element;

/**
 * Initialize a polymer template element with data.
 * 
 * @author Vaadin Ltd
 * @since
 * @deprecated {@link InjectableLitElementInitializer} should be used for Lit
 *             templates since polymer support is deprecated, we recommend you
 *             to use {@link LitTemplate} instead. Read more details from
 *             <a href=
 *             "https://vaadin.com/blog/future-of-html-templates-in-vaadin">the
 *             Vaadin blog.</a>
 */
@Deprecated
public class InjectablePolymerElementInitializer
        extends AbstractInjectableElementInitializer {

    private static final String DYNAMIC_ATTRIBUTE_PREFIX = "Template {} contains an attribute {} in element {} whose value";
    private final Class<? extends Component> templateClass;

    /**
     * Creates an initializer for the {@code element}.
     * 
     * @param element
     *            element to initialize
     * @param templateClass
     *            the class of the template component
     */
    public InjectablePolymerElementInitializer(Element element,
            Class<? extends Component> templateClass) {
        super(element);
        this.templateClass = templateClass;
    }

    @Override
    protected boolean isStaticAttribute(String name, String value) {
        if (name.endsWith("$")) {
            // this is an attribute binding, ignore it since we don't support
            // bindings: the value is not an expression
            getLogger().debug(
                    "Template {} contains an attribute {} in element {} which "
                            + "ends with $ and ignored by initialization since this is an attribute binding",
                    templateClass.getSimpleName(), name, getElement().getTag());
            return false;
        }
        if (value == null) {
            return true;
        }
        if (value.contains("{{") && value.contains("}}")) {
            // this is a binding, skip it
            getLogger().debug(
                    "{} contains two-way binding and it's ignored by initilization",
                    DYNAMIC_ATTRIBUTE_PREFIX, templateClass.getSimpleName(),
                    name, getElement().getTag());
            return false;
        }
        if (value.contains("[[") && value.contains("]]")) {
            // this is another binding, skip it
            getLogger().debug(
                    "{} contains binding and it's ignored by initilization",
                    DYNAMIC_ATTRIBUTE_PREFIX, templateClass.getSimpleName(),
                    name, getElement().getTag());
            return false;
        }
        if (value.contains("${") && value.contains("}")) {
            // this is a dynamic value
            getLogger().debug("{} is dynamic and it's ignored by initilization",
                    DYNAMIC_ATTRIBUTE_PREFIX, templateClass.getSimpleName(),
                    name, getElement().getTag());
            return false;
        }
        return true;
    }

    private static Logger getLogger() {
        return LoggerFactory
                .getLogger(InjectablePolymerElementInitializer.class);
    }
}
