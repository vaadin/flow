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
package com.vaadin.hummingbird.uitest.ui;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.router.ErrorNavigationHandler;
import com.vaadin.hummingbird.router.HasChildView;
import com.vaadin.hummingbird.router.Location;
import com.vaadin.hummingbird.router.NavigationEvent;
import com.vaadin.hummingbird.router.NavigationHandler;
import com.vaadin.hummingbird.router.Resolver;
import com.vaadin.hummingbird.router.RouterUI;
import com.vaadin.hummingbird.router.View;
import com.vaadin.hummingbird.router.ViewRenderer;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;

@WebServlet(asyncSupported = true, urlPatterns = { "/router-session/*" })
@VaadinServletConfiguration(ui = RouterUI.class, productionMode = false)
public class RouterTestServlet extends VaadinServlet {

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        getService().getRouter().setResolver(new Resolver() {
            @Override
            public NavigationHandler resolve(NavigationEvent navigationEvent) {
                if (navigationEvent.getLocation().getFirstSegment()
                        .equals("")) {
                    return new ViewRenderer(NormalView.class);
                }

                Optional<Class<? extends View>> res = getViewClasses()
                        .filter(c -> c.getSimpleName().equals(navigationEvent
                                .getLocation().getFirstSegment()))
                        .findAny();
                return res.map(
                        new Function<Class<? extends View>, NavigationHandler>() {
                    @Override
                    public NavigationHandler apply(Class<? extends View> c) {
                        return new ViewRenderer(c, Layout.class);
                    }
                }).orElseGet(() -> {
                    return new ErrorNavigationHandler(404);
                });
            }
        });
    }

    public static class Layout implements HasChildView {

        private Element element;
        private Element sessionId;

        public Layout() {
            element = new Element("div");
            sessionId = new Element("div").setAttribute("id", "sessionId");
            element.appendChild(sessionId);
            element.appendChild(new Element("div"));
            element.appendChild(new Element("hr"));
        }

        @Override
        public Element getElement() {
            return element;
        }

        @Override
        public void setChildView(View childView) {
            element.setChild(1, childView.getElement());
        }

        @Override
        public void onLocationChange(Location location) {
            WrappedSession session = VaadinSession.getCurrent().getSession();
            if (session == null) {
                sessionId.setTextContent("No session");
            } else {
                sessionId.setTextContent("Session id: " + session.getId());
            }
        }

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Stream<Class<? extends View>> getViewClasses() {
        return (Stream) Stream.of(NormalView.class, AnotherNormalView.class,
                ViewWhichCausesInternalException.class,
                ViewWhichInvalidatesSession.class);
    }

    public static abstract class MyAbstractView implements View {
        private Element element;

        protected MyAbstractView() {
            element = new Element("div");
            getViewClasses().forEach(c -> {
                String viewName = c.getSimpleName();
                Element div = new Element("div");
                element.appendChild(div);
                if (getClass() == c) {
                    div.appendChild(new Element("b").setTextContent(viewName));
                } else {
                    div.appendChild(
                            new Element("a").setAttribute("href", viewName)
                                    .setAttribute("routerlink", "")
                                    .setTextContent(viewName));
                }
                div.appendChild(new Element("hr"));
            });
        }

        @Override
        public Element getElement() {
            return element;
        }
    }

    public static class NormalView extends MyAbstractView {
        public NormalView() {
            super();
        }
    }

    public static class AnotherNormalView extends MyAbstractView {
        public AnotherNormalView() {
            super();
        }
    }

    public static class ViewWhichCausesInternalException
            extends MyAbstractView {
        public ViewWhichCausesInternalException() {
            super();
        }

        @Override
        public void onLocationChange(Location location) {
            throw new RuntimeException(
                    "Intentionally caused by " + getClass().getSimpleName());
        }
    }

    public static class ViewWhichInvalidatesSession extends MyAbstractView {
        public ViewWhichInvalidatesSession() {
            super();
        }

        @Override
        public void onLocationChange(Location location) {
            VaadinSession.getCurrent().getSession().invalidate();
        }
    }
}
