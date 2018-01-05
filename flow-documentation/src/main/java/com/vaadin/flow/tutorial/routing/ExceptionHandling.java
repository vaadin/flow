package com.vaadin.flow.tutorial.routing;

import java.nio.file.AccessDeniedException;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.NotFoundException;
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
            return 404;
        }
    }

    public class CustomNotFoundTarget extends RouteNotFoundError {

        @Override
        public int setErrorParameter(BeforeEnterEvent event,
                ErrorParameter<NotFoundException> parameter) {
            getElement().setText("My custom not found class!");
            return 404;
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
}
