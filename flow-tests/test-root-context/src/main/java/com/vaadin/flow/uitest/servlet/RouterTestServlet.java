package com.vaadin.flow.uitest.servlet;

import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.ParentLayout;
import com.vaadin.annotations.Route;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.flow.html.Div;
import com.vaadin.flow.router.RouterLayout;
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
