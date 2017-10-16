package com.vaadin.flow.uitest.servlet;

import javax.servlet.annotation.WebServlet;

import com.vaadin.router.ParentLayout;
import com.vaadin.router.Route;
import com.vaadin.server.VaadinServletConfiguration;
import com.vaadin.ui.html.Div;
import com.vaadin.router.HasUrlParameter;
import com.vaadin.router.RouterLayout;
import com.vaadin.router.event.BeforeNavigationEvent;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.html.NativeButton;

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
}
