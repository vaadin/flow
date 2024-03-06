/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template;

import java.util.stream.Stream;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.AllowClientUpdates;
import com.vaadin.flow.templatemodel.ClientUpdateMode;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.ui.AbstractDivView;

@Route("com.vaadin.flow.uitest.ui.template.ClientUpdateModeView")
public class ClientUpdateModeView extends AbstractDivView {

    public interface ClientUpdateModeModel extends TemplateModel {
        public String getValue();

        @AllowClientUpdates(ClientUpdateMode.ALLOW)
        public String getIndirectAllowed();

        public String getIndirect();

        @AllowClientUpdates(ClientUpdateMode.DENY)
        public String getTwoWayDenied();
    }

    @Tag("client-update-mode")
    @JsModule("./ClientUpdateMode.js")
    public static class ClientUpdateModeTemplate
            extends PolymerTemplate<ClientUpdateModeModel> {
    }

    public ClientUpdateModeView() {
        ClientUpdateModeTemplate template = new ClientUpdateModeTemplate();
        add(template);

        Element element = template.getElement();
        Stream.of("value", "indirectAllowed", "indirect", "twoWayDenied")
                .forEach(propertyName -> {
                    element.addPropertyChangeListener(propertyName,
                            event -> add(
                                    new Text(propertyName + " changed to "
                                            + event.getValue()),
                                    new Html("<br>")));
                });

    }
}
