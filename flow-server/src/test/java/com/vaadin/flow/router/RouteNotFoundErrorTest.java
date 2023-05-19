/*
 * Copyright 2000-2023 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.router;

import java.util.Collections;
import java.util.List;

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
        BeforeEnterEvent event = createEvent(true);

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

    @Test
    public void setErrorParameter_devMode_noRoutes() {
        RouteNotFoundError page = new RouteNotFoundError();
        BeforeEnterEvent event = createEvent(false);

        ErrorParameter<NotFoundException> param = new ErrorParameter<>(
                NotFoundException.class, new NotFoundException());

        Router router = Mockito.mock(Router.class);
        Mockito.when(event.getSource()).thenReturn(router);
        RouteRegistry registry = Mockito.mock(RouteRegistry.class);
        Mockito.when(router.getRegistry()).thenReturn(registry);
        Mockito.when(registry.getRegisteredRoutes()).thenReturn(List.of());

        event.getSource().getRegistry().getRegisteredRoutes();

        page.setErrorParameter(event, param);

        MatcherAssert.assertThat(page.getElement().toString(),
                CoreMatchers.containsString("No views found"));
    }

    private BeforeEnterEvent createEvent(boolean productionMode) {
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
        Mockito.when(config.isProductionMode()).thenReturn(productionMode);
        Mockito.when(event.getUI()).thenReturn(ui);
        return event;
    }

}
