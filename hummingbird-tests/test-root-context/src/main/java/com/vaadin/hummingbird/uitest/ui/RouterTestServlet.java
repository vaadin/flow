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
package com.vaadin.hummingbird.uitest.ui;

import java.util.Optional;
import java.util.stream.Stream;

import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.ElementFactory;
import com.vaadin.hummingbird.router.DefaultErrorView;
import com.vaadin.hummingbird.router.HasChildView;
import com.vaadin.hummingbird.router.LocationChangeEvent;
import com.vaadin.hummingbird.router.NavigationEvent;
import com.vaadin.hummingbird.router.NavigationHandler;
import com.vaadin.hummingbird.router.Resolver;
import com.vaadin.hummingbird.router.RouterConfiguration;
import com.vaadin.hummingbird.router.RouterConfigurator;
import com.vaadin.hummingbird.router.View;
import com.vaadin.hummingbird.uitest.servlet.TestViewRenderer;
import com.vaadin.hummingbird.uitest.ui.RouterTestServlet.MyRouterConfigurator;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;

@WebServlet(asyncSupported = true, urlPatterns = { "/router-session/*" })
@VaadinServletConfiguration(productionMode = false, routerConfigurator = MyRouterConfigurator.class)
public class RouterTestServlet extends VaadinServlet {

    public static class MyRouterConfigurator implements RouterConfigurator {

        @Override
        public void configure(RouterConfiguration configuration) {
            configuration.setResolver(new Resolver() {

                @Override
                public Optional<NavigationHandler> resolve(
                        NavigationEvent navigationEvent) {
                    if (navigationEvent.getLocation().getFirstSegment()
                            .equals("")) {
                        return Optional
                                .of(new TestViewRenderer(NormalView.class));
                    }

                    Optional<Class<? extends View>> res = getViewClasses()
                            .filter(c -> c.getSimpleName()
                                    .equals(navigationEvent.getLocation()
                                            .getFirstSegment()))
                            .findAny();
                    NavigationHandler handler = res
                            .map(clazz -> new TestViewRenderer(clazz,
                                    Layout.class))
                            .orElse(new TestViewRenderer(
                                    DefaultErrorView.class));
                    return Optional.of(handler);
                }
            });

        }

    }

    public static class Layout implements HasChildView {

        private Element element;
        private Element sessionId;

        public Layout() {
            element = ElementFactory.createDiv();
            sessionId = ElementFactory.createDiv().setAttribute("id",
                    "sessionId");
            element.appendChild(sessionId);
            element.appendChild(ElementFactory.createDiv());
            element.appendChild(ElementFactory.createHr());
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
        public void onLocationChange(LocationChangeEvent event) {
            WrappedSession session = VaadinSession.getCurrent().getSession();
            if (session == null) {
                sessionId.setText("No session");
            } else {
                sessionId.setText("Session id: " + session.getId());
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
            element = ElementFactory.createDiv();
            getViewClasses().forEach(c -> {
                String viewName = c.getSimpleName();
                Element div = ElementFactory.createDiv();
                element.appendChild(div);
                if (getClass() == c) {
                    div.appendChild(ElementFactory.createStrong(viewName));
                } else {
                    div.appendChild(ElementFactory.createRouterLink(viewName,
                            viewName));
                }
                div.appendChild(ElementFactory.createHr());
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
        public void onLocationChange(LocationChangeEvent event) {
            throw new RuntimeException(
                    "Intentionally caused by " + getClass().getSimpleName());
        }
    }

    public static class ViewWhichInvalidatesSession extends MyAbstractView {
        public ViewWhichInvalidatesSession() {
            super();
        }

        @Override
        public void onLocationChange(LocationChangeEvent event) {
            VaadinSession.getCurrent().getSession().invalidate();
        }
    }
}
