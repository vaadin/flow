package com.vaadin.router;

import com.vaadin.router.event.AfterNavigationEvent;
import com.vaadin.router.event.AfterNavigationListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.Tag;

@Tag(Tag.DIV)
public class RouteNotFoundError extends Component
        implements AfterNavigationListener {

    public RouteNotFoundError() {
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        getElement().setText(
                "Could not navigate to " + event.getLocation().getPath());
    }
}
