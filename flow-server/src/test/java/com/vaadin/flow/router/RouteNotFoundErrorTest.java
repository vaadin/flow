/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class RouteNotFoundErrorTest {

    @Tag(Tag.A)
    private static class RouteTarget extends Component {

    }

    @Test
    void setErrorParameter_productionMode_pathContainRoutesTemplate_renderedElementHasNoRoutes() {

        RouteNotFoundError page = new RouteNotFoundError();
        BeforeEnterEvent event = createEvent(true);

        ErrorParameter<NotFoundException> param = new ErrorParameter<NotFoundException>(
                NotFoundException.class, new NotFoundException());

        Router router = mock(Router.class);
        when(event.getSource()).thenReturn(router);
        RouteRegistry registry = mock(RouteRegistry.class);
        when(router.getRegistry()).thenReturn(registry);
        RouteData data = new RouteData(Collections.emptyList(), "bar",
                Collections.emptyList(), RouteTarget.class,
                Collections.emptyList());
        when(registry.getRegisteredRoutes())
                .thenReturn(Collections.singletonList(data));

        event.getSource().getRegistry().getRegisteredRoutes();

        try (MockedStatic<VaadinService> service = mockStatic(
                VaadinService.class, Mockito.CALLS_REAL_METHODS)) {
            VaadinService vaadinService = mock(VaadinService.class);
            DeploymentConfiguration configuration = mock(
                    DeploymentConfiguration.class);
            when(vaadinService.getDeploymentConfiguration())
                    .thenReturn(configuration);
            when(configuration.isProductionMode()).thenReturn(true);
            when(configuration.getFrontendFolder())
                    .thenReturn(new File("front"));

            service.when(() -> VaadinService.getCurrent())
                    .thenReturn(vaadinService);
            page.setErrorParameter(event, param);
        }

        assertThat(page.getElement().toString(), not(containsString("bar")));
    }

    @Test
    void setErrorParameter_devMode_noRoutes() {
        RouteNotFoundError page = new RouteNotFoundError();
        BeforeEnterEvent event = createEvent(false);

        ErrorParameter<NotFoundException> param = new ErrorParameter<>(
                NotFoundException.class, new NotFoundException());

        VaadinService vaadinService = mock(VaadinService.class);
        when(vaadinService.getDeploymentConfiguration()).thenReturn(null);

        Router router = mock(Router.class);
        when(event.getSource()).thenReturn(router);
        RouteRegistry registry = mock(RouteRegistry.class);
        when(router.getRegistry()).thenReturn(registry);
        when(registry.getRegisteredRoutes()).thenReturn(List.of());

        event.getSource().getRegistry().getRegisteredRoutes();

        try (MockedStatic<FrontendUtils> util = mockStatic(FrontendUtils.class);
                MockedStatic<VaadinService> service = mockStatic(
                        VaadinService.class, Mockito.CALLS_REAL_METHODS);) {
            util.when(() -> FrontendUtils.getProjectFrontendDir(any()))
                    .thenReturn(null);
            util.when(() -> FrontendUtils.isHillaUsed(any())).thenReturn(false);
            service.when(() -> VaadinService.getCurrent())
                    .thenReturn(vaadinService);

            page.setErrorParameter(event, param);
        }

        assertThat(page.getElement().toString(),
                containsString("No views found"));
    }

    private BeforeEnterEvent createEvent(boolean productionMode) {
        BeforeEnterEvent event = mock(BeforeEnterEvent.class);
        Location location = mock(Location.class);
        when(location.getPath()).thenReturn("{{routes}}");
        when(event.getLocation()).thenReturn(location);
        UI ui = mock(UI.class);
        VaadinSession session = mock(VaadinSession.class);
        when(ui.getSession()).thenReturn(session);
        when(ui.getSessionOrThrow()).thenCallRealMethod();
        DeploymentConfiguration config = mock(DeploymentConfiguration.class);
        when(session.getConfiguration()).thenReturn(config);
        when(config.isProductionMode()).thenReturn(productionMode);
        when(config.getFrontendFolder()).thenReturn(new File("front"));
        when(event.getUI()).thenReturn(ui);
        return event;
    }

}
