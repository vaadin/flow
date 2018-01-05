package com.vaadin.flow.tutorial.routing;

import javax.servlet.http.HttpServletResponse;
import java.util.Optional;
import java.util.stream.Stream;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("routing/tutorial-routing-exception-handling.asciidoc")
public class ExceptionHandling {

    @Tag(Tag.DIV)
    public class RouteNotFoundError extends Component
            implements HasErrorParameter<NotFoundException> {

        @Override
        public int setErrorParameter(BeforeEnterEvent event,
                ErrorParameter<NotFoundException> parameter) {
            getElement().setText("Could not navigate to '"
                    + event.getLocation().getPath() + "'");
            return HttpServletResponse.SC_NOT_FOUND;
        }
    }

    @ParentLayout(MainLayout.class)
    public class CustomNotFoundTarget extends RouteNotFoundError {

        @Override
        public int setErrorParameter(BeforeEnterEvent event,
                ErrorParameter<NotFoundException> parameter) {
            getElement().setText("My custom not found class!");
            return HttpServletResponse.SC_NOT_FOUND;
        }
    }

    public class AuthenticationHandler implements BeforeEnterObserver {
        @Override
        public void beforeEnter(BeforeEnterEvent event) {
            Class<?> target = event.getNavigationTarget();
            if (!currentUserMayEnter(target)) {
                event.rerouteToError(AccessDeniedException.class);
            }
        }

        private boolean currentUserMayEnter(Class<?> target) {
            // implementation omitted
            return false;
        }
    }

    @Route(value = "dashboard", layout = MainLayout.class)
    @Tag(Tag.DIV)
    public class Dashboard extends Component {
        public Dashboard() {
            init();
        }

        private void init() {
            getWidgets().forEach(this::addWidget);
        }

        public void addWidget(Widget widget) {
            // Implementation omitted
        }

        private Stream<Widget> getWidgets() {
            // Implementation omitted, gets faulty state widget
            return Stream.of(new ProtectedWidget());
        }
    }

    public class ProtectedWidget extends Widget {
        public ProtectedWidget() {
            if (!AccessHandler.getInstance().isAuthenticated()) {
                throw new AccessDeniedException("Unauthorized widget access");
            }
            // Implementation omitted
        }
    }

    @Tag(Tag.DIV)
    public abstract class Widget extends Component {
        public boolean isProtected() {
            // Implementation omitted
            return true;
        }
    }

    @Tag(Tag.DIV)
    @ParentLayout(MainLayout.class)
    public class AccessDeniedExceptionHandler extends Component
            implements HasErrorParameter<AccessDeniedException> {

        @Override
        public int setErrorParameter(BeforeEnterEvent event,
                ErrorParameter<AccessDeniedException> parameter) {
            getElement().setText(
                    "Tried to navigate to a view without correct access rights");
            return HttpServletResponse.SC_FORBIDDEN;
        }
    }

    @Tag(Tag.DIV)
    public class BlogPost extends Component implements HasUrlParameter<Long> {

        @Override
        public void setParameter(BeforeEvent event, Long parameter) {
            removeAll();

            Optional<BlogRecord> record = getRecord(parameter);

            if (!record.isPresent()) {
                event.rerouteToError(IllegalArgumentException.class,
                        getI18NProvider().getTranslation("blog.post.not.found",
                                event.getLocation().getPath()));
            } else {
                displayRecord(record.get());
            }
        }

        private void removeAll() {
            // NO-OP
        }

        private void displayRecord(BlogRecord record) {
            // NO-OP
        }

        public Optional<BlogRecord> getRecord(Long id) {
            // Implementation omitted
            return Optional.empty();
        }
    }

    @Tag(Tag.DIV)
    public class FaultyBlogPostHandler extends Component
            implements HasErrorParameter<IllegalArgumentException> {

        @Override
        public int setErrorParameter(BeforeEnterEvent event,
                ErrorParameter<IllegalArgumentException> parameter) {
            Label message = new Label(parameter.getCustomMessage());
            getElement().appendChild(message.getElement());

            return HttpServletResponse.SC_NOT_FOUND;
        }
    }

    private class BlogRecord {
    }

    public class MainLayout extends Component implements RouterLayout {
    }

    public class AccessDeniedException extends RuntimeException {
        public AccessDeniedException() {
            super();
        }

        public AccessDeniedException(String message) {
            super(message);
        }
    }
}
