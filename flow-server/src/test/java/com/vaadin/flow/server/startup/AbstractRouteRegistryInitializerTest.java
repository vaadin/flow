/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.server.startup;

import java.util.stream.Stream;

import javax.servlet.ServletContext;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RoutePrefix;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.TestRouteRegistry;
import com.vaadin.flow.server.InvalidRouteLayoutConfigurationException;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.theme.AbstractTheme;
import com.vaadin.flow.theme.Theme;

public class AbstractRouteRegistryInitializerTest {

    private AbstractRouteRegistryInitializer initializer;

    @Before
    public void setUp() {
        initializer = new AbstractRouteRegistryInitializer() {
        };
        ApplicationRouteRegistry registry = new TestRouteRegistry();
        ServletContext servletContext = Mockito.mock(ServletContext.class);
        Mockito.when(servletContext.getAttribute(RouteRegistry.class.getName()))
                .thenReturn(registry);

        VaadinServletService service = Mockito.mock(VaadinServletService.class);
        VaadinServlet servlet = Mockito.mock(VaadinServlet.class);
        Mockito.when(service.getServlet()).thenReturn(servlet);
        CurrentInstance.set(VaadinService.class, service);
        Mockito.when(servlet.getServletContext()).thenReturn(servletContext);
    }

    @Test
    public void validatePwa_prefixedParent_validattionPasses() {
        initializer.validatePwaClass(
                Stream.of(BaseRouteWithParentPrefixAndRouteAlias.class));
    }

    @Test(expected = InvalidRouteLayoutConfigurationException.class)
    public void validatePwa_notPrefixedParent_validattionFails() {
        initializer.validatePwaClass(Stream.of(BaseRouteWithParent.class));
    }

    @Test
    public void validatePwa_prefixedParentForNotRouteTarget_validationPasses() {
        initializer.validatePwaClass(
                Stream.of(RouteWithParentPrefixAndRouteAlias.class));
    }

    @Test(expected = InvalidRouteLayoutConfigurationException.class)
    public void validatePwa_notParentForNotRouteTarget_validationFails() {
        initializer.validatePwaClass(Stream.of(RouteWithParent.class));
    }

    @Test(expected = InvalidRouteLayoutConfigurationException.class)
    public void validateTheme_targetHasThemeAndHasParentViaAlias_validationFails() {
        initializer.validateRouteClasses(
                Stream.of(RouteWithThemeAndRouteParentInAlias.class));
    }

    @Test
    public void validateTheme_targetHasThemeAndNoParents_validationPasses() {
        initializer.validateRouteClasses(Stream.of(RouteWithTheme.class));
    }

    @Route(value = "", layout = RoutePrefixParent.class)
    @RouteAlias("alias")
    @Tag(Tag.DIV)
    @PWA(name = "foo", shortName = "bar")
    public static class BaseRouteWithParentPrefixAndRouteAlias
            extends Component {
    }

    @Tag(Tag.DIV)
    @RoutePrefix("parent")
    @PWA(name = "foo", shortName = "bar")
    public static class RoutePrefixParent extends Component
            implements RouterLayout {
    }

    @Route(value = "", layout = RouteParent.class)
    @Tag(Tag.DIV)
    @PWA(name = "foo", shortName = "bar")
    public static class BaseRouteWithParent extends Component {
    }

    @Tag(Tag.DIV)
    @PWA(name = "foo", shortName = "bar")
    public static class RouteParent extends Component implements RouterLayout {
    }

    @Route(value = "flow", layout = RoutePrefixParent.class)
    @RouteAlias("alias")
    @Tag(Tag.DIV)
    @PWA(name = "foo", shortName = "bar")
    public static class RouteWithParentPrefixAndRouteAlias extends Component {
    }

    @Route(value = "flow", layout = RouteParent.class)
    @Tag(Tag.DIV)
    @PWA(name = "foo", shortName = "bar")
    public static class RouteWithParent extends Component {
    }

    @Route(value = "flow")
    @RouteAlias(value = "alias", layout = RouteParent.class)
    @Theme(AbstractTheme.class)
    public static class RouteWithThemeAndRouteParentInAlias {
    }

    @Route(value = "flow")
    @RouteAlias(value = "alias")
    @Theme(AbstractTheme.class)
    public static class RouteWithTheme {
    }
}
