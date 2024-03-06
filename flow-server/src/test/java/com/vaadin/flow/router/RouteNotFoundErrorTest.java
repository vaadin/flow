/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router;

import java.util.Collections;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.VaadinSession;

public class RouteNotFoundErrorTest {

    @Tag(Tag.A)
    private static class RouteTarget extends Component {

    }

    @Test
    public void setErrorParameter_productionMode_pathContainRoutesTemplate_renderedElementHasNoRoutes() {

        RouteNotFoundError page = new RouteNotFoundError();

        BeforeEnterEvent event = Mockito.mock(BeforeEnterEvent.class);
        Location location = Mockito.mock(Location.class);
        Mockito.when(location.getPath()).thenReturn("{{routes}}");
        Mockito.when(event.getLocation()).thenReturn(location);

        UI ui = Mockito.mock(UI.class);
        VaadinSession session = Mockito.mock(VaadinSession.class);
        Mockito.when(ui.getSession()).thenReturn(session);
        DeploymentConfiguration config = Mockito
                .mock(DeploymentConfiguration.class);
        Mockito.when(session.getConfiguration()).thenReturn(config);
        Mockito.when(config.isProductionMode()).thenReturn(true);

        Mockito.when(event.getUI()).thenReturn(ui);

        ErrorParameter<NotFoundException> param = new ErrorParameter<NotFoundException>(
                NotFoundException.class, new NotFoundException());

        Router router = Mockito.mock(Router.class);
        Mockito.when(event.getSource()).thenReturn(router);
        RouteRegistry registry = Mockito.mock(RouteRegistry.class);
        Mockito.when(router.getRegistry()).thenReturn(registry);
        RouteData data = new RouteData(Collections.emptyList(), "bar",
                Collections.emptyList(), RouteTarget.class,
                Collections.emptyList());
        Mockito.when(registry.getRegisteredRoutes())
                .thenReturn(Collections.singletonList(data));

        event.getSource().getRegistry().getRegisteredRoutes();

        page.setErrorParameter(event, param);

        MatcherAssert.assertThat(page.getElement().toString(),
                CoreMatchers.not(CoreMatchers.containsString("bar")));
    }

}
