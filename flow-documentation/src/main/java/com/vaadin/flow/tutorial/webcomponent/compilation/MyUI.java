package com.vaadin.flow.tutorial.webcomponent.compilation;

import java.util.Optional;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.router.RouterInterface;
import com.vaadin.flow.router.legacy.Router;
import com.vaadin.flow.router.legacy.RouterConfiguration;
import com.vaadin.flow.router.legacy.RouterConfigurator;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.tutorial.annotations.CodeFor;

@CodeFor("web-components/tutorial-webcomponents-es5.asciidoc")
@HtmlImport("index.html")
public class MyUI extends UI {

    private Router router;

    public class MyRouterConfigurator implements RouterConfigurator {
        @Override
        public void configure(RouterConfiguration configuration) {
            // You router configuration code
        }
    }

    @Override
    public void doInit(VaadinRequest request, int uiId) {
        super.doInit(request, uiId);
        router = new Router();
        router.reconfigure(new MyRouterConfigurator());
        router.initializeUI(this, request);
    }

    @Override
    public Optional<RouterInterface> getRouterInterface() {
        return Optional.ofNullable(router);
    }
}
