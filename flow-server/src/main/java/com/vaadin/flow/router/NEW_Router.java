package com.vaadin.flow.router;

import java.util.Optional;

import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinService;
import com.vaadin.server.startup.RouteRegistry;
import com.vaadin.ui.Component;
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

        Optional<Class<? extends Component>> navigationTarget = RouteRegistry
                .getInstance().getNavigationTarget(location);
        if (navigationTarget.isPresent()) {

            NavigationEvent navigationEvent = new NavigationEvent(this,
                    location, ui, trigger);

            NEW_StaticRouteTargetRenderer handler = new NEW_StaticRouteTargetRenderer(
                    navigationTarget.get(), location.getPath());
            return handler.handle(navigationEvent);
        }

        return 404;
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
