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
package com.vaadin.flow.npmtest.ui;

import javax.servlet.annotation.WebServlet;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.polymertemplate.Id;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.templatemodel.TemplateModel;

@Route(value = "")
@Tag("my-component")
@HtmlImport("frontend://my-component.html")
@JsModule("./my-component.js")
public class IdTestView extends PolymerTemplate<TemplateModel> {

    /**
     * All other ITs are run with `bowerMode=true` and it's not possible
     * to configure each test module with its own value at pom.xml level.
     *
     * It should be possible to add a servlet fragment to each test in the
     * suite, but seems simpler so far to have this here to override default
     * system property just for npm test.
     */
    @WebServlet(asyncSupported = true, urlPatterns = { "/*" })
    public static class NpmEnabledVaadinServlet extends VaadinServlet {
      public NpmEnabledVaadinServlet() {
        System.clearProperty("vaadin.bowerMode");
      }
    }

    @Id
    NativeButton button;
    @Id
    Div content;
    public IdTestView() {
        button.addClickListener(e -> {
            String s = content.getText();
            Integer val = 1 + Integer.parseInt(s == null || s.isEmpty() ? "0" : s);
            content.setText(String.valueOf(val));
        });
    }
}
