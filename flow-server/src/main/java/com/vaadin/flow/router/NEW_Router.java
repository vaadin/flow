package com.vaadin.flow.router;

import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinService;
import com.vaadin.ui.UI;

/**
 *
 */
public class NEW_Router implements NEW_RouterInterface {

    private volatile RouterConfiguration configuration = new RouterConfiguration() {
        @Override
        public boolean isConfigured() {
            return true;
        }
    };

    @Override
    public void initializeUI(UI ui, VaadinRequest initRequest) {

        String pathInfo = initRequest.getPathInfo();

        final String path;
        if (pathInfo == null) {
            path = "";
        } else {
            assert pathInfo.startsWith("/");
            path = pathInfo.substring(1);
        }

        final QueryParameters queryParameters = QueryParameters
                .full(initRequest.getParameterMap());

        ui.getPage().getHistory().setHistoryStateChangeHandler(
                e -> navigate(ui, e.getLocation(), e.getTrigger()));

        Location location = new Location(path, queryParameters);
        int statusCode = navigate(ui, location, NavigationTrigger.PAGE_LOAD);

        VaadinResponse response = VaadinService.getCurrentResponse();
        if (response != null) {
            response.setStatus(statusCode);
        }
    }

    @Override
    public int navigate(UI ui, Location location, NavigationTrigger trigger) {
        assert ui != null;
        assert location != null;
        assert trigger != null;

        /*
        // Read volatile field only once per navigation
        ImmutableRouterConfiguration currentConfig = configuration;
        assert currentConfig.isConfigured();

        NavigationEvent navigationEvent = new NavigationEvent(this, location,
                ui, trigger);

        Optional<NavigationHandler> handler = currentConfig.getResolver()
                .resolve(navigationEvent);

        if (!handler.isPresent()) {
            handler = currentConfig.resolveRoute(location);
        }

        // Redirect foo/bar <-> foo/bar/ if there is no mapping for the given
        // location but there is a mapping for the other
        if (!handler.isPresent() && !location.getPath().isEmpty()) {
            Location toggledLocation = location.toggleTrailingSlash();
            Optional<NavigationHandler> toggledHandler = currentConfig
                    .resolveRoute(toggledLocation);
            if (toggledHandler.isPresent()) {
                handler = Optional
                        .of(new InternalRedirectHandler(toggledLocation));
            }
        }

        if (!handler.isPresent()) {
            NavigationHandler errorHandler = currentConfig.getErrorHandler();
            handler = Optional.of(errorHandler);
        }
        */
        
        return 500;
    }

    @Override
    public void reconfigure(RouterConfigurator configurator) {
        // NO-OP
    }

    @Override
    public ImmutableRouterConfiguration getConfiguration() {
        return configuration;
    }
}
