package com.vaadin.flow.uitest.servlet;

import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.Route;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.flow.html.Div;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.event.BeforeNavigationEvent;
import com.vaadin.server.VaadinServlet;

@WebServlet(asyncSupported = true, urlPatterns = { "/new-router-session/*" })
@VaadinServletConfiguration(productionMode = false, usingNewRouting = true)
public class RouterTestServlet extends VaadinServlet {

    public static class ClassNameDiv extends Div {
        public ClassNameDiv() {
            setText(this.getClass().getSimpleName());
            setId("name-div");
        }
    }

    @Route("")
    public static class RootNavigationTarget extends ClassNameDiv {
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
        public void setParameter(BeforeNavigationEvent event,
                String parameter) {
            setText(String.format("Hello, %s!", parameter));
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
}
