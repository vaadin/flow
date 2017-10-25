package com.vaadin.flow.uitest.servlet;

import javax.servlet.annotation.WebServlet;
import java.util.stream.Stream;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.router.HasUrlParameter;
import com.vaadin.router.ParentLayout;
import com.vaadin.router.Route;
import com.vaadin.router.RouterLayout;
import com.vaadin.router.event.BeforeNavigationEvent;
import com.vaadin.router.event.BeforeNavigationObserver;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletConfiguration;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;
import com.vaadin.ui.Component;
import com.vaadin.ui.html.Div;
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

    @Route("abc")
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

    public static class Layout extends Div
            implements RouterLayout, BeforeNavigationObserver {

        private Element sessionId;

        public Layout() {
            sessionId = ElementFactory.createDiv().setAttribute("id",
                    "sessionId");
            getElement().appendChild(sessionId);
            getElement().appendChild(ElementFactory.createDiv());
            getElement().appendChild(ElementFactory.createHr());
        }

        @Override
        public void beforeNavigation(BeforeNavigationEvent event) {
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
            implements BeforeNavigationObserver {

        @Override
        public void beforeNavigation(BeforeNavigationEvent event) {
            throw new RuntimeException(
                    "Intentionally caused by " + getClass().getSimpleName());
        }
    }

    @Route(value = "ViewWhichInvalidatesSession", layout = Layout.class)
    public static class ViewWhichInvalidatesSession extends MyAbstractView
            implements BeforeNavigationObserver {

        @Override
        public void beforeNavigation(BeforeNavigationEvent event) {
            VaadinSession.getCurrent().getSession().invalidate();
        }
    }

}
