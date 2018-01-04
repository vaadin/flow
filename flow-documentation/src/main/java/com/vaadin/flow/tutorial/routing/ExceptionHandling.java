package com.vaadin.flow.tutorial.routing;

import javax.servlet.http.HttpServletResponse;
import java.nio.file.AccessDeniedException;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.router.BeforeNavigationEvent;
import com.vaadin.flow.router.BeforeNavigationObserver;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("routing/tutorial-routing-exception-handling.asciidoc")
public class ExceptionHandling {

    @Tag(Tag.DIV)
    public class RouteNotFoundError extends Component
            implements HasErrorParameter<NotFoundException> {

        @Override
        public int setErrorParameter(BeforeNavigationEvent event,
                ErrorParameter<NotFoundException> parameter) {
            getElement().setText("Could not navigate to '"
                    + event.getLocation().getPath() + "'");
            return HttpServletResponse.SC_NOT_FOUND;
        }
    }

    public class CustomNotFoundTarget extends RouteNotFoundError {

        @Override
        public int setErrorParameter(BeforeNavigationEvent event,
                ErrorParameter<NotFoundException> parameter) {
            getElement().setText("My custom not found class!");
            return HttpServletResponse.SC_NOT_FOUND;
        }
    }

    public class AuthenticationHandler implements BeforeNavigationObserver {
        @Override
        public void beforeNavigation(BeforeNavigationEvent event) {
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

    @Tag(Tag.DIV)
    public class AccessDeniedExceptionHandler extends Component
            implements HasErrorParameter<AccessDeniedException> {

        @Override
        public int setErrorParameter(BeforeNavigationEvent event,
                ErrorParameter<AccessDeniedException> parameter) {
            getElement().setText(
                    "Tried to navigate to a view without correct access rights");
            return HttpServletResponse.SC_FORBIDDEN;
        }
    }

    @Tag(Tag.DIV)
    public class BlogPost extends Component implements HasUrlParameter<Long> {

        @Override
        public void setParameter(BeforeNavigationEvent event, Long parameter) {
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
        public int setErrorParameter(BeforeNavigationEvent event,
                ErrorParameter<IllegalArgumentException> parameter) {
            Label message = new Label(parameter.getCustomMessage());
            getElement().appendChild(message.getElement());

            return HttpServletResponse.SC_NOT_FOUND;
        }
    }

    private class BlogRecord {
    }

}
