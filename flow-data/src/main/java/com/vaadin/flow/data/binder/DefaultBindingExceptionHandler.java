/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.data.binder;

import java.util.Optional;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.NodeOwner;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

/**
 * Default implementation of {@link BindingExceptionHandler}.
 * <p>
 * The handler tries to identify the {@link HasElement} field using
 * {@code "label"} and {@code "id"} property values and if they are not
 * available it tries dump all the attributes and properties in dev mode. The
 * exception is not produced if the element has no any attribute or property.
 *
 * @author Vaadin Ltd
 *
 */
public class DefaultBindingExceptionHandler implements BindingExceptionHandler {

    private static final String ID = "id";
    private static final String LABEL = "label";

    @Override
    public Optional<BindingException> handleException(HasValue<?, ?> field,
            Exception exception) {
        if (!(field instanceof HasElement)) {
            return Optional.empty();
        }
        Element element = ((HasElement) field).getElement();
        StringBuilder majorProperties = new StringBuilder();
        String label = element.getProperty(LABEL);
        if (label != null) {
            appendProperty(majorProperties, LABEL, label);
        }
        String id = element.getProperty(ID);
        if (id != null) {
            appendProperty(majorProperties, ID, id);
        }

        UI ui = getUI(element);
        if (majorProperties.length() == 0 && ui != null
                && ui.getSession() != null
                && !ApplicationConfiguration
                        .get(ui.getSession().getService().getContext())
                        .isProductionMode()) {
            element.getAttributeNames()
                    .forEach(attribute -> appendProperty(majorProperties,
                            attribute, element.getAttribute(attribute)));
            element.getPropertyNames()
                    .forEach(property -> appendProperty(majorProperties,
                            property, element.getProperty(property)));
        }

        if (majorProperties.length() > 0) {
            majorProperties.insert(0,
                    "An exception has been thrown inside binding logic for the field element [")
                    .append("]");
        }

        if (majorProperties.length() > 0) {
            return Optional.of(new BindingException(majorProperties.toString(),
                    exception));
        }
        return Optional.empty();
    }

    private UI getUI(Element element) {
        NodeOwner owner = element.getNode().getOwner();
        UI ui = null;
        if (element.getNode().isAttached()) {
            ui = ((StateTree) owner).getUI();
        }
        if (ui == null) {
            ui = UI.getCurrent();
        }
        return ui;
    }

    private void appendProperty(StringBuilder builder, String name,
            String value) {
        if (builder.length() > 0) {
            builder.append(", ");
        }
        builder.append(name).append("='").append(value).append("'");
    }
}
