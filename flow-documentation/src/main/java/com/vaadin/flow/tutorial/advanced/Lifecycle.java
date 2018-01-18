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
package com.vaadin.flow.tutorial.advanced;

import javax.servlet.ServletException;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.SessionDestroyEvent;
import com.vaadin.flow.server.SessionDestroyListener;
import com.vaadin.flow.server.SessionInitEvent;
import com.vaadin.flow.server.SessionInitListener;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("advanced/tutorial-application-lifecycle.asciidoc")
public class Lifecycle {

    public static class Basics {
        public class MyServlet extends VaadinServlet {
            @Override
            protected void servletInitialized() throws ServletException {
                super.servletInitialized();
                // ...
            }
        }
    }

    public class MyServlet extends VaadinServlet
            implements SessionInitListener, SessionDestroyListener {

        @Override
        protected void servletInitialized() throws ServletException {
            super.servletInitialized();
            getService().addSessionInitListener(this);
            getService().addSessionDestroyListener(this);
        }

        @Override
        public void sessionInit(SessionInitEvent event)
                throws ServiceException {
            // Do session start stuff here
        }

        @Override
        public void sessionDestroy(SessionDestroyEvent event) {
            // Do session end stuff here
        }
    }

    @Route("")
    public class MainLayout extends Div {

        @Override
        protected void onAttach(AttachEvent attachEvent) {
            UI ui = getUI().get();
            Button button = new Button("Logout", event -> {
                // Redirect this page immediately
                ui.getPage().executeJavaScript(
                        "window.location.href='logout.html'");

                // Close the session
                ui.getSession().close();
            });

            add(button);

            // Notice quickly if other UIs are closed
            ui.setPollInterval(3000);
        }
    }
}
