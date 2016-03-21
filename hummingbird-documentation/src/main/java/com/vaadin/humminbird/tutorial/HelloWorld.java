/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.humminbird.tutorial;

import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.ElementFactory;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.UI;

/**
 * Tutorial code related to tutorial-hello-world.asciidoc.
 */
public class HelloWorld {

    private HelloWorld() {
    };

    /**
     * The UI class for Hello World tutorial.
     */
    public static class HelloWorldUI extends UI {

        /**
         * The servlet definition for Hello World tutorial.
         */
        @WebServlet(urlPatterns = "/*", name = "HelloWorldServlet")
        @VaadinServletConfiguration(ui = HelloWorldUI.class, productionMode = false)
        public static class HelloWorldServlet extends VaadinServlet {
        }

        @Override
        protected void init(VaadinRequest request) {
            Element div = ElementFactory.createDiv("Hello world");
            getElement().appendChild(div);
        }
    }

}
