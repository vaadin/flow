package com.vaadin.flow.tutorial.webcomponent.compilation;

import java.util.Optional;

import com.vaadin.ui.common.HtmlImport;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.RouterConfiguration;
import com.vaadin.flow.router.RouterConfigurator;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.router.RouterInterface;
import com.vaadin.flow.tutorial.annotations.CodeFor;
import com.vaadin.ui.UI;

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
