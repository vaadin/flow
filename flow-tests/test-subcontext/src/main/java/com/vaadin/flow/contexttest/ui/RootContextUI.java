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
package com.vaadin.flow.contexttest.ui;

import javax.servlet.annotation.WebServlet;

import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServletConfiguration;

public class RootContextUI extends DependencyUI {

    /**
     * The main servlet for the application.
     */
    @WebServlet(urlPatterns = {
            "/*" }, name = "UIServlet", asyncSupported = true)
    @VaadinServletConfiguration(ui = RootContextUI.class, productionMode = false)
    public static class Servlet extends NoRouterServlet {
    }

    @Override
    protected void init(VaadinRequest request) {
        getElement().appendChild(ElementFactory.createDiv("Root Context UI")
                .setAttribute("id", "root"));
        super.init(request);
    }

}
