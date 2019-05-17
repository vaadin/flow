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
package com.vaadin.flow.uitest.ui.template;

import java.util.stream.Stream;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.dependency.HtmlImport;
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
    @HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/ClientUpdateMode.html")
    @JsModule("ClientUpdateMode.js")
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
