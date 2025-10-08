/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.uitest.servlet;

import jakarta.servlet.annotation.WebServlet;

import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedSession;

@WebServlet(asyncSupported = true, urlPatterns = { "/new-router-session/*" })
public class RouterTestServlet extends VaadinServlet {

    public static class ClassNameDiv extends Div {
        public ClassNameDiv() {
            setText(this.getClass().getSimpleName());
            setId("name-div");
        }
    }

    @Route("abc")
    public static class RootNavigationTarget extends ClassNameDiv {
    }

    @Route("throws")
    public static class ThrowsNavigationTarget extends Div {
        public ThrowsNavigationTarget() {
            throw new IllegalStateException("This is an expected exception");
        }
    }

    @Route("foo")
    public static class FooNavigationTarget extends ClassNameDiv {
    }

    @Route("foo/bar")
    public static class FooBarNavigationTarget extends ClassNameDiv {
    }

    @Route("greeting")
    public static class GreetingNavigationTarget extends Div
            implements HasUrlParameter<String> {
        public GreetingNavigationTarget() {
            setId("greeting-div");
        }

        @Override
        public void setParameter(BeforeEvent event, String parameter) {
            setText(String.format("Hello, %s!", parameter));
        }
    }

    @Route("ElementQueryView")
    public static class ElementQueryView extends Div {

        public ElementQueryView() {
            for (int i = 0; i < 10; i++) {
                add(new Div(new NativeButton("Button " + i)));
            }
        }

    }

    public static class MyRouterLayout extends Div implements RouterLayout {

        public MyRouterLayout() {
            setId("layout");
        }
    }

    @Route(value = "baz", layout = MyRouterLayout.class)
    public static class ChildNavigationTarget extends ClassNameDiv {

    }

    public static class MainLayout extends Div implements RouterLayout {
        public MainLayout() {
            setId("mainLayout");
        }
    }

    @ParentLayout(MainLayout.class)
    public static class MiddleLayout extends Div implements RouterLayout {
        public MiddleLayout() {
            setId("middleLayout");
        }
    }

    @Route(value = "target", layout = MiddleLayout.class)
    public static class TargetLayout extends ClassNameDiv {

    }

    @Route("noParent")
    @RouteAlias(value = "twoParents", layout = MiddleLayout.class)
    public static class AliasLayout extends ClassNameDiv {

    }

    public static class Layout extends Div
            implements RouterLayout, BeforeEnterObserver {

        private Element sessionId;

        public Layout() {
            sessionId = ElementFactory.createDiv().setAttribute("id",
                    "sessionId");
            getElement().appendChild(sessionId);
            getElement().appendChild(ElementFactory.createDiv());
            getElement().appendChild(ElementFactory.createHr());
        }

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            WrappedSession session = VaadinSession.getCurrent().getSession();
            if (session == null) {
                sessionId.setText("No session");
            } else {
                sessionId.setText("Session id: " + session.getId());
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Stream<Class<? extends Component>> getViewClasses() {
        return (Stream) Stream.of(NormalView.class, AnotherNormalView.class,
                ViewWhichCausesInternalException.class,
                ViewWhichInvalidatesSession.class);
    }

    public static abstract class MyAbstractView extends Div {

        protected MyAbstractView() {
            getViewClasses().forEach(c -> {
                String viewName = c.getSimpleName();
                Element div = ElementFactory.createDiv();
                getElement().appendChild(div);
                if (getClass() == c) {
                    div.appendChild(ElementFactory.createStrong(viewName));
                } else {
                    div.appendChild(ElementFactory.createRouterLink(viewName,
                            viewName));
                }
                div.appendChild(ElementFactory.createHr());
            });
        }

    }

    @Route(value = "NormalView", layout = Layout.class)
    public static class NormalView extends MyAbstractView {
    }

    @Route(value = "AnotherNormalView", layout = Layout.class)
    public static class AnotherNormalView extends MyAbstractView {
    }

    @Route(value = "ViewWhichCausesInternalException", layout = Layout.class)
    public static class ViewWhichCausesInternalException extends MyAbstractView
            implements BeforeEnterObserver {

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            throw new RuntimeException(
                    "Intentionally caused by " + getClass().getSimpleName());
        }
    }

    @Route(value = "ViewWhichInvalidatesSession", layout = Layout.class)
    public static class ViewWhichInvalidatesSession extends MyAbstractView
            implements BeforeEnterObserver {

        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            Location location = event.getUI().getInternals()
                    .getActiveViewLocation();
            if (!location.getPath().isEmpty()) {
                VaadinSession.getCurrent().getSession().invalidate();
            }
        }
    }

}
