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
package com.vaadin.flow.uitest.ui;

import javax.servlet.annotation.WebServlet;

import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.router.RouterLink;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletConfiguration;
import com.vaadin.ui.html.Div;

@WebServlet(asyncSupported = true, urlPatterns = { "/router-link/*" })
@VaadinServletConfiguration(productionMode = false, usingNewRouting = true)
public class RouterLinkTestServlet extends VaadinServlet {

    public static class ViewImpl extends Div {
        public ViewImpl() {
            RouterLink link = new RouterLink("Home", ViewImpl.class);
            link.addClassName("root-link");
            add(link);
            getElement().appendChild(ElementFactory.createBr());
            link = new RouterLink("Another view", AnotherView.class);
            add(link);
        }
    }

    public static class AnotherView extends Div {
    }
}
