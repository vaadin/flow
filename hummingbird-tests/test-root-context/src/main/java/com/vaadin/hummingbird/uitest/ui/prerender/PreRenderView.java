/*
 * Copyright 2000-2017 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless requigreen by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.hummingbird.uitest.ui.prerender;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.vaadin.annotations.StyleSheet;
import com.vaadin.hummingbird.html.Anchor;
import com.vaadin.hummingbird.html.Button;
import com.vaadin.hummingbird.html.Div;
import com.vaadin.hummingbird.html.Hr;
import com.vaadin.hummingbird.router.View;
import com.vaadin.hummingbird.template.model.TemplateModel;
import com.vaadin.hummingbird.uitest.ui.DependencyView;
import com.vaadin.hummingbird.uitest.ui.DependencyView.JSStreamFactory;
import com.vaadin.server.StreamResourceRegistration;
import com.vaadin.server.VaadinService;
import com.vaadin.ui.AttachEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.Template;
import com.vaadin.ui.Text;

@StyleSheet("/com/vaadin/hummingbird/uitest/prerender/prerender.css")
public class PreRenderView extends Div implements View {

    public static class PreTemplate extends Template {

        public PreTemplate() {
            getModel().setColored(true);
            getModel().setLink("/view");
        }

        public interface Model extends TemplateModel {
            public void setColored(boolean colored);

            public void setLink(String link);
        }

        @Override
        protected Model getModel() {
            return (Model) super.getModel();
        }
    }

    private StreamResourceRegistration jsImport;

    public PreRenderView() {
        add(createComponentPart());
        add(new Hr());
        add(new PreTemplate());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        if (null != VaadinService.getCurrentRequest().getParameter("delay")) {
            jsImport = DependencyView.registerResource(attachEvent.getUI(),
                    "some.js", new JSStreamFactory("delayed import", 5000) {
                        @Override
                        protected InputStream stringToStream(String jsString) {
                            byte[] bytes = "window.document.body.appendChild(window.document.createElement('meter'));"
                                    .getBytes(StandardCharsets.UTF_8);
                            return new ByteArrayInputStream(bytes);
                        }
                    });
            attachEvent.getUI().getPage()
                    .addJavaScript(jsImport.getResourceUri().toString());
        }
    }

    private static Component createComponentPart() {
        Div container = new Div();
        container.setId("component");

        container.add(new Text("Components"));
        Button button = new Button("A button");
        button.setId("cmp-basic");
        container.add(button);

        Button styleButton = new Button("A green button using inline style");
        styleButton.setId("cmp-inline-style");
        styleButton.getStyle().set("backgroundColor", "green");
        container.add(styleButton);
        Button classButton = new Button("A green button using class");
        classButton.setId("cmp-class");
        classButton.setClassName("backgroundClass");
        container.add(classButton);

        Anchor link = new Anchor("/view", "A link");
        link.setId("cmp-link");
        container.add(link);

        return container;
    }

}
