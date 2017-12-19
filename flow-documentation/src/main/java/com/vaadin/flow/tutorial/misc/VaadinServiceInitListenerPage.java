package com.vaadin.flow.tutorial.misc;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("miscellaneous/tutorial-service-init-listener.asciidoc")
public class VaadinServiceInitListenerPage {

    public class ApplicationServiceInitListener
            implements VaadinServiceInitListener {

        @Override
        public void serviceInit(ServiceInitEvent event) {
            event.addBootstrapListener(response -> {
                // BoostrapListener to change the bootstrap page
            });

            event.addDependencyFilter((dependencies, filterContext) -> {
                // DependencyFilter to add/remove/change dependencies sent to
                // the client
                return dependencies;
            });

            event.addRequestHandler((session, request, response) -> {
                // RequestHandler to change how responses are handled
                return false;
            });
        }

    }

}
